## Why

Users with large folder hierarchies must scroll or expand the tree every time they navigate to a frequently used folder. Bookmarking (pinning) folders provides instant access to favourites without tree traversal.

## What Changes

- A new `folder_bookmarks` table stores per-user bookmarks (`id`, `userId`, `folderPath`, `createdAt`)
- `GET /api/folders/bookmarks` returns the authenticated user's bookmarked folders
- `POST /api/folders/bookmarks` adds a bookmark; `DELETE /api/folders/bookmarks/{id}` removes one
- The `FolderNavComponent` shows a pinned section above the main tree using a `MatDivider` separator
- A pin icon on each tree node toggles the bookmark state

## Capabilities

### New Capabilities

- `folder-bookmarks`: Users can bookmark frequently used folders. Bookmarked folders appear in a pinned section above the main folder tree for instant access.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V20__create_folder_bookmarks.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/folder/` — use case interfaces for list, add, remove bookmark
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/FolderController.java` — new bookmark endpoints
- `JPPhotoManagerWeb/backend/src/test/` — tests for bookmark operations
- `JPPhotoManagerWeb/frontend/src/app/features/folder-nav/folder-nav.component.ts` — pinned section and pin icon per node
