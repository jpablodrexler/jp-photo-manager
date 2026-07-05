## Why

The application has no durable record of who did what to which asset or when — there is no way to answer "who deleted this photo" or "when was this folder last catalogued" after the fact. A flexible, append-only audit trail is needed for accountability and troubleshooting, and it should not require a schema migration every time a new action type or metadata field is added. MongoDB (already provisioned for `asset_exif` by `mongodb-exif-store`) and Kafka (already provisioned by `kafka-catalog-pipeline`) make this cheap to add now: catalog and delete events already flow through `asset.cataloged` and `asset.deleted`, so a dedicated consumer group can durably persist them without touching the use-case layer.

## What Changes

- Add a new `AuditLogRepository` domain port (`domain/port/out/`) with a single `void log(AuditEvent event)` method.
- Add a `MongoAuditLogRepositoryImpl` adapter (`infrastructure/persistence/mongo`) that writes documents to a new append-only `asset_audit_log` MongoDB collection.
- Add a `MongoIndexInitializer`-managed compound index on `{ userId: 1, timestamp: -1 }` and a TTL index on `timestamp` (365-day expiry) for the `asset_audit_log` collection, created automatically at startup.
- Add an `AuditEvent` domain model carrying `userId`, `action`, `entityType`, `entityId`, `timestamp`, and an optional free-form `metadata` map whose shape varies per action type (tag, delete, catalog-run, sync-run, convert-run, view, download, rate).
- Add a Kafka consumer (`AuditLogKafkaListener`, consumer group `audit-log-writer`) subscribed to `asset.cataloged`, `asset.deleted`, `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`, which maps each message to an `AuditEvent` and calls `AuditLogRepository.log(...)`.
- Wire direct `AuditLogRepository.log(...)` calls from use cases whose actions are not carried by an existing Kafka topic: asset view, asset download, tag add/remove, and star-rating change.
- Expose `GET /api/audit-log?userId=&entityId=&from=&to=&page=&size=` returning paginated audit entries for the authenticated user (or, for an `ADMIN` user, optionally any user via the `userId` filter), sorted by `timestamp` descending.

## Capabilities

### New Capabilities
- `mongodb-audit-log`: append-only MongoDB-backed audit trail of user actions (view, download, tag, rate, delete, sync-run, convert-run, catalog-run), populated both by a dedicated Kafka consumer for already-published events and by direct port calls for actions with no existing topic; exposes a paginated read API.

### Modified Capabilities
(none — this change only adds new components; no existing capability's requirements change)

## Impact

- **New domain code**: `AuditLogRepository` port, `AuditEvent` domain model.
- **New infrastructure code**: `MongoAuditLogRepositoryImpl`, `AuditLogKafkaListener`, `AuditLogDocument` (Spring Data MongoDB document), index registration in `MongoIndexInitializer`.
- **New API code**: `AuditLogController`, `AuditLogEntryResponse` DTO, `GetAuditLogUseCase`.
- **Modified use cases**: tag add/remove, star-rating change, asset view/download, and (if not already covered by the Kafka consumer) soft-delete/purge use cases gain a direct `auditLogRepository.log(...)` call.
- **Infrastructure**: no new Docker service — reuses the MongoDB container from `mongodb-exif-store` (#72) and the Kafka cluster from `kafka-catalog-pipeline` (#75), both already running.
- **No Flyway migration**: PostgreSQL schema is untouched.
