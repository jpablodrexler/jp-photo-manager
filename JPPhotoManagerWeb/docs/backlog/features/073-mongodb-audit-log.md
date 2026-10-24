# Feature 73 — mongodb-audit-log

**As a** administrator, **I want** an audit log of user actions, **so that** I can see who did what and when.

## Acceptance Criteria
- AC1: Given an action (view, download, tag, rate, delete, catalog run, sync run, convert run), when it occurs, then an append-only entry is stored with user, action, entity, time and action-specific metadata.
- AC2: Given the audit write fails, when the primary request runs, then the request still succeeds.
- AC3: Given catalog events, when asset.cataloged arrives, then an entry is written; intermediate progress is ignored and a completed job produces one run-level entry with its results; a sync completion does the same.
- AC4: Given several instances, when events arrive, then a single consumer group avoids duplicate writes.
- AC5: Given the startup, when indexes are created, then it is idempotent.
- AC6: Given the retention period, when it elapses, then older documents are purged automatically.
- AC7: Given the history API, when a non-admin queries, then they see only their own; querying another user's is refused; an admin can query any user filtered by entity and time range.

## Technical notes

Append-only MongoDB collection `asset_audit_log` recording every user action — view, download, tag, rate, delete, sync-run, convert-run, catalog-run — as a flexible document `{ userId, action, entityType, entityId, timestamp, metadata: { … } }`; each event type carries its own `metadata` shape (e.g. tag events include `tagName`; delete events include `folderId` and `permanent` flag) without schema migrations; time-range queries and per-user activity feeds are native aggregation pipeline operations; a new `AuditLogRepository` port interface (domain) with a `MongoAuditLogRepositoryImpl` adapter (infrastructure) keeps MongoDB out of the use-case layer; pairs with `kafka-catalog-pipeline` (#75) as the natural durable consumer of `asset.cataloged` and `asset.deleted` Kafka events

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
