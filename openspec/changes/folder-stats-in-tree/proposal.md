## Why

The folder tree in the sidebar shows folder names but no indication of how many assets each folder contains or its total disk usage. Users cannot gauge the relative size of folders without navigating into each one. Displaying asset count and total file size inline in the tree provides immediate context and helps with cleanup decisions.

## What Changes

- A new `GET /api/folders/stats?path=...` endpoint returns `{ assetCount, totalSizeBytes }` for a given folder path
- The `FolderNavComponent` fetches stats for each visible folder node and renders them alongside the folder name

## Capabilities

### New Capabilities

- `folder-stats-in-tree`: Each folder node in the sidebar tree displays its asset count and total file size, loaded on demand as the tree is expanded.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/folder/GetFolderStatsUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/folder/GetFolderStatsUseCaseImpl.java` — use case implementation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/FolderController.java` — new GET endpoint
- `JPPhotoManagerWeb/backend/src/test/` — tests for the use case and endpoint
- `JPPhotoManagerWeb/frontend/src/app/features/folder-nav/folder-nav.component.ts` — fetch and display per-folder stats
