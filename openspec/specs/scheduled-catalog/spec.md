## ADDED Requirements

### Requirement: Startup trigger

The backend SHALL start the first catalog run as soon as the application is fully initialized, with no manual intervention.

#### Scenario: First run on application ready
WHEN the Spring application context publishes `ApplicationReadyEvent`
THEN `CatalogScheduler` submits a catalog run to `ThreadPoolTaskScheduler` with an initial delay of zero

---

### Requirement: Periodic scheduling with fixed delay

The backend SHALL repeat catalog runs with a fixed delay between the end of one run and the start of the next, equal to the value of `photomanager.catalog-cooldown-minutes`.

#### Scenario: Cooldown observed after run completes
WHEN a catalog run finishes
THEN the next run starts no sooner than `catalog-cooldown-minutes` minutes after completion

#### Scenario: Cooldown not applied during a run
WHEN a catalog run is in progress
THEN no additional run is scheduled to start before the current one ends

---

### Requirement: Distributed lock via database table

The backend SHALL use the `catalog_run_state` table as the single source of truth for catalog execution state across all JVM instances.

The table SHALL contain exactly one row with columns: `id` (PK, always 1), `is_running` (boolean), `started_at` (timestamptz), `last_heartbeat_at` (timestamptz), `instance_id` (varchar).

#### Scenario: Lock acquired before run starts
WHEN a catalog run is about to start
THEN the backend executes `UPDATE catalog_run_state SET is_running=true … WHERE id=1 AND is_running=false`
AND the run proceeds only if 1 row was updated

#### Scenario: Lock skipped when already held
WHEN the lock is held by any instance (is_running=true)
AND another instance or the API endpoint attempts to start a run
THEN the attempt is silently skipped and the existing run continues unaffected

#### Scenario: Lock released after run ends
WHEN a catalog run completes (successfully or with error)
THEN `is_running` is set to false and `instance_id`, `started_at`, `last_heartbeat_at` are cleared
AND the release uses `WHERE instance_id = :thisInstance` to prevent accidentally releasing another instance's lock

---

### Requirement: Per-batch heartbeat

The backend SHALL update `last_heartbeat_at` in `catalog_run_state` after every `catalog-batch-size` assets are persisted during a catalog run.

The heartbeat update SHALL commit in its own transaction (propagation `REQUIRES_NEW`) so it is immediately visible to other JVM instances regardless of the enclosing folder transaction state.

#### Scenario: Heartbeat refreshed mid-folder
WHEN `catalog-batch-size` assets have been saved within a folder
THEN `last_heartbeat_at` is updated to the current time and committed immediately

#### Scenario: Heartbeat visible to all instances
WHEN the heartbeat is committed via REQUIRES_NEW
THEN other JVM instances querying `catalog_run_state` see the updated timestamp immediately

---

### Requirement: Stale run detection and cleanup

The backend SHALL detect catalog runs that have not produced a heartbeat within `photomanager.catalog-timeout` minutes and SHALL clean them up.

Cleanup SHALL:
1. Interrupt the catalog thread if this JVM instance holds the stale lock.
2. Release the DB lock (`is_running = false`) so other instances can acquire it.

A `@Scheduled(fixedDelay = 60_000)` task SHALL perform this check every 60 seconds.

#### Scenario: Stale run on the same instance is interrupted
WHEN `last_heartbeat_at` is older than `catalog-timeout` minutes
AND `instance_id` matches this JVM's `catalogInstanceId`
THEN the running catalog thread is interrupted
AND the DB lock is released

#### Scenario: Stale lock from a crashed remote instance is released
WHEN `last_heartbeat_at` is older than `catalog-timeout` minutes
AND `instance_id` does not match this JVM's `catalogInstanceId`
THEN the DB lock is released
AND no local thread interruption is attempted

#### Scenario: Non-stale run is not disturbed
WHEN `last_heartbeat_at` is within the last `catalog-timeout` minutes
THEN the stale check takes no action

---

### Requirement: API endpoint availability

The `GET /api/assets/catalog` SSE endpoint SHALL remain available for manual and troubleshooting use.

When the endpoint is called while a catalog run is already in progress, the backend SHALL skip the run (same lock-acquire logic) and return an SSE stream that completes immediately with no events.

---

### Requirement: Configuration

`photomanager.catalog-cooldown-minutes` (integer, existing) SHALL control the fixed delay between catalog runs.

`photomanager.catalog-timeout` (integer, minutes, default 60) SHALL be added to `application.yml` and SHALL control the stale-run threshold.

`photomanager.catalog-batch-size` (integer, existing, default 1000) SHALL control the number of assets persisted between heartbeat refreshes.
