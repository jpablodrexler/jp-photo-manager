## Context

`ExifPanelComponent` has a `private exifCache = new Map<number, ExifMetadata | null>()` and a `loadExif(assetId)` method that checks the map before calling `AssetService.getExif(assetId)`. The component is created when the viewer opens and destroyed on route change. Navigating to a folder, then back to the viewer, destroys and recreates the component — the cache is lost and all EXIF entries must be refetched. A singleton service (`providedIn: 'root'`) persists for the Angular application lifetime (the browser tab session), so navigating away and back never loses cached EXIF data.

## Goals / Non-Goals

**Goals:**
- Extract the EXIF cache map to a singleton service
- Preserve the existing cache-or-fetch logic (same behavior, different lifetime)

**Non-Goals:**
- Changing the EXIF API endpoint or backend
- Implementing cache expiration within the session (session-lifetime caching is sufficient)
- Persisting the cache across page reloads (localStorage/IndexedDB would be needed; not worth it for EXIF)

## Decisions

### 1. Simple `Map<number, ExifMetadata | null>` in a singleton service

**Decision:** Wrap the existing `Map<number, ExifMetadata | null>` in an `@Injectable({ providedIn: 'root' })` service with `get(assetId)`, `set(assetId, data)`, and `has(assetId)` methods.

**Rationale:** The existing logic is correct; only the lifetime needs to change. The simplest refactoring is to move the map to a service without changing the lookup logic.

**Alternative considered:** Using `@Cacheable` via `server-side-spring-cache` (#28). That caches on the backend; the frontend still makes a network call on component creation. The frontend service-level cache eliminates the network call entirely.

### 2. `ExifPanelComponent` retains the `loadExif()` method signature

**Decision:** Keep the `loadExif(assetId: number)` method in `ExifPanelComponent` but delegate to `ExifCacheService` instead of the local map.

**Rationale:** Minimal change surface. The template and `ngOnChanges` hook remain unchanged.

### 3. Cache null values to avoid redundant 404 calls

**Decision:** `ExifCacheService` caches `null` for assets with no EXIF data, same as the existing local cache.

**Rationale:** An asset with no EXIF data will never have EXIF data unless re-cataloged. Caching `null` prevents re-fetching on every panel open.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Stale cache if EXIF is updated (e.g., re-catalog) | Low | Users can refresh the page; EXIF rarely changes after initial cataloging |
| Cache grows unbounded for large catalogs | Low | EXIF metadata objects are small (< 1 KB each); 10,000 entries ≈ 10 MB — acceptable for a browser tab session |
