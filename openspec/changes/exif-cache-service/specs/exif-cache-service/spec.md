# exif-cache-service

EXIF metadata fetched by `ExifPanelComponent` is cached at session level in a singleton `ExifCacheService`. Navigating away and back to the viewer does not trigger redundant API calls for previously loaded assets.

---

## ADDED Requirements

### Requirement: ExifCacheService caches EXIF data at session level

A singleton `ExifCacheService` SHALL maintain a `Map<number, ExifMetadata | null>` that persists for the Angular application lifetime (the browser tab session). `ExifPanelComponent` SHALL use this service instead of its own local map.

#### Scenario: EXIF data is returned from cache on second access

- **GIVEN** EXIF data for asset 42 was fetched and cached during a previous viewer session
- **WHEN** the user navigates away and back to asset 42 in the viewer
- **THEN** `ExifCacheService.has(42)` returns `true` and no network request is made to `/api/assets/42/exif`

#### Scenario: Null is cached for assets with no EXIF

- **GIVEN** an asset has no EXIF data (the API returns null)
- **WHEN** the viewer opens for that asset a second time
- **THEN** `ExifCacheService.has(assetId)` returns `true` with value `null` and no network request is made

### Requirement: ExifPanelComponent delegates EXIF lookups to ExifCacheService

`ExifPanelComponent` SHALL NOT maintain its own EXIF cache map. All cache reads and writes SHALL go through `ExifCacheService`.

#### Scenario: ExifPanelComponent uses ExifCacheService on init

- **WHEN** `ExifPanelComponent` loads an asset
- **THEN** it calls `exifCacheService.has(assetId)` before calling the API, and calls `exifCacheService.set(assetId, data)` after a successful fetch
