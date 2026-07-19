## ADDED Requirements

### Requirement: Audit events are persisted in an append-only MongoDB collection
The system SHALL persist audit events as documents in a MongoDB collection named `asset_audit_log`, accessed exclusively through a domain port `AuditLogRepository` with a single method `void log(AuditEvent event)`. Each `AuditEvent` SHALL carry `userId`, `action`, `entityType`, `entityId`, `timestamp`, and an optional `metadata` map whose shape varies by `action`. Documents SHALL never be updated or deleted through normal application flow (append-only); the only deletions SHALL be automatic expiry via the TTL index.

#### Scenario: Logging a tag-add action
- **GIVEN** an authenticated user adds a tag to an asset
- **WHEN** `AddTagToAssetUseCase` completes the tag write
- **THEN** `AuditLogRepository.log(...)` is called with `action=AssetTagged`, `entityType=ASSET`, `entityId=<assetId>`, and `metadata={ tagName, tagId }`

#### Scenario: Logging an asset-delete action
- **GIVEN** an authenticated user permanently deletes an asset
- **WHEN** the purge use case removes the asset
- **THEN** an audit document is written with `action=AssetDeleted`, `entityType=ASSET`, `entityId=<assetId>`, and `metadata={ folderId, permanent: true }`

#### Scenario: Audit write failure does not fail the primary request
- **GIVEN** the MongoDB connection is temporarily unavailable
- **WHEN** a use case calls `AuditLogRepository.log(...)` as part of an otherwise successful operation (e.g. tagging an asset)
- **THEN** the failure is caught, logged at `WARN`, and the primary request (tagging) still succeeds and returns normally

---

### Requirement: Per-action metadata shapes are documented and consistent
Each action type SHALL populate `metadata` with a consistent, documented shape:

| Action | metadata fields |
|---|---|
| `AssetViewed` | `{}` (no extra fields) |
| `AssetDownloaded` | `{}` (no extra fields) |
| `AssetTagged` | `{ tagName, tagId }` |
| `AssetUntagged` | `{ tagName, tagId }` |
| `AssetRated` | `{ rating }` |
| `AssetDeleted` | `{ folderId, permanent }` |
| `CatalogRun` | `{ foldersScanned, assetsAdded, durationMs }` |
| `SyncRun` | `{ sourceDir, targetDir, filesCopied, filesDeleted }` |
| `ConvertRun` | `{ sourceDir, targetDir, filesConverted }` |

#### Scenario: Catalog-run metadata reflects run results
- **GIVEN** a catalog run with `runId=42` finishes having scanned 3 folders and added 120 assets in 5,340 ms
- **WHEN** the `done=true` message for `runId=42` is consumed
- **THEN** an audit document is written with `action=CatalogRun`, `entityType=CATALOG_RUN`, `entityId="42"`, and `metadata={ foldersScanned: 3, assetsAdded: 120, durationMs: 5340 }`

---

### Requirement: A Kafka consumer durably persists already-published lifecycle and job-completion events
The system SHALL run a `@KafkaListener` (consumer group `audit-log-writer`) subscribed to `asset.cataloged`, `asset.deleted`, `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`. For `job.*.progress` topics, only messages with `done=true` SHALL produce an audit event; intermediate progress messages SHALL be ignored by this listener.

#### Scenario: asset.cataloged event produces an audit entry
- **WHEN** an `AssetCatalogedEvent{assetId=7, folderPath="/photos/2024", timestamp}` is published to `asset.cataloged`
- **THEN** `AuditLogKafkaListener` writes an audit document with `action=AssetCataloged`, `entityType=ASSET`, `entityId="7"`

#### Scenario: Intermediate catalog progress message is ignored
- **WHEN** a `CatalogProgressMessage{runId=42, done=false}` arrives on `job.catalog.progress`
- **THEN** `AuditLogKafkaListener` does not write any audit document for that message

#### Scenario: Catalog job completion produces a single run-level audit entry
- **WHEN** a `CatalogProgressMessage{runId=42, done=true}` arrives on `job.catalog.progress`
- **THEN** `AuditLogKafkaListener` writes exactly one audit document with `action=CatalogRun` and `entityId="42"`

#### Scenario: Sync job completion produces a run-level audit entry with results
- **WHEN** a `SyncProgressMessage{runId=8, done=true, results=[...]}` arrives on `job.sync.progress`
- **THEN** `AuditLogKafkaListener` writes an audit document with `action=SyncRun`, `entityId="8"`, and `metadata` derived from the results

#### Scenario: Single consumer group avoids duplicate writes across instances
- **GIVEN** two application instances are both members of the `audit-log-writer` consumer group
- **WHEN** an `AssetDeletedEvent` is published to `asset.deleted` (3 partitions)
- **THEN** exactly one of the two instances processes that message and writes the corresponding audit document, per Kafka's consumer-group partition assignment

---

### Requirement: Direct port calls cover actions with no existing Kafka topic
Use cases for actions that have no corresponding Kafka topic SHALL call `AuditLogRepository.log(...)` directly and synchronously as part of handling the request: opening the full-size asset viewer (`AssetViewed`), downloading an asset (`AssetDownloaded`), adding or removing a tag (`AssetTagged` / `AssetUntagged`), and changing a star rating (`AssetRated`).

#### Scenario: Viewing an asset logs an AssetViewed event
- **WHEN** an authenticated user opens the full-size viewer for `assetId=15`
- **THEN** an audit document is written with `action=AssetViewed`, `entityType=ASSET`, `entityId="15"`, `userId=<the authenticated user's id>`

#### Scenario: Rating an asset logs an AssetRated event
- **WHEN** an authenticated user sets a 4-star rating on `assetId=22`
- **THEN** an audit document is written with `action=AssetRated`, `entityId="22"`, `metadata={ rating: 4 }`

---

### Requirement: Compound and TTL indexes are created automatically at startup
The `asset_audit_log` collection SHALL have a compound index on `{ userId: 1, timestamp: -1 }` supporting per-user history queries sorted newest-first, and a TTL index on `timestamp` that expires documents after 365 days. Both indexes SHALL be created idempotently at application startup (safe to run repeatedly against an existing collection) by extending the existing `MongoIndexInitializer`.

#### Scenario: Indexes exist after startup
- **WHEN** the application starts against a MongoDB instance where the `asset_audit_log` collection does not yet have these indexes
- **THEN** both the `{ userId: 1, timestamp: -1 }` compound index and the TTL index on `timestamp` (365-day expiry) exist on the collection after startup completes

#### Scenario: Startup is idempotent when indexes already exist
- **WHEN** the application restarts and both indexes already exist with the expected definitions
- **THEN** no duplicate index is created and startup does not fail

#### Scenario: Documents older than the retention period are purged automatically
- **GIVEN** an audit document with `timestamp` more than 365 days in the past
- **WHEN** MongoDB's TTL monitor runs its periodic sweep
- **THEN** the expired document is automatically removed from `asset_audit_log` with no application code involved

---

### Requirement: Paginated read API for audit history
The system SHALL expose `GET /api/audit-log` returning a paginated list of audit entries sorted by `timestamp` descending, filterable by `userId`, `entityId`, `from`, and `to` (ISO-8601 timestamps), with `page` and `size` query parameters. A non-admin authenticated user SHALL only be able to query their own `userId` (the `userId` filter defaults to the authenticated principal and any other value SHALL be rejected). A user with the `ADMIN` role SHALL be permitted to query any `userId`, including omitting the filter to retrieve entries across all users.

#### Scenario: Non-admin user queries their own audit history
- **GIVEN** an authenticated non-admin user with id `5`
- **WHEN** they call `GET /api/audit-log`
- **THEN** the response contains only audit entries where `userId=5`, sorted by `timestamp` descending, paginated according to `page`/`size`

#### Scenario: Non-admin user attempts to query another user's audit history
- **GIVEN** an authenticated non-admin user with id `5`
- **WHEN** they call `GET /api/audit-log?userId=9`
- **THEN** the response is `403 Forbidden`

#### Scenario: Admin queries a specific user's audit history
- **GIVEN** an authenticated user with the `ADMIN` role
- **WHEN** they call `GET /api/audit-log?userId=9`
- **THEN** the response contains audit entries where `userId=9`, sorted by `timestamp` descending

#### Scenario: Admin queries audit history filtered by entity and time range
- **GIVEN** an authenticated user with the `ADMIN` role
- **WHEN** they call `GET /api/audit-log?entityId=15&from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z`
- **THEN** the response contains only entries for `entityId=15` with `timestamp` within the given range
