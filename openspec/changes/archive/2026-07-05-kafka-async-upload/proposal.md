## Why

`POST /api/assets/upload` currently does SHA-256 hashing, EXIF extraction, and thumbnail generation synchronously on the HTTP request thread (via `CatalogFolderPort.createAsset`), the same code path the folder-scan catalog job uses. For large files — RAW images (40–80 MB) and video — this blocks the uploading browser tab for multiple seconds per file, and a batch of dropped files serializes this cost across the whole batch. The Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75) already provides everything needed to decouple this: durable topics, `KafkaTemplate` producers, and the persistent-consumer-group pattern proven by `AuditLogKafkaListener` (`audit-log-writer` group). This change reuses that pattern to let hashing, EXIF extraction, and thumbnail generation run as three independent, horizontally scalable Kafka consumers instead of inline work on the request thread.

## What Changes

- **BREAKING**: `POST /api/assets/upload` returns `202 Accepted` with `{ assetId, status: "PROCESSING" }` instead of `201 Created` with a fully populated `AssetDto`. The controller synchronously validates the file, writes it to disk via `StoragePort`, persists a minimal placeholder `Asset` row, and publishes an `AssetUploadedEvent { assetId, filePath, folderPath, fileName }` to a new `asset.uploaded` Kafka topic.
- Add a `processing_status` column (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`) plus three nullable completion timestamp columns (`hash_completed_at`, `exif_completed_at`, `thumbnail_completed_at`) to `assets` (new Flyway migration `V32`). Relax the existing `NOT NULL` constraints on `hash` and `thumbnail_creation_date_time` since a placeholder row is now persisted before either is computed.
- Add three independent, persistent-consumer-group `@KafkaListener`s (`asset-hash-processor`, `asset-exif-processor`, `asset-thumbnail-processor`), each subscribed to `asset.uploaded`, following the `AuditLogKafkaListener` explicit-`groupId` pattern (not the per-instance `sse-broadcaster-*` pattern) so each event is processed exactly once per stage across all running instances:
  - Hash consumer computes the SHA-256 hash via `StoragePort.computeHash` and updates `Asset.hash` + `hash_completed_at`.
  - EXIF consumer extracts metadata via `StoragePort.getExifMetadata` and upserts into `AssetExifRepository` (MongoDB) + sets `exif_completed_at`.
  - Thumbnail consumer generates the 200×150 thumbnail via `StoragePort.generateThumbnail`, persists it via `ThumbnailPort.saveThumbnail`, and sets `thumbnailCreationDateTime` + `thumbnail_completed_at`.
  - Each consumer commits its own result in its own transaction; a failure in one stage (e.g. EXIF extraction throws) does not roll back or block the other two.
- Add a `job.upload.progress` Kafka topic and `UploadProgressMessage` DTO (`assetId`, `stage`, `done`). After each consumer commits, it publishes a progress message; when all three stages have reported completion for a given `assetId`, `processing_status` transitions to `COMPLETED` (or `FAILED` if any stage's retries are exhausted) and a final `done=true` message is published.
- Register an `SseEmitter` per upload (keyed by `assetId`) in `KafkaProgressRegistry`, following the existing catalog/sync/convert pattern; a new `KafkaProgressListener` method forwards `job.upload.progress` messages to the emitter. Expose `GET /api/assets/upload/{assetId}/observe` (SSE) so the frontend can watch a specific upload's progress.
- Frontend `DropZoneComponent`/`AssetService` changes: after the multipart POST resolves with `202`, open an `EventSource` against the observe endpoint instead of treating the POST response as final; the per-file progress row shows "Processing…" until `done=true` arrives, then reloads the asset the same way the existing `uploadComplete` batch-refresh does today.
- Add a retry/failure surface: if a consumer's processing throws, it retries with Spring Kafka's default error handling; after exhausting retries it publishes `stage` failure to `job.upload.progress` and the asset's `processing_status` becomes `FAILED`, visible on the placeholder asset so it can be manually re-triggered (a `POST /api/assets/{id}/reprocess` endpoint re-publishes `AssetUploadedEvent` for that asset without redoing already-completed stages).

## Capabilities

### New Capabilities
- `kafka-async-upload`: asynchronous, Kafka-driven post-processing of uploaded assets (hash, EXIF, thumbnail) via three independent consumer groups, with SSE-based progress reporting and partial-failure recovery via re-processing.

### Modified Capabilities
- `drag-and-drop-upload`: the `POST /api/assets/upload` response contract changes from synchronous `201 Created` + full `AssetDto` to asynchronous `202 Accepted` + `{ assetId, status }`; the "gallery refreshes after upload" and "per-file progress" requirements are updated to reflect the new async completion signal (SSE) rather than the HTTP response itself marking completion.

## Impact

- **Backend**: `AssetController.uploadAsset`, `UploadAssetUseCaseImpl`, `CatalogFolderServiceImpl` (its inline hash/EXIF/thumbnail block is extracted into the three new consumers and no longer runs for the upload path — the folder-scan catalog path is unaffected), `KafkaTopicConfig` (new topics), `KafkaProgressListener` (new method), `KafkaProgressRegistry` (no structural change, reused), new `infrastructure/kafka/AssetUploadProcessingListener.java` (or three separate listener classes), new Flyway migration, `AssetEntity`/`Asset` domain model (new fields).
- **Frontend**: `AssetService.uploadAsset`, `DropZoneComponent` (progress/completion handling), any component reading `AssetDto` immediately after upload (must now wait for the SSE `done` signal before the asset has hash/EXIF/thumbnail data).
- **Database**: new Flyway migration adding `processing_status`, `hash_completed_at`, `exif_completed_at`, `thumbnail_completed_at` columns to `assets`; relaxes `hash` and `thumbnail_creation_date_time` `NOT NULL` constraints.
- **Kafka**: new topics `asset.uploaded` and `job.upload.progress`; three new persistent consumer groups (`asset-hash-processor`, `asset-exif-processor`, `asset-thumbnail-processor`).
- **Dependencies**: none new (reuses `spring-kafka`, already a dependency since #75).
