## 1. Config and Database

- [x] 1.1 Add `photomanager.catalog-timeout: 60` to `JPPhotoManagerWeb/backend/src/main/resources/application.yml`
- [x] 1.2 Create `V2__add_catalog_run_state.sql` in `src/main/resources/db/migration/` with the single-row `catalog_run_state` table (`id`, `is_running`, `started_at`, `last_heartbeat_at`, `instance_id`) and seed `INSERT INTO catalog_run_state (id, is_running) VALUES (1, false)`

## 2. Domain — Entity and Repository

- [x] 2.1 Create `CatalogRunState` JPA entity in `domain/entity/` mapping `catalog_run_state` (fields: `id`, `running`, `startedAt`, `lastHeartbeatAt`, `instanceId`)
- [x] 2.2 Create `CatalogRunStateRepository` in `domain/repository/` extending `JpaRepository<CatalogRunState, Integer>` with the following `@Modifying @Query` methods:
  - `int tryAcquire(String instanceId, Instant now)` — `UPDATE … SET is_running=true … WHERE id=1 AND is_running=false`
  - `void release(String instanceId)` — `UPDATE … SET is_running=false … WHERE id=1 AND instance_id=:instanceId`
  - `@Transactional(propagation=REQUIRES_NEW) void refreshHeartbeat(String instanceId, Instant now)` — `UPDATE … SET last_heartbeat_at=:now WHERE id=1 AND instance_id=:instanceId AND is_running=true`
  - `boolean isStaleForInstance(String instanceId, Instant threshold)` — `SELECT COUNT(s)>0 … WHERE id=1 AND is_running=true AND instance_id=:instanceId AND last_heartbeat_at < :threshold`
  - `int releaseStaleForOtherInstances(String instanceId, Instant threshold)` — `UPDATE … SET is_running=false … WHERE id=1 AND is_running=true AND instance_id != :instanceId AND last_heartbeat_at < :threshold`

## 3. AppConfig Updates

- [x] 3.1 Add `@EnableScheduling` to `AppConfig`
- [x] 3.2 Add `@Bean ThreadPoolTaskScheduler catalogTaskScheduler()` with pool size 1 and thread name prefix `catalog-scheduler-`
- [x] 3.3 Add `@Bean String catalogInstanceId()` returning `UUID.randomUUID().toString()`

## 4. CatalogFolderService — Heartbeat Callback

- [x] 4.1 Add `Runnable heartbeatCallback` parameter to `CatalogFolderService.catalogFolder()` interface method (new signature: `catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback, Runnable heartbeatCallback, AtomicInteger processed, int total)`)
- [x] 4.2 Update `CatalogFolderServiceImpl`:
  - Add `@Value("${photomanager.catalog-batch-size:1000}") int batchSize` field
  - Add a local `assetsProcessed` counter inside the asset loop
  - After each `assetRepository.save(asset)`, increment the counter; when `assetsProcessed % batchSize == 0`, call `heartbeatCallback.run()`
- [x] 4.3 Update `CatalogFolderServiceImplTest` — add `heartbeatCallback` argument (no-op `Runnable`) to all existing `catalogFolder(...)` call sites; add new test `catalogFolder_afterBatchSizeAssets_callsHeartbeat`

## 5. CatalogAssetsService — Lock, runCatalog(), and Interrupt-Safe Loop

- [x] 5.1 Add `void runCatalog()` to the `CatalogAssetsService` interface
- [x] 5.2 Refactor `CatalogAssetsServiceImpl`:
  - Inject `CatalogRunStateRepository catalogRunStateRepository` and `@Qualifier("catalogInstanceId") String instanceId`
  - Extract `doRunCatalog(Consumer<CatalogChangeNotification> callback)` from the existing `catalogAssetsAsync()` body:
    - At the top of the folder loop, check `Thread.currentThread().isInterrupted()` — return early if set
    - Pass `() -> catalogRunStateRepository.refreshHeartbeat(instanceId, Instant.now())` as `heartbeatCallback` to every `catalogFolderService.catalogFolder()` call
  - Implement `runCatalog()`: `tryAcquire` → `doRunCatalog(n -> {})` → `release` in `finally`
  - Rewrite `catalogAssetsAsync()`: `tryAcquire` → `doRunCatalog(callback)` → `release` in `finally`; if acquire fails return `CompletableFuture.completedFuture(null)`
- [x] 5.3 Update `CatalogAssetsServiceImplTest`:
  - Add mocks for `CatalogRunStateRepository` and `instanceId`
  - Update all existing `catalogFolder(...)` verify calls to include the new `heartbeatCallback` argument (use `any(Runnable.class)`)
  - Add tests: `runCatalog_lockAlreadyHeld_skipsRun`, `runCatalog_releasesLockAfterSuccess`, `runCatalog_releasesLockAfterException`, `catalogAssetsAsync_lockAlreadyHeld_returnsEmptyFuture`, `doRunCatalog_threadInterrupted_stopsAtNextFolder`

## 6. CatalogScheduler

- [x] 6.1 Create `CatalogScheduler` in `infrastructure/service/` (or `config/`) annotated `@Component @Slf4j @RequiredArgsConstructor`:
  - Inject: `CatalogAssetsService`, `CatalogRunStateRepository`, `@Qualifier("catalogTaskScheduler") ThreadPoolTaskScheduler`, `@Qualifier("catalogInstanceId") String instanceId`
  - `@Value` fields: `catalogCooldownMinutes` (`${photomanager.catalog-cooldown-minutes:2}`), `catalogTimeoutMinutes` (`${photomanager.catalog-timeout:60}`)
  - `volatile Thread catalogRunThread` field
  - `@EventListener(ApplicationReadyEvent.class) void onApplicationReady()`: calls `catalogTaskScheduler.scheduleWithFixedDelay(this::executeCatalogRun, Instant.now(), Duration.ofMinutes(catalogCooldownMinutes))`
  - `private void executeCatalogRun()`: sets `catalogRunThread = Thread.currentThread()`, calls `catalogAssetsService.runCatalog()`, clears `catalogRunThread` in `finally`
  - `@Scheduled(fixedDelay = 60_000) void cleanupStaleCatalogs()`: computes threshold; if `isStaleForInstance` → interrupt local thread + `release(instanceId)`; then `releaseStaleForOtherInstances(instanceId, threshold)` and log count
- [x] 6.2 Create `CatalogSchedulerTest` in `infrastructure/service/` (or matching test package):
  - Test `onApplicationReady_schedulesImmediateRun`: verify `scheduleWithFixedDelay` is called with `Instant.now()` start and `Duration.ofMinutes(cooldown)` delay
  - Test `cleanupStaleCatalogs_ownStaleLock_interruptsThreadAndReleasesLock`
  - Test `cleanupStaleCatalogs_remoteStaleLock_releasesWithoutInterrupt`
  - Test `cleanupStaleCatalogs_noStaleLock_doesNothing`

## 7. Frontend — Remove Catalog Trigger from GalleryComponent

- [x] 7.1 Remove from `gallery.component.ts`:
  - `startCatalog()` method
  - `cataloging`, `catalogProgress`, `catalogEventSource` fields
  - `ngOnDestroy()` hook (verify no other usage first)
  - `MatProgressBarModule` from `imports` array
  - `OnDestroy` from `implements` clause
  - The `this.startCatalog()` call in `ngOnInit()`
- [x] 7.2 Remove `<mat-progress-bar>` and its surrounding `@if (cataloging)` block from `gallery.component.html`
- [x] 7.3 Update `gallery.component.cy.ts`: add test `ngOnInit_doesNotCallCatalogAssets` verifying `assetService.catalogAssets` is never called after mount; remove any existing test that expects the catalog to be triggered on init

## 8. Documentation

- [x] 8.1 Add all the changes on the catalog process in this plan to @JPPhotoManagerWeb/README.md file, in a dedicated Catalog Process section.
