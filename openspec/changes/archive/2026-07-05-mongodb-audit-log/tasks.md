## 1. Domain layer

- [x] 1.1 Create `AuditEvent` domain model (`domain/model/` or co-located with other domain models) with fields `userId`, `action` (enum: `AssetViewed`, `AssetDownloaded`, `AssetTagged`, `AssetUntagged`, `AssetRated`, `AssetDeleted`, `AssetCataloged`, `CatalogRun`, `SyncRun`, `ConvertRun`), `entityType` (enum: `ASSET`, `CATALOG_RUN`, `SYNC_RUN`, `CONVERT_RUN`), `entityId`, `timestamp`, and an optional `Map<String, Object> metadata`.
- [x] 1.2 Create `AuditLogRepository` port in `domain/port/out/` with a single method `void log(AuditEvent event)`.

## 2. MongoDB persistence adapter

- [x] 2.1 Create `AuditLogDocument` Spring Data MongoDB document class in `infrastructure/persistence/mongo/` mapping to the `asset_audit_log` collection.
- [x] 2.2 Create `MongoAuditLogRepository` (Spring Data MongoDB repository interface) in `infrastructure/persistence/mongo/`.
- [x] 2.3 Create `MongoAuditLogRepositoryImpl` adapter in `infrastructure/persistence/adapter/` implementing `AuditLogRepository`, mapping `AuditEvent` ↔ `AuditLogDocument` and delegating to `MongoAuditLogRepository`.
- [x] 2.4 Extend `MongoIndexInitializer` to ensure a compound index `{ userId: 1, timestamp: -1 }` and a TTL index on `timestamp` (365-day `expireAfterSeconds`) exist on `asset_audit_log` at startup, following the same idempotent pattern used for the `asset_exif` indexes.

## 3. Kafka consumer for already-published events

- [x] 3.1 Create `AuditLogKafkaListener` in `infrastructure/kafka/` with `@KafkaListener` methods (consumer group `audit-log-writer`, explicit — not per-instance like `sse-broadcaster-*`) subscribed to `asset.cataloged`, `asset.deleted`, `job.catalog.progress`, `job.sync.progress`, `job.convert.progress`.
- [x] 3.2 Map `AssetCatalogedEvent` → `AuditEvent{action=AssetCataloged, entityType=ASSET, entityId=assetId}` and call `AuditLogRepository.log(...)`.
- [x] 3.3 Map `AssetDeletedEvent` → `AuditEvent{action=AssetDeleted, entityType=ASSET, entityId=assetId, metadata={folderId, permanent}}`.
- [x] 3.4 For `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`: ignore messages where `done=false`; for `done=true` messages, map to `AuditEvent{action=CatalogRun|SyncRun|ConvertRun, entityType=CATALOG_RUN|SYNC_RUN|CONVERT_RUN, entityId=runId, metadata=<run-specific fields per the spec table>}`.
- [x] 3.5 Wrap each `AuditLogRepository.log(...)` call in the listener in a try/catch that logs at `WARN` on failure without throwing (a failed audit write must not cause message redelivery loops).

## 4. Direct port calls for actions with no existing topic

- [x] 4.1 Add an `AuditLogRepository.log(...)` call to `AddTagToAssetUseCaseImpl` (`action=AssetTagged`, `metadata={tagName, tagId}`) and `RemoveTagFromAssetUseCaseImpl` (`action=AssetUntagged`, same metadata shape).
- [x] 4.2 Add an `AuditLogRepository.log(...)` call to `RateAssetUseCaseImpl` (`action=AssetRated`, `metadata={rating}`).
- [x] 4.3 Add an `AuditLogRepository.log(...)` call to `StreamAssetUseCaseImpl` or `GetAssetImageUseCaseImpl` (whichever backs the full-size viewer open, per design's "viewer open" interpretation) with `action=AssetViewed`.
- [x] 4.4 Add an `AuditLogRepository.log(...)` call to `DownloadAssetsUseCaseImpl` with `action=AssetDownloaded`.
- [x] 4.5 Wrap each of the four call sites above in a try/catch (or a shared helper) that logs at `WARN` on failure and never fails the primary request.

## 5. Read API

- [x] 5.1 Create `GetAuditLogUseCase` port (`domain/port/in/`) and `GetAuditLogUseCaseImpl` (`application/usecase/`) accepting `userId`, `entityId`, `from`, `to`, `page`, `size` filters and returning a paginated result sorted by `timestamp` descending.
- [x] 5.2 Add a `findByFilters(...)` query method to `MongoAuditLogRepository` (or `AuditLogRepository` port, if pagination/filtering belongs at the port level) covering the filter combinations above.
- [x] 5.3 Create `AuditLogEntryResponse` DTO in `api/dto/` (or existing DTO package) and `AuditLogController` in `infrastructure/web/controller/` exposing `GET /api/audit-log`.
- [x] 5.4 Enforce authorization: non-admin users may only query their own `userId` (default to the authenticated principal; reject any other value with `403`); `ADMIN` role may query any `userId` or omit it, following the `@PreAuthorize` pattern used by `database-backup`'s admin endpoints.

## 6. Tests

- [x] 6.1 Unit test `MongoAuditLogRepositoryImpl` (mapping correctness, delegate calls) with Mockito.
- [x] 6.2 Unit test `AuditLogKafkaListener` covering: `asset.cataloged` → audit write, `asset.deleted` → audit write, `done=false` progress messages ignored, `done=true` progress messages → single run-level audit write, and a MongoDB failure being caught and logged without throwing.
- [x] 6.3 Unit test the four direct-call sites (`AddTagToAssetUseCaseImpl`, `RemoveTagFromAssetUseCaseImpl`, `RateAssetUseCaseImpl`, viewer/download use cases) verifying `AuditLogRepository.log(...)` is invoked with the expected `AuditEvent` and that a thrown exception from the port does not propagate to the caller.
- [x] 6.4 Unit test `GetAuditLogUseCaseImpl` filter and pagination logic.
- [x] 6.5 Unit test `AuditLogController` authorization rules (non-admin querying own vs. another user's `userId`; admin querying any `userId`).
- [x] 6.6 Integration test (extending the project's MongoDB test base, mirroring `mongodb-exif-store` integration tests) verifying the compound and TTL indexes are created on a real MongoDB instance at startup.

## 7. Documentation

- [x] 7.1 Update `CLAUDE.md` web-architecture section to document the `asset_audit_log` MongoDB collection, the `audit-log-writer` Kafka consumer group, and the new `GET /api/audit-log` endpoint, following the existing style used for `mongodb-exif-store` and `redis-refresh-tokens` entries.
