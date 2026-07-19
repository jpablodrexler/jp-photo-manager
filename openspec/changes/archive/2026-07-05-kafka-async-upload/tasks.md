## 1. Database migration

- [x] 1.1 Create `V32__add_asset_processing_status.sql`: relax `hash` and `thumbnail_creation_date_time` to nullable, add `processing_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'`, add `hash_completed_at`, `exif_completed_at`, `thumbnail_completed_at` TIMESTAMP columns (nullable) on `assets`
- [x] 1.2 Add an index on `processing_status` if gallery queries will filter/sort on it
- [x] 1.3 Update `AssetEntity` (JPA) with the new fields and a `ProcessingStatus` enum (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`)
- [x] 1.4 Update `Asset` domain model with the new fields, keeping `hash` and `thumbnailCreationDateTime` as nullable types
- [x] 1.5 Audit existing hash-dependent queries (e.g. duplicate detection) and add `processing_status = 'COMPLETED'` / `hash IS NOT NULL` filters where needed so in-flight uploads are excluded

## 2. Kafka topics and message contracts

- [x] 2.1 Add `AssetUploadedEvent` record (`assetId`, `filePath`, `folderPath`, `fileName`) to `application/dto`
- [x] 2.2 Add `UploadProgressMessage` record (`assetId`, `stage` enum `HASH`/`EXIF`/`THUMBNAIL`, `done`, `failed`) to `application/dto`
- [x] 2.3 Add `NewTopic` beans for `asset.uploaded` (3 partitions) and `job.upload.progress` (1 partition) to `KafkaTopicConfig`
- [x] 2.4 Confirm `spring.json.trusted.packages` in `application.yml`/`application-test.yml` covers the new DTOs' package

## 3. Upload endpoint changes

- [x] 3.1 Update `UploadAssetUseCaseImpl.execute` to persist a placeholder `Asset` (`processing_status = PENDING`, `hash = null`, `thumbnailCreationDateTime = null`) after writing the file to disk, instead of delegating to `CatalogFolderPort.createAsset`
- [x] 3.2 Publish `AssetUploadedEvent` to `asset.uploaded` (keyed by `assetId`) after the placeholder row is committed
- [x] 3.3 Update `AssetController.uploadAsset` to return `202 Accepted` with `{ assetId, status: "PROCESSING" }`
- [x] 3.4 Verify `CatalogFolderServiceImpl`'s folder-scan batch path (`createAsset` used by the Spring Batch item writer) is unchanged and still sets `processing_status = COMPLETED` (or relies on the column default) for fully-synchronous catalog runs

## 4. Hash, EXIF, and thumbnail consumers

- [x] 4.1 Create `AssetHashProcessor` (`@KafkaListener(topics = "asset.uploaded", groupId = "asset-hash-processor")`): compute SHA-256 via `StoragePort.computeHash`, update `Asset.hash` + `hash_completed_at`, commit in its own transaction
- [x] 4.2 Create `AssetExifProcessor` (`groupId = "asset-exif-processor"`): extract EXIF via `StoragePort.getExifMetadata`, upsert into `AssetExifRepository`, set `exif_completed_at`
- [x] 4.3 Create `AssetThumbnailProcessor` (`groupId = "asset-thumbnail-processor"`): generate thumbnail via `StoragePort.generateThumbnail`, persist via `ThumbnailPort.saveThumbnail`, set `thumbnailCreationDateTime` + `thumbnail_completed_at`
- [x] 4.4 In each processor, after committing, re-read the asset row and check whether the other two completion timestamps are non-null; if so, idempotently set `processing_status = COMPLETED` (guarded `UPDATE ... WHERE processing_status <> 'COMPLETED'`) and publish the final `job.upload.progress` message with `done=true`
- [x] 4.5 If none of the other two are complete yet, publish a non-final `job.upload.progress` message (`done=false`) for this stage
- [x] 4.6 Configure a bounded retry policy (`DefaultErrorHandler` + `FixedBackOff`, e.g. 3 retries / 2s) per listener container; on retry exhaustion, set `processing_status = FAILED` and publish `{stage, done:true, failed:true}`

## 5. SSE progress bridge

- [x] 5.1 Add `KafkaProgressListener.onUploadProgress` (`@KafkaListener(topics = "job.upload.progress")`, per-instance group, same pattern as the existing catalog/sync/convert handlers) forwarding messages to the `SseEmitter` registered for `assetId` in `KafkaProgressRegistry`
- [x] 5.2 On `done=true`, call `emitter.complete()` and remove the `assetId` entry from the registry (idempotent/no-op if already removed)
- [x] 5.3 Add `GET /api/assets/upload/{assetId}/observe` to `AssetController`: create an `SseEmitter`, register it in `KafkaProgressRegistry` keyed by `assetId`, return immediately

## 6. Reprocessing endpoint

- [x] 6.1 Add `POST /api/assets/{id}/reprocess` (admin-only, following the `@PreAuthorize("hasRole('ADMIN')")` pattern used by `UploadAssetUseCaseImpl`): validate the asset exists and `processing_status = FAILED` (or allow force-reprocessing any status), set `processing_status = PROCESSING`, and republish `AssetUploadedEvent` for that asset
- [x] 6.2 Verify each processor's overwrite behavior is safe to re-run (idempotent) with no special-case "skip if already done" branching required

## 7. Frontend changes

- [x] 7.1 Update `AssetService.uploadAsset` to handle `202 Accepted` (`{ assetId, status }`) instead of `201 Created` (`AssetDto`)
- [x] 7.2 Add a method to open an `EventSource` against `GET /api/assets/upload/{assetId}/observe` and emit typed events for stage progress and final completion
- [x] 7.3 Update `DropZoneComponent`: after `202` response, show "Processing…" per file row; subscribe to the observe SSE stream; on `done` show the success icon; on upload-time errors (`415`, etc.) keep existing error-icon behavior unchanged
- [x] 7.4 Update the batch-level `uploadComplete` emission to wait for all files' SSE streams to finish (success or failure) rather than firing when all POST responses return
- [x] 7.5 Verify `GalleryComponent.loadAssets()` still refreshes correctly after the updated `uploadComplete` timing

## 8. Tests

- [x] 8.1 Backend unit tests: `UploadAssetUseCaseImpl` publishes `AssetUploadedEvent` and persists a `PENDING` placeholder without calling `CatalogFolderPort.createAsset`
- [x] 8.2 Backend unit tests: each of `AssetHashProcessor`, `AssetExifProcessor`, `AssetThumbnailProcessor` correctly updates its fields and correctly determines when to flip `processing_status` to `COMPLETED`
- [x] 8.3 Backend unit test: idempotent `COMPLETED` transition under simulated concurrent completion
- [x] 8.4 Backend unit test: retry exhaustion sets `processing_status = FAILED` and publishes a failed progress message
- [x] 8.5 Backend integration test (`@EmbeddedKafka`, Testcontainers Postgres + Mongo): end-to-end upload through all three consumers reaches `processing_status = COMPLETED` with correct hash/EXIF/thumbnail data
- [x] 8.6 Backend integration test: `POST /api/assets/{id}/reprocess` after a simulated EXIF failure results in `COMPLETED` without altering already-correct hash/thumbnail data
- [x] 8.7 Frontend Cypress component tests: `DropZoneComponent` shows "Processing…" after `202`, then success icon after simulated SSE `done` event (using `MockEventSource`)
- [x] 8.8 Frontend Cypress component tests: `AssetService.uploadAsset` correctly parses the `202` response body

## 9. Documentation and rollout

- [x] 9.1 Update `CLAUDE.md` web-architecture section describing the upload flow, new Kafka topics, and consumer groups (following the existing style used for `kafka-catalog-pipeline`, `mongodb-audit-log`, etc.)
- [x] 9.2 Update the Kafka topics table in `CLAUDE.md` to include `asset.uploaded` and `job.upload.progress`
- [x] 9.3 Run `mvn verify -Pintegration-tests` locally against Docker (Postgres, Kafka, MongoDB) to confirm the full pipeline before merging
