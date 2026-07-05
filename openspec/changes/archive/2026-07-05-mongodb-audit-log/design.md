## Context

The backend has no durable log of user actions. Two categories of events already exist that a new consumer could tap into: (1) Kafka events published by `kafka-catalog-pipeline` (#75) — `asset.cataloged`, `asset.deleted`, and the three `job.*.progress` topics — and (2) actions with no existing Kafka event at all (viewing an image, downloading an image, adding/removing a tag, changing a star rating). MongoDB is already provisioned and used for `asset_exif` (`mongodb-exif-store`, #72), with `MongoIndexInitializer` as the established place to register indexes at startup. This change adds a second MongoDB collection, `asset_audit_log`, following the same adapter-behind-a-domain-port pattern already used for `AssetExifRepository`.

## Goals / Non-Goals

**Goals:**
- Durable, append-only audit trail covering view, download, tag, rate, delete, sync-run, convert-run, and catalog-run actions.
- Zero Flyway migrations — MongoDB's schemaless documents absorb new action types and per-action metadata shapes without a schema change.
- Reuse the existing Kafka topics wherever an event is already published; only add direct port calls for actions that have no corresponding topic.
- Keep `AuditLogRepository` a plain domain port so the Kafka consumer and any future producer (e.g., a REST admin action) can write through the same interface.
- Bound collection growth with a TTL index (365 days) rather than requiring manual cleanup.

**Non-Goals:**
- No UI is being added in this change beyond the read API; a "recent activity" panel is a candidate follow-up, not scope here.
- No retroactive backfill of historical actions — audit logging starts from the point this change is deployed.
- No exactly-once delivery guarantee for the Kafka-consumed events. Kafka's at-least-once semantics mean a duplicate audit entry is possible on consumer restart/rebalance; this is acceptable for an audit trail (duplicates are a lesser risk than gaps) and is called out as a known trade-off below.
- Not building a generic "event sourcing" system — `asset_audit_log` is a read-optimized log, not a system of record that other use cases replay from.

## Decisions

**1. Domain port shape: single `log(AuditEvent event)` method, not per-action methods.**
A single generic method keeps the port stable as new action types are added — callers construct an `AuditEvent` with an `action` enum/string and a `metadata` map rather than requiring a new port method (and mock/test churn) per action. Alternative considered: one method per action type (`logTagAdded(...)`, `logAssetDeleted(...)`) — rejected because it would require touching the port and every implementation each time a new action type is introduced, working against the "no schema/contract churn" goal of using MongoDB in the first place.

**2. Dedicated Kafka consumer group `audit-log-writer`, separate from `sse-broadcaster-*`.**
The existing `KafkaProgressListener` consumer group is per-instance (`sse-broadcaster-{instance}`) so that every instance receives every message for local SSE fan-out. Audit logging is the opposite: each event must be written exactly-once-ish (at-least-once is fine, but not once-per-instance), so `AuditLogKafkaListener` uses a single shared consumer group `audit-log-writer` — Kafka's consumer group protocol ensures only one instance processes each partition, avoiding duplicate audit rows when multiple app instances are running.

**3. Direct port calls for actions with no existing topic (view, download, tag, rate).**
Introducing a Kafka topic for every fine-grained action (e.g. `asset.viewed`) would add topic-management overhead disproportionate to the value for this change. These four action types call `AuditLogRepository.log(...)` synchronously from the existing use case, matching the "direct port call fallback" pattern already described for this improvement's design intent (`asset-tagging` and `star-ratings` use cases gain the call inline). Alternative considered: publish new Kafka topics for these too, for consumency — rejected as unnecessary indirection when the write is already happening inside a use case that can call the port directly.

**4. Reuse `job.*.progress` `done=true` messages for run-level audit entries (catalog-run, sync-run, convert-run), not per-item messages.**
Per-item progress messages (one per asset/file) would flood the audit log with noise; only the final `done=true` message per `runId`, which already carries the run's high-level results, is mapped to a single `AuditEvent`. Non-`done` progress messages are ignored by `AuditLogKafkaListener`.

**5. Index design: compound `{ userId: 1, timestamp: -1 }` plus a TTL index on `timestamp`.**
The compound index supports the primary read pattern (`GET /api/audit-log` filtered and sorted per user, newest first). The TTL index (`expireAfterSeconds` corresponding to 365 days) bounds unbounded growth automatically, consistent with the retention policy called out in `improvements.md`. Both indexes are registered idempotently at startup by extending `MongoIndexInitializer`, the same class that already manages the `asset_exif` indexes.

**6. Read API is scoped to the authenticated user unless the caller has `ADMIN` role.**
`GET /api/audit-log` defaults `userId` to the authenticated principal; a non-admin user supplying a different `userId` gets `403 Forbidden`. Admins may query any `userId` (or omit it for all users), reusing the `role-based-access-control` (#33, already implemented) `@PreAuthorize` pattern seen in `database-backup` (#44).

## Risks / Trade-offs

- [Consumer rebalance can produce duplicate audit entries] → Acceptable for an audit trail; documents include enough fields (`entityId`, `action`, `timestamp` to the millisecond) that duplicates are identifiable if ever needed, and no downstream logic currently depends on exact-once counts.
- [Kafka consumer lag delays audit visibility for catalog/sync/convert events] → Acceptable; the read API is for historical review, not real-time monitoring (which is already covered by SSE progress streams).
- [Adding direct `auditLogRepository.log(...)` calls inside existing use cases (tag, rate, view, download) introduces a new dependency and a possible failure point in synchronous request paths] → Mirror the `mongodb-exif-store` and `redis-refresh-tokens` precedent: wrap the call in a try/catch, log at `WARN` on failure, and never fail the primary request because of an audit-write failure.
- [Unbounded metadata map shape drifts over time as new action types are added] → Documented per-action metadata shapes live in the spec and in `improvements.md`; this is an accepted trade-off of the schemaless design and is why MongoDB was chosen over a new PostgreSQL table.

## Migration Plan

1. Add `AuditLogRepository` port, `AuditEvent` domain model, `MongoAuditLogRepositoryImpl`, `AuditLogDocument`.
2. Extend `MongoIndexInitializer` to create the `asset_audit_log` compound and TTL indexes at startup (idempotent — safe to run against an existing collection).
3. Add `AuditLogKafkaListener` (`audit-log-writer` consumer group) subscribed to `asset.cataloged`, `asset.deleted`, and the `done=true` case of `job.catalog.progress`, `job.sync.progress`, `job.convert.progress`.
4. Add direct `auditLogRepository.log(...)` calls to the tag add/remove, star-rating change, asset view, and asset download use cases.
5. Add `GetAuditLogUseCase`, `AuditLogController`, and `AuditLogEntryResponse` DTO for the read API.
6. No data migration and no rollback complexity beyond removing the new classes and Kafka subscription — no existing table or behavior is altered.

## Open Questions

- Should "asset view" be logged on every gallery thumbnail render, or only on opening the full-size viewer? This design assumes the latter (viewer open) to avoid a write-per-thumbnail-scroll flood; confirm during implementation if product intent differs.
- Should the 365-day TTL be configurable via `application.yml` rather than hardcoded? Deferred as a follow-up; hardcoding matches the simplicity bar for this first iteration.
