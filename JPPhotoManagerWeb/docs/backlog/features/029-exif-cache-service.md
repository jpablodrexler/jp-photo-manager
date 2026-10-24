# Feature 29 — exif-cache-service

**As a** photo library owner, **I want** EXIF data cached for the whole session, **so that** moving back to the viewer does not refetch what I already saw.

## Acceptance Criteria
- AC1: Given an asset's EXIF data was fetched, when I access it again, then it is served from the cache.
- AC2: Given an asset with no EXIF data, when it is fetched once, then the null result is cached too.
- AC3: Given the EXIF panel, when it initialises, then it reads through the shared cache service.
- AC4: Given I navigate away and back, when the panel recreates, then the cache persists.

## Technical notes

Move the `Map<number, ExifMetadata | null>` from `ExifPanelComponent` (destroyed on every navigation) to a singleton `ExifCacheService`; the cache currently lives only for the lifetime of the component instance, so navigating away and back to the viewer discards all fetched EXIF data and triggers redundant API calls; a service-level cache persists for the entire session

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
