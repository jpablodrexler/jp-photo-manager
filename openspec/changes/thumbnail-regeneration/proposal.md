## Why

Thumbnails can become corrupted, or their size may change (e.g., updated `photomanager.thumbnail-width` / `thumbnail-height` configuration), or EXIF rotation correction may be applied retroactively. There is currently no way to regenerate thumbnails without re-cataloging the entire folder (which is slow and does much more work than needed). A dedicated `POST /api/assets/regenerate-thumbnails` endpoint deletes existing thumbnail files and regenerates them through the existing infrastructure adapters.

## What Changes

- New `RegenerateThumbnailsUseCase` port (in) and `RegenerateThumbnailsUseCaseImpl` implementation
- New `POST /api/assets/regenerate-thumbnails?folderPath=...` endpoint (optional `folderPath` scopes the operation)
- Streams progress via `SseEmitter` (same pattern as catalog/sync/convert)
- No Flyway migration; reuses existing `ThumbnailPort` and `StoragePort` infrastructure adapters

## Capabilities

### New Capabilities

- `thumbnail-regeneration`: Thumbnails can be regenerated for all assets or a specific folder via a dedicated endpoint, without a full re-catalog.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/asset/RegenerateThumbnailsUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/RegenerateThumbnailsUseCaseImpl.java` — new use case implementation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — new SSE endpoint
- `JPPhotoManagerWeb/backend/src/test/` — new unit tests
- `JPPhotoManagerWeb/frontend/src/app/core/services/asset.service.ts` — new `regenerateThumbnails()` method returning `EventSource`
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.ts` — new toolbar action for regeneration
