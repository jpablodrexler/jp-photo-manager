## ADDED Requirements

### Requirement: Catalog run records completion timestamp on success

The system SHALL update `last_completed_at` in `catalog_run_state` to the current time when a catalog run completes without error. The update SHALL only be applied when `doRunCatalog` returns normally — it SHALL NOT be applied when the run fails or is interrupted.

#### Scenario: Successful scheduled run updates last_completed_at
- **WHEN** `CatalogAssetsService.runCatalog()` is called
- **AND** the distributed lock is acquired
- **AND** `doRunCatalog` completes without throwing
- **THEN** `CatalogRunStateRepository.markCompleted(instanceId, now)` is called before the lock is released

#### Scenario: Failed scheduled run does not update last_completed_at
- **WHEN** `CatalogAssetsService.runCatalog()` is called
- **AND** `doRunCatalog` throws an exception
- **THEN** `markCompleted` is NOT called
- **AND** the lock is still released in the finally block

#### Scenario: Successful API-triggered run updates last_completed_at
- **WHEN** `CatalogAssetsService.catalogAssetsAsync()` is called
- **AND** the distributed lock is acquired
- **AND** `doRunCatalog` completes without throwing
- **THEN** `CatalogRunStateRepository.markCompleted(instanceId, now)` is called before the lock is released

#### Scenario: Null last_completed_at before any run
- **WHEN** the application starts for the first time
- **AND** no catalog run has ever completed
- **THEN** `catalog_run_state.last_completed_at` is NULL

### Requirement: markCompleted is scoped to the owning instance

The `markCompleted` update SHALL use a `WHERE id=1 AND instance_id=:instanceId` guard so that a stale completing thread cannot overwrite the timestamp of a run started by a different JVM instance that took over the lock.

#### Scenario: markCompleted uses instance-scoped WHERE clause
- **WHEN** `CatalogRunStateRepository.markCompleted(instanceId, now)` is called
- **THEN** the UPDATE query targets `WHERE id=1 AND instance_id=:instanceId`
- **AND** if the instance_id no longer matches, 0 rows are updated and no error is thrown
