## Context

The catalog process scans root directories, generates thumbnails, and persists asset metadata. `GalleryComponent.ngOnInit()` calls `startCatalog()` on every page load, opening an SSE stream to `GET /api/assets/catalog`. In a multi-user deployment:

- N simultaneous users trigger N concurrent catalog runs.
- No minimum interval between runs.
- No protection against hung runs (blocked I/O, deadlock).
- `photomanager.catalog-cooldown-minutes = 2` is configured but never read.

`CatalogFolderServiceImpl.catalogFolder()` is `@Transactional` and processes all assets in a folder within a single transaction. The asset loop inside that method is where heartbeats must fire, every `catalog-batch-size` assets, to support collections so large a single folder could take hours.

## Goals / Non-Goals

**Goals:**
- Backend owns the catalog lifecycle: first run on app-ready, then fixed delay of `catalog-cooldown-minutes`.
- Exactly one run active at a time across all JVM instances (distributed guarantee via DB).
- Heartbeat per `catalog-batch-size` assets keeps long-running catalogs alive past any single-run timeout.
- Stale runs (no heartbeat for `catalog-timeout` minutes) are detected, the local thread interrupted, and the DB lock released.
- `GET /api/assets/catalog` SSE endpoint remains functional for manual use.
- Remove catalog-triggering code from `GalleryComponent`.
- Spring-native components only — no external dependencies.

**Non-Goals:**
- UI button to trigger catalog manually.
- Changing catalog algorithm, folder-scanning logic, or SSE event format.
- Sub-batch heartbeat granularity (per-asset instead of per-`catalog-batch-size`).

## Decisions

### D1 — Scheduling: `ThreadPoolTaskScheduler` + `ApplicationReadyEvent`

`AppConfig` exposes a `@Bean ThreadPoolTaskScheduler catalogTaskScheduler()` (pool size 1). `CatalogScheduler` listens for `ApplicationReadyEvent` and calls:

```java
catalogTaskScheduler.scheduleWithFixedDelay(
    this::executeCatalogRun,
    Instant.now(),
    Duration.ofMinutes(cooldownMinutes)
);
```

`Instant.now()` satisfies requirement 5 (start immediately). `scheduleWithFixedDelay` measures delay from the **end** of the previous execution. Since `executeCatalogRun` calls `catalogAssetsService.runCatalog()` — a **synchronous** method — the scheduler thread blocks until the catalog finishes before the cooldown timer starts.

**Why `ThreadPoolTaskScheduler` over `@Scheduled(fixedDelayString)`:** `@Scheduled(fixedDelayString)` requires a property placeholder that resolves to milliseconds or an ISO-8601 duration string. `catalog-cooldown-minutes` is a plain integer. Converting it programmatically with `Duration.ofMinutes()` is cleaner and derives from the existing property without a second config key.

`@EnableScheduling` is added to `AppConfig` to support the `@Scheduled` stale-check method in `CatalogScheduler`.

### D2 — New synchronous `runCatalog()` on `CatalogAssetsService`

A new `void runCatalog()` method is added to the `CatalogAssetsService` domain interface and implemented synchronously (no `@Async`) in `CatalogAssetsServiceImpl`. The scheduler thread blocks inside it so `scheduleWithFixedDelay` measures from actual completion. Both `runCatalog()` and the existing `@Async catalogAssetsAsync()` delegate to a shared private `doRunCatalog(Consumer<CatalogChangeNotification>)`.

### D3 — Distributed lock: `catalog_run_state` DB table (single row)

Schema (Flyway `V2__add_catalog_run_state.sql`):

```sql
CREATE TABLE catalog_run_state (
    id                INTEGER      PRIMARY KEY DEFAULT 1,
    is_running        BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at        TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    instance_id       VARCHAR(255),
    CONSTRAINT single_row CHECK (id = 1)
);
INSERT INTO catalog_run_state (id, is_running) VALUES (1, false);
```

**Try-acquire** (returns rows-updated count — 1 = acquired, 0 = already locked):
```sql
UPDATE catalog_run_state
   SET is_running = true, started_at = :now, last_heartbeat_at = :now, instance_id = :instanceId
 WHERE id = 1 AND is_running = false
```

**Heartbeat refresh** (called every `catalog-batch-size` assets):
```sql
UPDATE catalog_run_state SET last_heartbeat_at = :now
 WHERE id = 1 AND instance_id = :instanceId AND is_running = true
```

**Release** (in `finally`):
```sql
UPDATE catalog_run_state
   SET is_running = false, started_at = null, last_heartbeat_at = null, instance_id = null
 WHERE id = 1 AND instance_id = :instanceId
```

PostgreSQL row-level locking ensures only one concurrent `UPDATE WHERE is_running=false` succeeds when multiple instances race.

### D4 — Heartbeat at asset-batch granularity inside `CatalogFolderServiceImpl`

`CatalogFolderService.catalogFolder()` signature gains a `Runnable heartbeatCallback` parameter:

```java
void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                   Runnable heartbeatCallback, AtomicInteger processed, int total);
```

Inside `CatalogFolderServiceImpl`, a local `assetsProcessed` counter increments after each `assetRepository.save()`. When `assetsProcessed % batchSize == 0`, `heartbeatCallback.run()` is called. `batchSize` is read from `@Value("${photomanager.catalog-batch-size:1000}")`.

**Transaction isolation for heartbeat:** `catalogFolder()` is `@Transactional`. The heartbeat write must be visible to other JVMs immediately, not only when the folder transaction commits. `CatalogRunStateRepository.refreshHeartbeat()` is annotated `@Transactional(propagation = REQUIRES_NEW)`: when called through the Spring proxy it suspends the outer folder transaction, commits the heartbeat update in its own transaction, and resumes the folder transaction. This makes the heartbeat visible across JVMs after every `catalog-batch-size` assets regardless of folder size.

**For the SSE endpoint path** (`catalogAssetsAsync`), an identical real heartbeat callback is used — the SSE caller benefits from the same lock and heartbeat protection.

### D5 — Stale detection and cleanup: `@Scheduled` in `CatalogScheduler`

A `@Scheduled(fixedDelay = 60_000)` method `cleanupStaleCatalogs()` runs every minute:

1. Compute `threshold = Instant.now() - Duration.ofMinutes(catalogTimeoutMinutes)`.
2. Query: `WHERE is_running=true AND instance_id = :instanceId AND last_heartbeat_at < :threshold`. If matched, this JVM holds a stale lock → interrupt the stored `volatile Thread catalogRunThread` and release the lock.
3. Release any remaining stale locks from other (crashed) instances: `WHERE is_running=true AND instance_id != :instanceId AND last_heartbeat_at < :threshold`.

**Thread interruption:** `CatalogScheduler.executeCatalogRun()` stores `Thread.currentThread()` into `volatile Thread catalogRunThread` on entry and clears it on exit. `cleanupStaleCatalogs()` calls `catalogRunThread.interrupt()`.

**Interrupt handling in `doRunCatalog()`:** The folder loop checks `Thread.currentThread().isInterrupted()` at the top of each iteration and returns early if set. Because `catalogFolder()` is `@Transactional`, an interrupted thread that propagates an exception through `createAsset()` (e.g., from blocked I/O) will cause that folder's transaction to roll back cleanly.

**Race between stale release and new acquire:** After `cleanupStaleCatalogs()` releases a stale lock, another instance may acquire it before the interrupted thread's `finally` block executes. The `release()` query is scoped to `WHERE instance_id = :instanceId`, so it safely no-ops if the new holder has already taken over.

### D6 — Instance identity: UUID bean in `AppConfig`

```java
@Bean
public String catalogInstanceId() {
    return UUID.randomUUID().toString();
}
```

Both `CatalogAssetsServiceImpl` and `CatalogScheduler` inject this bean. A UUID generated once per JVM startup provides stable, collision-resistant identity with no hostname or port configuration dependency.

### D7 — Frontend: full removal of catalog-triggering code

Delete from `GalleryComponent`: `startCatalog()`, `cataloging`, `catalogProgress`, `catalogEventSource`, the `ngOnDestroy` body, `MatProgressBarModule` import. Remove `<mat-progress-bar>` from the template. Remove `OnDestroy` from `implements` if no remaining usage. Keep `AssetService.catalogAssets()` — it backs the SSE endpoint.

## Risks / Trade-offs

- **Fixed delay vs. fixed rate:** If a catalog run takes longer than `cooldown-minutes`, the next run is pushed further out. Intentional — avoids overlap — but effective refresh rate degrades under heavy load.
- **Per-batch heartbeat inside a folder transaction:** The heartbeat commits via `REQUIRES_NEW` so it is visible immediately, but the assets being saved in the same folder are not yet visible until the folder transaction commits. This is by design and causes no correctness issue.
- **Interrupt at folder boundaries, not asset boundaries:** A thread interrupted while deep inside `createAsset()` (thumbnail generation, hash computation) will not stop until that asset completes or throws. Maximum added latency = time to process one asset. Acceptable.
- **API endpoint returns empty stream when locked:** Callers receive an empty SSE stream that closes immediately. Existing consumers must handle this. No change from current behavior once the guard is in place.
