## Why

The catalog process is currently auto-triggered by every browser tab that opens the Gallery page. In a multi-user, horizontally-scalable deployment this causes redundant concurrent runs across browser sessions and backend instances, and leaves no protection against long-running or hung processes consuming resources indefinitely. The backend should own the catalog lifecycle, with a shared database table coordinating execution state across all JVM instances.

## What Changes

- **BREAKING (frontend):** `GalleryComponent` no longer auto-starts the catalog on init. The progress bar, `cataloging`/`catalogProgress` state, `catalogEventSource`, and `startCatalog()` are removed.
- The `GET /api/assets/catalog` SSE endpoint is **retained** for manual/troubleshooting use.
- A new `CatalogScheduler` Spring component starts the first catalog run as soon as the application is fully initialized (`ApplicationReadyEvent`), then repeats with a fixed delay equal to `photomanager.catalog-cooldown-minutes` using Spring `ThreadPoolTaskScheduler`.
- A new `catalog_run_state` database table (single row) acts as the distributed lock. Before each run the backend performs an atomic `UPDATE … WHERE is_running = false` — if 0 rows are updated, a run is already in progress and the attempt is skipped. The row stores `is_running`, `started_at`, `last_heartbeat_at`, and `instance_id`.
- The `last_heartbeat_at` column is refreshed after every `catalog-batch-size` assets are persisted (inside the asset loop in `CatalogFolderServiceImpl`), not at folder boundaries, so catalog runs over very large collections stay alive past the timeout regardless of individual folder size.
- A new `photomanager.catalog-timeout` setting (default 60 minutes) defines the maximum time since the last heartbeat before a catalog run is considered stale. A `@Scheduled` stale-check task detects stale runs, interrupts the local thread if this JVM holds the lock, and releases the DB lock so other instances can proceed.
- No external dependencies are added. All scheduling and locking uses Spring and Spring Data JPA.

## Capabilities

### New Capabilities

- `scheduled-catalog`: Backend-owned catalog lifecycle — starts on application ready, repeats with fixed delay, enforces a distributed DB lock, refreshes heartbeat every `catalog-batch-size` assets, and auto-cleans stale runs.

### Modified Capabilities

- `gallery-auto-catalog`: The gallery no longer initiates the catalog process.

## Impact

- **Backend — new files:** `CatalogScheduler`; `CatalogRunState` JPA entity; `CatalogRunStateRepository`; Flyway migration `V2__add_catalog_run_state.sql`.
- **Backend — modified files:** `CatalogAssetsService` + `CatalogAssetsServiceImpl` (lock, `runCatalog()`, interrupt-safe loop); `CatalogFolderService` + `CatalogFolderServiceImpl` (heartbeat callback parameter, per-batch heartbeat call); `AppConfig` (`@EnableScheduling`, `ThreadPoolTaskScheduler` bean, `catalogInstanceId` UUID bean).
- **Config:** `photomanager.catalog-cooldown-minutes` now actively consumed; `photomanager.catalog-timeout: 60` added.
- **Frontend:** `GalleryComponent` simplified; `MatProgressBarModule` and progress bar removed.
- **Tests:** New JUnit tests for `CatalogScheduler`; updated tests for `CatalogAssetsServiceImpl` and `CatalogFolderServiceImpl`; updated Cypress test confirming gallery no longer calls `catalogAssets` on init.
