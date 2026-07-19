## 1. Configuration

- [ ] 1.1 Add `photomanager.folder-watch.enabled: false` and `photomanager.folder-watch.debounce-seconds: 5` to `application.yml`
- [ ] 1.2 Add corresponding `@ConfigurationProperties` fields or `@Value` injections to `FolderWatchService`

## 2. FolderWatchService implementation

- [ ] 2.1 Create `infrastructure/watch/FolderWatchService.java` annotated with `@Component` and implementing `InitializingBean`, `DisposableBean`
- [ ] 2.2 In `afterPropertiesSet()`, if `enabled = true`, start a daemon thread running the watch loop
- [ ] 2.3 In `destroy()`, close the `WatchService` and shut down the `ScheduledExecutorService`
- [ ] 2.4 Implement `registerAll(Path root)` that recursively registers a directory and all subdirectories with the `WatchService` for `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`
- [ ] 2.5 In the watch loop, when a `ENTRY_CREATE` event targets a directory, call `registerAll(newDir)` to watch new subdirectories
- [ ] 2.6 On any filesystem event, cancel any pending debounce future and reschedule `triggerCatalog()` after `debounce-seconds`
- [ ] 2.7 Implement `triggerCatalog()` that calls `catalogAssetsUseCase.execute()` and logs the result

## 3. Spring wiring

- [ ] 3.1 Inject `CatalogAssetsUseCase` into `FolderWatchService` via constructor injection
- [ ] 3.2 Inject configured root catalog folders (same configuration property used by the existing catalog service)

## 4. Unit tests

- [ ] 4.1 Test that `FolderWatchService` does not start watching when `enabled = false`
- [ ] 4.2 Test that a simulated filesystem event calls `CatalogAssetsUseCase.execute()` after the debounce period (use a mock WatchService or temp directory)
- [ ] 4.3 Test that multiple rapid events result in only one `execute()` call

## 5. Testing and Commit

- [ ] 5.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 5.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 5.3 Commit all changes (only after both test suites pass)
