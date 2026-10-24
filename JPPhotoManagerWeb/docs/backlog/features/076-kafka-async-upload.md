# Feature 76 — kafka-async-upload

**As a** photo library owner, **I want** uploads to return immediately while processing continues in the background, **so that** big files do not block me.

## Acceptance Criteria
- AC1: Given a valid upload, when I send it, then the file is saved, an event is published and HTTP 202 is returned before processing completes.
- AC2: Given the upload row, when the file transfer ends, then it shows Processing; when all stages finish, then a success indicator is shown; on failure, an error indicator.
- AC3: Given the asset, when the upload returns, then a placeholder asset is immediately visible in listings.
- AC4: Given hashing, EXIF extraction and thumbnail generation, when they run, then three independent consumer groups process them in parallel and one failing stage does not block or roll back the others.
- AC5: Given several instances, when events arrive, then each stage processes every event exactly once.
- AC6: Given the stages, when the last finishes, then the status flips to COMPLETED and concurrent completion detection is idempotent.
- AC7: Given a stage fails, when retries are exhausted, then it is marked failed; I can reprocess after an EXIF failure.
- AC8: Given the client, when it observes the upload, then it sees per-stage progress and the final completion.

## Technical notes

Decouple the `POST /api/assets/upload` HTTP thread from SHA-256 hashing, EXIF extraction, and thumbnail generation; the controller saves the file to disk, publishes an `AssetUploadedEvent { filePath, assetId, userId }` to the `asset.uploaded` Kafka topic, and returns HTTP 202; three independent consumer groups process hash computation, EXIF extraction, and thumbnail generation in parallel; eliminates multi-second blocking for large files (RAW 40–80 MB, video) and allows each processing stage to scale independently; requires the Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
