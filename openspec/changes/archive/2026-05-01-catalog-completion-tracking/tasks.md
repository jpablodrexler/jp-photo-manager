## 1. Database Migration

- [x] 1.1 Create `V3__add_catalog_last_completed.sql` in `src/main/resources/db/migration/` adding `last_completed_at TIMESTAMPTZ` (nullable, no default) to `catalog_run_state`

## 2. Domain — Entity and Repository

- [x] 2.1 Add `lastCompletedAt` (`Instant`) field to `CatalogRunState` JPA entity mapped to `last_completed_at`
- [x] 2.2 Add `markCompleted(String instanceId, Instant now)` to `CatalogRunStateRepository`: `@Modifying @Transactional @Query("UPDATE CatalogRunState s SET s.lastCompletedAt = :now WHERE s.id = 1 AND s.instanceId = :instanceId")`

## 3. Service — Record Completion on Success

- [x] 3.1 In `CatalogAssetsServiceImpl.runCatalog()`, call `catalogRunStateRepository.markCompleted(instanceId, Instant.now())` immediately after `doRunCatalog(n -> {})` returns (inside the try block, before the catch/finally)
- [x] 3.2 In `CatalogAssetsServiceImpl.catalogAssetsAsync()`, call `catalogRunStateRepository.markCompleted(instanceId, Instant.now())` immediately after `doRunCatalog(callback)` returns (inside the try block, before the finally)

## 4. Tests

- [x] 4.1 In `CatalogAssetsServiceImplTest`, add `runCatalog_successfulRun_marksCompleted`: verify `markCompleted` is called when `doRunCatalog` succeeds (storageService returns no directories)
- [x] 4.2 In `CatalogAssetsServiceImplTest`, add `runCatalog_failedRun_doesNotMarkCompleted`: verify `markCompleted` is NOT called when `storageService.directoryExists` throws
- [x] 4.3 In `CatalogAssetsServiceImplTest`, add `catalogAssetsAsync_successfulRun_marksCompleted`: verify `markCompleted` is called when `catalogAssetsAsync` completes successfully
