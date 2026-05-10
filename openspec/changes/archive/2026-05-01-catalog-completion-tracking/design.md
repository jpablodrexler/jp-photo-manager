## Context

The `catalog_run_state` table (added in V2) stores per-run locking state: `is_running`, `started_at`, `last_heartbeat_at`, and `instance_id`. These fields are cleared on `release()`, so once a run finishes the table retains no evidence that a run ever completed. The proposed `last_completed_at` column persists the completion timestamp independently of the lock lifecycle.

## Goals / Non-Goals

**Goals:**
- Record when a catalog run last completed successfully.
- Expose the timestamp through the existing `CatalogRunState` entity so callers can read it without an extra query.
- Ensure the timestamp is only updated on success, not on failure or interruption.

**Non-Goals:**
- Exposing `last_completed_at` through a REST endpoint (no API change).
- Per-instance completion history (single row, last-writer-wins across instances).
- Alerting or dashboards based on the timestamp.

## Decisions

### D1 — Single nullable column, not a separate table

A separate `catalog_run_history` table would support full history but adds significant complexity (insert on every run, query for latest, retention policy). The requirement is only to know *when the last run completed* — a single nullable column on the existing row is sufficient and keeps the schema minimal.

### D2 — Update before `release()` in the `finally` block

The `markCompleted` call is placed *inside the try block*, after `doRunCatalog()` returns, and *before* the `finally` release:

```
try {
    doRunCatalog(callback);
    markCompleted();          // only reached on success
} catch (Exception e) {
    log.error(...)
} finally {
    release();
}
```

This guarantees `last_completed_at` is set only when `doRunCatalog` returns without exception. An interrupted or failed run does not update the timestamp. There is no race window: `release()` is called in `finally` immediately after, so the row is never left in an inconsistent state where `is_running=false` but `last_completed_at` was just written by a concurrent instance.

### D3 — `markCompleted` scoped to `instance_id` for safety

The UPDATE query uses `WHERE id=1 AND instance_id=:instanceId` (same guard as `release`). This prevents a lagging finally block from overwriting a timestamp written by the instance that acquired the lock after a stale-lock cleanup.

## Risks / Trade-offs

- **Multi-instance last-writer-wins:** If two instances complete within milliseconds of each other (theoretically impossible due to the distributed lock, but defensive coding) the later write wins. This is acceptable because both values represent a valid successful completion.
- **No completion record if the process crashes before `markCompleted`:** A JVM kill between `doRunCatalog` returning and `markCompleted` executing means `last_completed_at` is not updated for that run. This is intentional — a crashed process cannot be certain the run was fully committed.
