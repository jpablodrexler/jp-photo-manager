## Context

The `exif-metadata-panel` improvement (already implemented) persists `gpsLatitude` and `gpsLongitude` as `DOUBLE PRECISION` decimal-degree columns in `asset_exif` for every catalogued JPEG. The existing `GetAssetExifUseCaseImpl` reads these values and exposes them via `GET /api/assets/{id}/exif`. The frontend `ExifPanelComponent` currently renders them as plain decimal numbers.

The backend's hexagonal architecture provides the correct extension point: a new driven port `GeocodingPort` in `domain/port/out/` decouples the reverse-geocoding concern from the use case, and a `NominatimGeocodingAdapter` in `infrastructure/service/` provides the concrete HTTP implementation — exactly the same pattern used for `StoragePort`/`StorageServiceAdapter` and `HashCalculatorPort`/`AssetHashCalculatorAdapter`.

The `GetAssetsUseCase` pipeline and `AssetRepository` already support filter-based queries. A new `GetAssetsMapUseCase` can reuse the same `AssetFilter` infrastructure and add a `hasGps: true` predicate to scope results to GPS-tagged assets only.

The frontend does not have Leaflet installed. Adding it as an npm package and wrapping it in a standalone Angular component fits the project's feature-based architecture without requiring any shared module or global registration.

## Goals / Non-Goals

**Goals:**
- Introduce `GeocodingPort` as a driven port in `domain/port/out/` with a Nominatim adapter in `infrastructure/service/`; the interface is the hook that `circuit-breaker` (#40) will wrap later.
- Enrich the `AssetExif` domain model and `ExifMetadataDto` with a nullable `locationName` field populated by `GetAssetExifUseCaseImpl` via `GeocodingPort`.
- Display the `locationName` string as a "Location" row in `ExifPanelComponent`.
- Render a small embedded Leaflet mini-map (200×150 px, OpenStreetMap tiles) in `ExifPanelComponent` when GPS coordinates are non-null.
- Provide a `GET /api/assets/map` endpoint returning paginated GPS-tagged asset pins for a folder or album.
- Provide a `/map` route with a full-page Leaflet map showing clustered pins for the current folder or album; clicking a pin navigates to the asset in the gallery viewer.

**Non-Goals:**
- Caching geocoding results in the database — `locationName` is computed on each `GetAssetExifUseCase` call; a dedicated cache belongs in `exif-cache-service` (#29) or `server-side-spring-cache` (#28).
- Adding a Resilience4j circuit breaker to `NominatimGeocodingAdapter` — that is the explicit responsibility of `circuit-breaker` (#40).
- Drawing routes or tracks from GPS sequences.
- Offline map tile hosting.
- Filtering the gallery by location (bounding-box or radius search).
- Storing `locationName` in the database.
- Supporting GPS altitude in the map view (altitude is already stored in `asset_exif` but is not used here).

## Decisions

### 1. `GeocodingPort` in `domain/port/out/` — not a utility class

**Decision:** Reverse geocoding is expressed as a driven port interface `domain/port/out/GeocodingPort.java`:

```java
public interface GeocodingPort {
    Optional<String> reverseGeocode(double lat, double lon);
}
```

`GetAssetExifUseCaseImpl` injects it and calls it when GPS fields are non-null.

**Rationale:** Keeping the interface in the domain layer and the HTTP implementation in `infrastructure/service/` is consistent with every other external dependency in the project (`StoragePort`, `ThumbnailPort`, `HashCalculatorPort`, `JwtTokenPort`). It also makes the `circuit-breaker` (#40) integration trivial: a Resilience4j decorator or `@CircuitBreaker` annotation wraps the `NominatimGeocodingAdapter` class without touching the use case.

**Alternative considered:** Calling the Nominatim API directly inside `GetAssetExifUseCaseImpl` using an injected `RestClient`. Rejected because it embeds infrastructure (HTTP client) in the application layer, violating hexagonal boundaries and making the adapter non-swappable.

### 2. `locationName` computed on query — not stored in the database

**Decision:** `locationName` is populated by `GetAssetExifUseCaseImpl` at read time by calling `GeocodingPort.reverseGeocode(lat, lon)`. It is not persisted in `asset_exif`.

**Rationale:** Storing it would require either (a) a Flyway migration adding a `location_name` column and re-cataloging all assets, or (b) a separate deferred enrichment pipeline. For a v1 that explicitly defers caching to `server-side-spring-cache` (#28) and `exif-cache-service` (#29), on-demand computation is the simplest correct approach. If Nominatim is unavailable, `GeocodingPort.reverseGeocode` returns `Optional.empty()` and `locationName` is `null` — the panel degrades gracefully.

**Alternative considered:** Populating `locationName` during cataloging and caching it in a new `location_name` column. Deferred: the `circuit-breaker` (#40) dependency makes this risky without the breaker in place; a slow Nominatim response would stall the entire catalog process.

### 3. Nominatim as the geocoding provider — no API key required

**Decision:** `NominatimGeocodingAdapter` calls `https://nominatim.openstreetmap.org/reverse?lat={lat}&lon={lon}&format=json` with a descriptive `User-Agent: JPPhotoManager/1.0` header (required by Nominatim's usage policy). The response field `display_name` is truncated to `"{city}, {country}"` using the `address.city` (or `address.town` / `address.village`) and `address.country_code` fields.

**Rationale:** Nominatim is the only production-grade reverse-geocoding service available at zero cost with no API key. Google Maps and HERE require registration. The `User-Agent` requirement is straightforward and keeps the adapter self-contained. The `circuit-breaker` (#40) improvement will later wrap this adapter with Resilience4j so that Nominatim downtime does not stall the EXIF panel.

**Alternative considered:** Using Google Maps Geocoding API. Rejected because it requires a paid API key and per-request billing, which is inappropriate for a self-hosted open-source application.

### 4. `GET /api/assets/map` — separate from `GET /api/assets`

**Decision:** A dedicated `GET /api/assets/map` endpoint powered by `GetAssetsMapUseCase` returns only the fields needed for map pins: `assetId`, `gpsLatitude`, `gpsLongitude`. It accepts `folderPath` (or `albumId`) and `page` query parameters. The `AssetFilter` has a new `boolean hasGps` field; when `true`, the `AssetRepository.findFiltered` query adds a `WHERE gpsLatitude IS NOT NULL AND gpsLongitude IS NOT NULL` predicate.

**Rationale:** Inlining GPS data into `GET /api/assets` would require JOINing `asset_exif` on every page load regardless of whether the map view is open, adding overhead to the primary gallery query path. The dedicated endpoint is called only when the user navigates to `/map` or opens the EXIF panel.

**Alternative considered:** Adding an `?includeGps=true` parameter to `GET /api/assets`. Rejected for the same reason as the EXIF endpoint decision in `exif-metadata-panel`: it complicates the primary endpoint and forces the JOIN on all callers that set the flag.

### 5. Leaflet.js loaded as an npm dependency — not a CDN script

**Decision:** Add `leaflet`, `@types/leaflet`, `leaflet.markercluster`, and `@types/leaflet.markercluster` to `package.json`. The `MapViewComponent` and `ExifPanelComponent` import Leaflet programmatically inside `ngAfterViewInit()` rather than in the constructor, ensuring the DOM container exists before Leaflet mounts.

**Rationale:** npm dependency is consistent with how Angular projects manage third-party libraries, produces a single reproducible bundle, and works offline without a CDN dependency. The `@types` packages provide TypeScript declarations without any runtime overhead. Leaflet CSS is added globally in `angular.json` under `styles`.

**Alternative considered:** Loading Leaflet from `unpkg.com` via a `<script>` tag in `index.html`. Rejected because it introduces a CDN dependency, bypasses the build system, and does not produce TypeScript types.

### 6. Mini-map in `ExifPanelComponent` — not a separate component

**Decision:** The embedded mini-map is rendered directly inside `ExifPanelComponent` using a `<div #miniMap>` template reference variable. A `private miniMapInstance: L.Map | null` class field holds the Leaflet instance, initialised in the effect that reacts to `assetId` input changes.

**Rationale:** The mini-map is tightly coupled to the EXIF panel's data flow (same `assetId`, same coordinates). Extracting it to a separate `MiniMapComponent` with `@Input lat @Input lon` would be over-engineering for a 200×150 tile; the parent component already manages the EXIF fetch lifecycle. The full-page `/map` route warrants its own `MapViewComponent` because it has independent state (folder/page), navigation logic, and pin clustering.

**Alternative considered:** A reusable `<app-mini-map [lat]="..." [lon]="...">` component shared between the EXIF panel and the map route. Deferred: the two maps have entirely different responsibilities (one is a static location indicator; the other is an interactive clustered explorer). A shared abstraction would need to cover both cases and would be harder to reason about.

### 7. Cluster provider: `leaflet.markercluster`

**Decision:** The `/map` route uses the `leaflet.markercluster` plugin to cluster pins when multiple assets share nearby GPS coordinates. Clustering is done entirely client-side after `GET /api/assets/map` returns a page of pins.

**Rationale:** `leaflet.markercluster` is the de-facto standard for Leaflet clustering, is MIT-licensed, and requires no backend support. Client-side clustering avoids a server-side spatial aggregation query and is fast enough for the page sizes this endpoint returns (up to ~100 pins per page). Server-side clustering (e.g. a PostGIS `ST_ClusterWithin` query) is a future optimisation if the dataset grows significantly.

**Alternative considered:** No clustering — rendering every pin individually. Rejected because folders with hundreds of GPS-tagged photos taken at the same location (e.g. a holiday) would produce an unreadable pile of overlapping markers.

### 8. Pin click navigation — `/gallery?folderPath=...&assetId=...`

**Decision:** Clicking a cluster-expanded pin navigates to `/gallery` with `folderPath` and `assetId` as query parameters. `GalleryComponent` already reads `folderPath` from query params to set the active folder; `assetId` is a new optional query parameter that `GalleryComponent` will react to by locating the matching asset and opening viewer mode.

**Rationale:** Re-using the gallery route avoids duplicating asset viewing logic. The `assetId` query parameter is the minimal contract needed to identify a specific asset; the gallery can then scroll to it and open the viewer.

**Alternative considered:** Navigating to a dedicated `GET /api/assets/{id}` detail page. Rejected because the project has no asset detail route and adding one for this feature alone is disproportionate.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Nominatim rate limit (1 req/s per IP) | `GeocodingPort.reverseGeocode` is called only on `GET /api/assets/{id}/exif` (not during catalog or list); rate is at most one call per user interaction. `circuit-breaker` (#40) will add a backoff. |
| Nominatim service unavailability | `NominatimGeocodingAdapter` catches all exceptions and returns `Optional.empty()`; `locationName` in the response is `null`; the EXIF panel renders coordinates without the city label. |
| Leaflet bundle size (~145 kB minified + gzip) | Leaflet is loaded only in `features/map/` via lazy routing; `ExifPanelComponent` is in `shared/` and its import of Leaflet is tree-shakeable. The cost is only incurred when the user opens the EXIF panel or navigates to `/map`. |
| `AssetFilter.hasGps = true` changes the JPQL in `AssetRepositoryImpl` | The existing filter predicate builder must handle `hasGps` carefully to avoid SQL injection and to short-circuit the JOIN when `hasGps = false` (the default). Existing queries must not regress. |
| EXIF panel Leaflet instance leaks on navigation | `ExifPanelComponent.ngOnDestroy()` must call `miniMapInstance.remove()` to release the DOM and event listeners. |
