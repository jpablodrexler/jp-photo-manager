# folder-watch-service

A Java NIO `WatchService` monitors all configured root catalog folders for filesystem events (`ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`) and triggers incremental catalog updates automatically. Events are debounced before triggering to coalesce rapid batch operations.

---

## ADDED Requirements

### Requirement: FolderWatchService monitors configured root folders

When `photomanager.folder-watch.enabled: true`, a `FolderWatchService` SHALL start on application startup and register all configured root catalog folders (and their subdirectories) for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events using Java NIO `WatchService`.

#### Scenario: Service starts watching on application startup

- **GIVEN** `photomanager.folder-watch.enabled: true` and a root catalog folder `/photos`
- **WHEN** the Spring Boot application starts
- **THEN** `FolderWatchService` is active and `/photos` (and all subdirectories) are registered with the `WatchService`

#### Scenario: Service does not start when disabled

- **GIVEN** `photomanager.folder-watch.enabled: false`
- **WHEN** the Spring Boot application starts
- **THEN** no `WatchService` thread is started and no filesystem monitoring occurs

### Requirement: Filesystem events trigger a debounced catalog update

When a filesystem event is detected in a watched folder, the `FolderWatchService` SHALL debounce events with a configurable window (default 5 seconds) before calling `CatalogAssetsUseCase`.

#### Scenario: Single file added triggers catalog after debounce

- **GIVEN** `FolderWatchService` is active
- **WHEN** a new image file is added to a watched folder
- **THEN** after `debounce-seconds` seconds of inactivity, `CatalogAssetsUseCase.execute()` is called

#### Scenario: Rapid batch of files coalesces into a single catalog run

- **GIVEN** `FolderWatchService` is active and `debounce-seconds: 5`
- **WHEN** 100 files are copied into a watched folder over 3 seconds
- **THEN** only one `CatalogAssetsUseCase.execute()` call is made (after the batch completes and 5 seconds pass)

### Requirement: Watch-triggered catalog respects the distributed lock

The `FolderWatchService` SHALL call `CatalogAssetsUseCase.execute()` normally. If a catalog run is already in progress (lock held), the watch-triggered run SHALL be skipped gracefully without error.

#### Scenario: Watch-triggered run skipped when catalog already running

- **GIVEN** a catalog run is already in progress (lock held)
- **WHEN** a filesystem event fires and the debounce timer triggers
- **THEN** `CatalogAssetsUseCase` skips the run due to the lock and logs a debug message
