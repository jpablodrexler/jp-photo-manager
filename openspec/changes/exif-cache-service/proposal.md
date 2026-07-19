## Why

The `ExifPanelComponent` maintains a `Map<number, ExifMetadata | null>` to avoid refetching EXIF data while the component is alive. But the component is destroyed on every route navigation away from the viewer, so the cache is discarded and all previously fetched EXIF entries must be re-fetched the next time the user opens the viewer. Moving the cache to a singleton service makes it survive navigation and eliminates redundant API calls across the session.

## What Changes

- New singleton `ExifCacheService` in `core/services/` that holds a `Map<number, ExifMetadata | null>` for the entire session lifetime
- `ExifPanelComponent` delegates EXIF lookups to `ExifCacheService` instead of its own local map
- The local `exifCache` map in `ExifPanelComponent` is removed

## Capabilities

### New Capabilities

- `exif-cache-service`: EXIF metadata lookups are cached at session level in a singleton service, eliminating repeat API calls when the user navigates away and back to the viewer.

### Modified Capabilities

- `exif-metadata-panel`: `ExifPanelComponent` now reads from `ExifCacheService` instead of its own local map.

## Impact

- `JPPhotoManagerWeb/frontend/src/app/core/services/exif-cache.service.ts` — new service
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/exif-panel/exif-panel.component.ts` — remove local `exifCache` map; inject and use `ExifCacheService`
- `JPPhotoManagerWeb/frontend/src/app/core/services/exif-cache.service.cy.ts` — new Cypress component tests
