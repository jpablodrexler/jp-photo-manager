## ADDED Requirements

### Requirement: Upload publishes an event and returns immediately
When a file has been validated and written to disk, the system SHALL persist a placeholder `Asset` row with `processing_status = PENDING` and no hash or thumbnail data, publish an `AssetUploadedEvent { assetId, filePath, folderPath, fileName }` to the `asset.uploaded` Kafka topic keyed by `String.valueOf(assetId)`, and return `202 Accepted` with `{ assetId, status: "PROCESSING" }` without waiting for hashing, EXIF extraction, or thumbnail generation to complete.

#### Scenario: Upload returns before processing completes
- **GIVEN** a valid JPEG file is posted to `POST /api/assets/upload` with a cataloged `folderPath`
- **WHEN** the backend finishes writing the file to disk and persisting the placeholder asset row
- **THEN** the response is `202 Accepted` with a body `{ assetId: <id>, status: "PROCESSING" }`, returned before hash computation, EXIF extraction, or thumbnail generation begin

#### Scenario: Placeholder asset is immediately visible in listings
- **GIVEN** an upload has just returned `202 Accepted` with `assetId=123`
- **WHEN** the client calls `GET /api/assets?folderPath=`
- **THEN** the response includes an asset with `assetId=123` and `processing_status="PENDING"` (or `"PROCESSING"`), even though its hash and thumbnail are not yet populated

---

### Requirement: Three independent consumer groups process hash, EXIF, and thumbnail generation
The system SHALL run three independent, persistent Kafka consumer groups — `asset-hash-processor`, `asset-exif-processor`, and `asset-thumbnail-processor` — each subscribed to `asset.uploaded`. Each group SHALL process every `AssetUploadedEvent` exactly once, independently of the other two groups and independently of how many application instances are running.

The hash processor SHALL compute the SHA-256 hash via the existing hashing capability and persist it to `Asset.hash`, setting `hash_completed_at`. The EXIF processor SHALL extract EXIF metadata and upsert it into the `AssetExifRepository` (MongoDB), setting `exif_completed_at`. The thumbnail processor SHALL generate a 200x150 thumbnail and persist it via the thumbnail storage port, setting `Asset.thumbnailCreationDateTime` and `thumbnail_completed_at`. Each processor SHALL commit its own result in its own transaction, independent of the other two.

#### Scenario: Hash processor updates only the hash fields
- **WHEN** an `AssetUploadedEvent{assetId=123}` is consumed by `asset-hash-processor`
- **THEN** `Asset{assetId=123}.hash` is set to the computed SHA-256 value and `hash_completed_at` is set to the current timestamp, independent of whether the EXIF or thumbnail processors have run yet

#### Scenario: One stage failing does not block or roll back the others
- **GIVEN** EXIF extraction throws an exception for `assetId=123` (e.g. corrupt EXIF segment)
- **WHEN** the hash and thumbnail processors have already committed their results for `assetId=123`
- **THEN** `Asset{assetId=123}.hash` and thumbnail data remain persisted and unaffected by the EXIF processor's failure

#### Scenario: Multiple application instances each process every event exactly once per stage
- **GIVEN** two backend instances are running, each with the same three persistent consumer group ids
- **WHEN** an `AssetUploadedEvent` is published
- **THEN** exactly one instance's `asset-hash-processor` consumer processes it, exactly one instance's `asset-exif-processor` consumer processes it, and exactly one instance's `asset-thumbnail-processor` consumer processes it (Kafka consumer-group semantics: one member per group receives each partition's messages)

---

### Requirement: Processing status transitions to COMPLETED only when all three stages finish
The system SHALL track `processing_status` on each `Asset` as `PENDING`, `PROCESSING`, `COMPLETED`, or `FAILED`. When a processor commits its stage, it SHALL check whether the other two stages' completion timestamps are already non-null; if so, it SHALL set `processing_status = COMPLETED` and publish a final `job.upload.progress` message with `done=true` for that `assetId`. This update SHALL be idempotent so that a race between two processors observing "all three complete" simultaneously does not produce inconsistent state.

#### Scenario: Last stage to finish flips status to COMPLETED
- **GIVEN** `assetId=123` already has `hash_completed_at` and `exif_completed_at` set (non-null)
- **WHEN** the thumbnail processor commits `thumbnail_completed_at`
- **THEN** `Asset{assetId=123}.processing_status` becomes `COMPLETED` and a `job.upload.progress` message `{assetId=123, done=true}` is published

#### Scenario: Concurrent completion detection is idempotent
- **GIVEN** two processors simultaneously observe that all three completion timestamps are non-null for `assetId=123`
- **WHEN** both attempt to set `processing_status = COMPLETED`
- **THEN** the asset's `processing_status` ends as `COMPLETED` exactly once with no error, and at most the SSE client receives a harmless duplicate `done=true` event

---

### Requirement: Upload progress is observable via Server-Sent Events
The system SHALL expose `GET /api/assets/upload/{assetId}/observe` returning an `SseEmitter` registered in `KafkaProgressRegistry` keyed by `assetId`. As each processor publishes an `UploadProgressMessage{assetId, stage, done}` to `job.upload.progress`, the system SHALL forward it to the registered emitter, following the same Kafka-to-SSE bridging pattern used for catalog, sync, and convert progress. When `done=true` is received, the emitter SHALL be completed and removed from the registry.

#### Scenario: Client observes per-stage progress
- **GIVEN** a client has called `GET /api/assets/upload/123/observe` immediately after receiving `202 Accepted` for `assetId=123`
- **WHEN** the hash processor commits and publishes progress
- **THEN** the client's SSE stream receives an event indicating the `hash` stage completed for `assetId=123`

#### Scenario: Client observes final completion
- **WHEN** all three stages have completed for `assetId=123` and the final `done=true` message is published
- **THEN** the client's SSE stream receives a final event and the connection is closed via `emitter.complete()`

---

### Requirement: Failed processing can be re-triggered without redoing completed stages
If a processor exhausts its retry attempts for a given `AssetUploadedEvent`, the system SHALL set `processing_status = FAILED` on the corresponding asset. The system SHALL expose `POST /api/assets/{id}/reprocess`, which re-publishes an `AssetUploadedEvent` for that `assetId` to `asset.uploaded`. Each processor SHALL be idempotent, so re-running all three processors after a partial failure produces the same correct end state as if all three had succeeded on the first attempt.

#### Scenario: Reprocessing after EXIF failure
- **GIVEN** `assetId=123` has `processing_status = FAILED` because the EXIF processor exhausted its retries, while hash and thumbnail already completed successfully
- **WHEN** an admin calls `POST /api/assets/123/reprocess`
- **THEN** a new `AssetUploadedEvent{assetId=123}` is published; the hash and thumbnail processors recompute and overwrite their fields with the same values, the EXIF processor succeeds this time, and `processing_status` becomes `COMPLETED`

#### Scenario: Retries are attempted before marking failed
- **GIVEN** the EXIF processor throws a transient exception the first time it processes `assetId=123`
- **WHEN** the Kafka listener's error handler retries the message
- **AND** the second attempt succeeds
- **THEN** `exif_completed_at` is set normally and `processing_status` is never set to `FAILED` for this asset
