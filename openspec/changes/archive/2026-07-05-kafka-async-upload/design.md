## Context

`POST /api/assets/upload` is fully synchronous today: `AssetController.uploadAsset` calls `UploadAssetUseCaseImpl.execute`, which writes the file to disk via `StoragePort.copyFile` and then delegates to `CatalogFolderPort.createAsset` — the exact same private method (`CatalogFolderServiceImpl.createAsset`) the folder-scan catalog batch job uses. That method computes the SHA-256 hash, extracts EXIF metadata (persisted to MongoDB via `AssetExifRepository`), generates a 200×150 thumbnail (persisted via `ThumbnailPort`), and saves the `Asset` row — all inline, in one `@Transactional` block, on the HTTP request thread. For RAW (40–80 MB) and video files this takes multiple seconds per file; a drag-drop batch of several such files serializes this cost.

`kafka-catalog-pipeline` (#75) already provisioned the Kafka broker (single KRaft node, `docker-compose.yml`, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, topics declared exclusively via `KafkaTopicConfig` `@Bean NewTopic`), the SSE-over-Kafka bridge (`KafkaProgressRegistry` + `KafkaProgressListener`), and — critically — the exact consumer-group pattern this change needs: `AuditLogKafkaListener` proves that a topic can be consumed by an explicit, persistent, shared `groupId` (`audit-log-writer`) independently of the per-instance `sse-broadcaster-*` group used for progress fan-out, and that multiple listeners can consume the same topics without interfering with each other.

The `assets` table currently declares `hash TEXT NOT NULL` and `thumbnail_creation_date_time TIMESTAMP NOT NULL` (`V1__initial_schema.sql`). No "processing status" concept exists anywhere in the schema or domain model. This is the central schema decision this design must resolve, since the whole point of the change is to persist an asset row *before* those values are known.

## Goals / Non-Goals

**Goals:**
- Return from `POST /api/assets/upload` as soon as the file is validated and written to disk, without waiting for hashing, EXIF extraction, or thumbnail generation.
- Process hashing, EXIF extraction, and thumbnail generation as three independent Kafka consumer groups so each can be scaled, retried, and monitored independently.
- Give the frontend a reliable, low-latency signal (SSE) for when an uploaded asset becomes fully populated, without polling.
- Allow a partially-failed upload (e.g. EXIF extraction failed, hash and thumbnail succeeded) to be resolved by re-processing only the failed stage.
- Preserve the existing folder-scan catalog pipeline (`CatalogAssetsUseCase` / Spring Batch) entirely unchanged — this change only touches the single-file upload path.

**Non-Goals:**
- Changing the folder-scan catalog batch pipeline (`kafka-catalog-pipeline`, #75) or its `job.catalog.progress` topic — the upload path gets its own topics.
- General upload resumability / chunked uploads — the multipart POST itself remains a single synchronous request; only the post-processing after the bytes are on disk becomes async.
- Horizontal auto-scaling infrastructure (e.g. Kubernetes HPA tied to consumer lag) — this design only makes the three stages *able* to scale independently via separate consumer groups; provisioning multiple instances is a deployment concern out of scope here.
- Changing how the folder-scan catalog path computes hash/EXIF/thumbnail — `CatalogFolderServiceImpl.createAsset` remains used by the batch pipeline; only the upload-specific call site is replaced.

## Decisions

### 1. Persist a placeholder `Asset` row synchronously, before any Kafka message is published

**Decision:** `UploadAssetUseCaseImpl` (renamed responsibility, still the same class) writes the file to disk, then inserts a minimal `Asset` row with `processing_status = PENDING`, `hash = NULL`, `thumbnail_creation_date_time = NULL`, and the fields that are already known synchronously (`fileName`, `folder`, `fileSize`, `fileCreationDateTime`, `fileModificationDateTime`, `isVideo`, `fileType`). The generated `assetId` is included in the `AssetUploadedEvent` published to `asset.uploaded`.

**Rationale:** The thumbnail blob name is derived from `assetId` (`Asset.getThumbnailBlobName()` = `assetId + ".bin"`), so an `assetId` must exist before the thumbnail consumer can write anything. Persisting the row first also gives the frontend an immediate `assetId` to reference in the `202` response body and the SSE observe endpoint, and gives `GET /api/assets?folderPath=` something to return right away (as a "processing" placeholder) rather than the asset being invisible until all three stages finish.

**Alternative considered:** Defer the `Asset` insert entirely until all three stages complete, staging intermediate results in Kafka message payloads or a Redis hash keyed by a synthetic `uploadId`. Rejected: it duplicates state, complicates "what does the gallery show right now" (a still-uploading file would have no row at all, which regresses the existing `GET /api/assets` behavior right after upload described in `drag-and-drop-upload`), and adds a second correlation key (`uploadId` vs `assetId`) with a translation step once processing finishes. Using the real `assetId` from the start is simpler.

### 2. New Flyway migration: `processing_status` + three completion timestamps; relax `NOT NULL`

**Decision:** `V32__add_asset_processing_status.sql` adds:
```sql
ALTER TABLE assets ALTER COLUMN hash DROP NOT NULL;
ALTER TABLE assets ALTER COLUMN thumbnail_creation_date_time DROP NOT NULL;
ALTER TABLE assets ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE assets ADD COLUMN hash_completed_at TIMESTAMP;
ALTER TABLE assets ADD COLUMN exif_completed_at TIMESTAMP;
ALTER TABLE assets ADD COLUMN thumbnail_completed_at TIMESTAMP;
```
Existing rows default to `COMPLETED` (they were, by definition, fully processed under the old synchronous path) and their `hash`/`thumbnail_creation_date_time` values are untouched. Only new upload-path rows start at `PENDING`. The folder-scan catalog path continues to insert fully-populated rows and can either set `processing_status = 'COMPLETED'` explicitly or rely on the same default.

**Rationale:** `processing_status` as a `VARCHAR` enum (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`) rather than three independent booleans keeps the "is this asset usable yet" check to a single-column comparison for gallery queries, while the three `*_completed_at` timestamp columns retain per-stage observability (which stage is slow/stuck) without needing to inspect Kafka consumer lag. `NULL` on a completed timestamp column plus `processing_status = FAILED` identifies exactly which stage needs re-triggering.

**Alternatives considered:**
- Three boolean flags only (no enum): rejected — computing "is it done" requires `hash_completed_at IS NOT NULL AND exif_completed_at IS NOT NULL AND thumbnail_completed_at IS NOT NULL` on every gallery list query; a single indexed `processing_status` column is cheaper and clearer.
- A separate `asset_processing_jobs` table instead of columns on `assets`: rejected as over-engineering for three stages with no need for job history beyond "did it finish" — the existing `assets` row already carries per-asset processing state elsewhere in the schema (e.g. `thumbnail_creation_date_time` itself is already a rudimentary "last processed" marker).

### 3. Three independent, persistent Kafka consumer groups on one topic (`asset.uploaded`)

**Decision:** One topic, `asset.uploaded` (3 partitions, matching `job.catalog.progress`/`asset.cataloged` for consistency), carrying `AssetUploadedEvent { assetId, filePath, folderPath, fileName }`, keyed by `String.valueOf(assetId)` for same-partition ordering per asset. Three separate `@Component` listener classes — `AssetHashProcessor`, `AssetExifProcessor`, `AssetThumbnailProcessor` — each with a single `@KafkaListener(topics = "asset.uploaded", groupId = "asset-hash-processor")` (etc.), mirroring `AuditLogKafkaListener`'s explicit-persistent-group pattern rather than `KafkaProgressListener`'s per-instance pattern. Because each group id is distinct, Kafka delivers every message to all three groups independently — each stage sees every upload exactly once regardless of how many app instances are running or how partitions are assigned within a group.

**Rationale:** This is a proven pattern already in the codebase (`AuditLogKafkaListener` doc comment explicitly contrasts the two group strategies). No new abstraction is needed — just three more listener classes following the existing template. Using one topic instead of three keeps ordering trivial (all three stages read the same message) and avoids triple-publishing from the producer side.

**Alternative considered:** Three separate topics (`asset.upload.hash.requested`, `asset.upload.exif.requested`, `asset.upload.thumbnail.requested`), each with one consumer group. Rejected: no benefit here since all three consumers need the same input (`assetId`, `filePath`) and none depend on another's output; a single topic with three groups is simpler to reason about and to add `KafkaTopicConfig` beans for.

### 4. Progress aggregation via a fourth topic (`job.upload.progress`) and `KafkaProgressRegistry`, mirroring the existing SSE pattern exactly

**Decision:** Each of the three processors, after committing its own DB write, publishes an `UploadProgressMessage { assetId, stage, done: false }` to `job.upload.progress` (1 partition — low volume, ordering not critical across stages). A new `KafkaProgressListener.onUploadProgress` method (same class as the existing catalog/sync/convert handlers) receives every message, forwards it to the `SseEmitter` registered for that `assetId` in `KafkaProgressRegistry` (same map, just keyed by `assetId` instead of `runId` — the registry is already `Map<Long, SseEmitter>` so no structural change is needed), and separately increments an in-memory-per-instance counter of stages seen for that `assetId`. When all three stages have reported for a given `assetId` (tracked via a small `ConcurrentHashMap<Long, EnumSet<Stage>>` inside the listener, cleared on completion), the listener publishes/forwards a final `done=true` message and calls `assetRepository` to flip `processing_status` to `COMPLETED`.

**Rationale:** Reuses 100% of the existing SSE bridge infrastructure (`SseEmitter` + `KafkaProgressRegistry`) with no changes to its public shape — only a new topic and a new listener method, exactly like the existing controller-registers-emitter-before-launching-job pattern documented in `kafka-catalog-pipeline`'s spec. Completion tracking lives in the listener (which already runs on every instance and receives every message via the per-instance `sse-broadcaster-*` group), not in a new dedicated coordinator service.

**Alternative considered:** Have each processor check `assets.hash_completed_at, exif_completed_at, thumbnail_completed_at IS NOT NULL` directly (i.e. derive "all done" from the DB row rather than an in-memory per-instance stage set) and have whichever processor observes all three complete responsibility for flipping `processing_status` and publishing `done=true`. This is actually simpler and avoids in-memory state entirely — **this alternative is adopted instead of the in-memory tracking** for the final design: each processor, immediately after its own commit, re-reads the asset row and checks whether the *other two* completion timestamps are already non-null; whichever processor finds all three non-null is the one that flips `processing_status = COMPLETED` and publishes the final `done=true`. This avoids a stateful in-memory set that would be lost on instance restart and is resilient to processing happening out of order or on different instances.

### 5. Failure handling: bounded retries then `FAILED`, with manual re-processing

**Decision:** Rely on Spring Kafka's default `DefaultErrorHandler` with a `FixedBackOff` (e.g. 3 retries, 2s apart) per listener container. If a processor's handler throws after retries are exhausted, the error handler's recoverer publishes an `UploadProgressMessage { assetId, stage, done: true, failed: true }` (via a `DeadLetterPublishingRecoverer`-adjacent hook, or simply caught inside the listener method itself with a try/catch that calls the same "publish stage result" path with a `failed` flag) and sets `processing_status = FAILED` on the asset row. `POST /api/assets/{id}/reprocess` re-publishes `AssetUploadedEvent` for that `assetId`; each processor is idempotent (it always overwrites its own field(s) unconditionally), so re-running all three consumers for an asset that already has some stages completed simply re-computes and overwrites already-correct values for those stages — no special "skip if already done" branching is required, keeping the processors simple.

**Rationale:** Idempotent overwrite-on-reprocess is simpler than conditional skip logic and costs only a redundant hash/EXIF/thumbnail computation (already an accepted cost of the current synchronous path for every asset). Bounded retries with a terminal `FAILED` state avoids an infinite retry loop blocking the topic/partition for other assets' messages.

### 6. Frontend: `202` + SSE observe endpoint, per-file progress row shows "Processing…"

**Decision:** `AssetService.uploadAsset` still performs the multipart POST and reports `HttpEventType.UploadProgress` for the byte-transfer bar (unchanged). On `HttpEventType.Response` with status `202`, it opens `new EventSource('/api/assets/upload/{assetId}/observe')` instead of treating the response as final; `DropZoneComponent`'s per-file row switches from the upload progress bar to a "Processing…" state, then to the existing success/error icon when the SSE stream emits `done`. The batch-level `uploadComplete` event (which triggers `GalleryComponent.loadAssets()`) now fires after all files' SSE streams complete rather than after all POST responses complete.

## Risks / Trade-offs

- **[Risk]** A dropped SSE connection (e.g. tab closed mid-upload) means the frontend never sees the `done` signal, so the gallery might not auto-refresh for that file. → **Mitigation**: the placeholder asset row already exists with `processing_status = PENDING`/`PROCESSING` visible via `GET /api/assets`, so a manual refresh or subsequent `GET` will reflect the true state once processing finishes server-side regardless of SSE delivery; the SSE stream is purely a UX convenience, not the source of truth.
- **[Risk]** Three consumers now read the file from disk independently (hash re-reads the file, EXIF re-reads the file, thumbnail re-reads the file) instead of one pass reading it once — more disk I/O than today's single-pass synchronous flow. → **Mitigation**: accepted trade-off; the whole point is to parallelize these three CPU/IO-bound stages across separate consumers (potentially separate instances), so some redundant I/O in exchange for not serializing them on one thread is the intended trade. This can be revisited later (e.g. caching file bytes in a short-lived local cache) if disk I/O becomes the bottleneck.
- **[Risk]** Partial-failure races: two processors could simultaneously observe "all three complete" and both attempt to flip `processing_status` to `COMPLETED` and publish `done=true` twice. → **Mitigation**: the `processing_status` UPDATE is idempotent (`UPDATE assets SET processing_status='COMPLETED' WHERE assetId=? AND processing_status<>'COMPLETED'`); a double `done=true` SSE message is harmless (`SseEmitter.complete()` on an already-completed emitter is a no-op / already guarded in `KafkaProgressListener`).
- **[Risk]** Relaxing `hash NOT NULL` weakens a data-integrity guarantee other code may implicitly rely on (e.g. duplicate detection by hash). → **Mitigation**: `FindDuplicatedAssetsService`-equivalent queries and any hash-based lookups must filter `WHERE hash IS NOT NULL` or `WHERE processing_status = 'COMPLETED'`; this is called out explicitly as a task so existing hash-dependent queries are audited.
- **[Trade-off]** The upload endpoint's response contract changes from `201` + full body to `202` + minimal body — this is a **breaking change** for any existing API consumer (frontend is updated in this same change; any external/API consumers would break). Accepted given this is an internal SPA-only API with no external consumer contract documented.

## Migration Plan

1. Ship the Flyway migration (`V32`) — additive/relaxing only, safe to deploy ahead of code that uses the new columns (existing rows get `processing_status='COMPLETED'` by default, no behavior change for them).
2. Deploy backend changes: new topics (`KafkaTopicConfig`), the three processor listener classes, the updated `UploadAssetUseCaseImpl` and `AssetController.uploadAsset` (return `202`), the new `job.upload.progress` listener method, and the `reprocess` endpoint — all in one deploy, since the controller and processors must agree on the message contract simultaneously.
3. Deploy frontend changes (SSE-based completion) in the same release train as the backend `202` change, since the old frontend expects `201` + full `AssetDto` and would break against the new response shape.
4. **Rollback**: if the async path misbehaves in production, the controller change can be reverted independently of the migration (the migration is backward compatible — old synchronous code path never reads `processing_status` and continues to always write fully-populated rows, satisfying the relaxed-but-still-usually-populated columns). No data cleanup is required to roll back.

## Open Questions

- Should the three completion timestamp columns be consolidated into a JSONB `processing_stages` column instead of three fixed columns, for easier addition of a fourth stage later (e.g. a future virus-scan or perceptual-hash stage)? Deferred — three columns is simpler now and the schema addresses only the three stages this change introduces; a future change can migrate to JSONB if more stages are added.
- Should `GET /api/assets` expose `processing_status` in `AssetDto` so the frontend can render a "processing" badge on the thumbnail itself (not just the drop-zone row) if the user navigates away and back before processing finishes? Left to task-level implementation detail / follow-up UX polish, not a blocking design decision.
