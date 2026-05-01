## Why

The `catalog_run_state` table tracks whether a catalog run is currently active but has no record of when the last successful run finished. Without this, operators and monitoring tools cannot tell whether the catalog has ever completed or how stale the current asset index is.

## What Changes

- Add a nullable `last_completed_at TIMESTAMPTZ` column to `catalog_run_state` via a new Flyway migration (`V3`).
- Add a `markCompleted(String instanceId, Instant now)` method to `CatalogRunStateRepository` that updates the column on the row held by the given instance.
- Call `markCompleted` in `CatalogAssetsServiceImpl` after `doRunCatalog()` returns successfully in both `runCatalog()` and `catalogAssetsAsync()` — before the `finally` release, so only successful runs update the timestamp.

## Capabilities

### New Capabilities

- `catalog-completion-timestamp`: Records when a catalog run last completed successfully, enabling operators and future features to determine catalog freshness.

### Modified Capabilities

## Impact

- **Backend — modified files:** `CatalogRunStateRepository` (new query method); `CatalogAssetsServiceImpl` (call to `markCompleted` on success); Flyway migration `V3__add_catalog_last_completed.sql`.
- **Entity:** `CatalogRunState` gains the `lastCompletedAt` field.
- **Tests:** `CatalogAssetsServiceImplTest` gets two new tests verifying `markCompleted` is called on success and not called on failure.
