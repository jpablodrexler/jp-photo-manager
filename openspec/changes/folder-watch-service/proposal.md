## Why

Currently, the catalog must be manually triggered by the user after adding, modifying, or deleting files in the watched folders. A Java NIO `WatchService` that monitors configured root catalog folders for filesystem events (`ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`) and triggers incremental catalog updates automatically keeps the catalog in sync without any manual action.

## What Changes

- New `FolderWatchService` in `infrastructure/watch/` that uses Java NIO `WatchService` to monitor all root catalog folders recursively
- Debounces rapid filesystem events (batch window: configurable, default 5 seconds) before triggering `CatalogAssetsUseCase`
- Reuses the existing `CatalogAssetsUseCase` and the `catalog_run_state` distributed lock as the execution path
- Configurable via `photomanager.folder-watch.enabled` and `photomanager.folder-watch.debounce-seconds` in `application.yml`

## Capabilities

### New Capabilities

- `folder-watch-service`: The application automatically detects filesystem changes in configured root catalog folders and triggers incremental catalog updates, keeping the catalog in sync without manual user action.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/watch/FolderWatchService.java` — new service
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/watch/FolderWatchService.java` — `@Component` started by Spring lifecycle; watches root folders on startup
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — new `photomanager.folder-watch.enabled` and `photomanager.folder-watch.debounce-seconds` properties
- `JPPhotoManagerWeb/backend/src/test/java/com/jpablodrexler/photomanager/infrastructure/watch/FolderWatchServiceTest.java` — new unit tests
