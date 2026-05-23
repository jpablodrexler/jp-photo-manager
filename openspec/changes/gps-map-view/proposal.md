## Why

The gallery and EXIF panel already store GPS coordinates (`gpsLatitude`, `gpsLongitude`) for every catalogued JPEG in the `asset_exif` table. However, those coordinates are currently displayed only as raw decimal numbers inside the EXIF panel — there is no spatial visualisation and no way to see where a photo was taken on a map, or to browse a folder's worth of photos geographically.

A dedicated `/map` route and an embedded Leaflet.js mini-map inside the EXIF viewer close this gap. Users can visually orient a single photo relative to its shoot location, or switch to the map route to see all GPS-tagged assets in the current folder or album clustered on a world map, and navigate to any one of them directly. A `GeocodingPort` interface + Nominatim adapter also translates lat/lon to a human-readable city/country string shown in the EXIF panel, enriching what was previously an opaque pair of floats.

## What Changes

- **Backend — `GeocodingPort`:** introduce a new driven port interface `domain/port/out/GeocodingPort.java` with a single method `Optional<String> reverseGeocode(double lat, double lon)` returning a formatted `"City, Country"` string or empty when the service is unavailable.
- **Backend — `NominatimGeocodingAdapter`:** implement `GeocodingPort` in `infrastructure/service/` using Spring's `RestClient` to call the free Nominatim reverse-geocoding API (`https://nominatim.openstreetmap.org/reverse`). Includes a `User-Agent` header (required by Nominatim policy).
- **Backend — `GetAssetExifUseCase` / `GetAssetExifUseCaseImpl`:** extend the existing EXIF use case to also call `GeocodingPort.reverseGeocode` (only when `gpsLatitude`/`gpsLongitude` are non-null) and populate a new `locationName` field on `AssetExif`.
- **Backend — `AssetExif` domain model:** add nullable `String locationName` field.
- **Backend — `ExifMetadataDto`:** add `String locationName` field.
- **Backend — `GetAssetsMapUseCase`:** new use case in `domain/port/in/asset/` returning a paginated list of map pin DTOs (assetId, gpsLatitude, gpsLongitude, thumbnailUrl) for assets in a folder or album that have non-null GPS coordinates.
- **Backend — `GET /api/assets/map`:** new endpoint in `AssetController` accepting `folderPath` (or `albumId`) and `page` query parameters; returns `PaginatedResult<MapPin>`.
- **Frontend — `GeoPoint` and `MapPin` models:** add `core/models/map-pin.model.ts`.
- **Frontend — `AssetService.getMapPins()`:** new method calling `GET /api/assets/map`.
- **Frontend — `MapViewComponent`:** new standalone component under `features/map/` backed by Leaflet.js and `leaflet.markercluster`; renders clustered photo pins; clicking a pin navigates to `/gallery?folderPath=...&assetId=...` which auto-selects the asset in viewer mode.
- **Frontend — `/map` route:** register a new lazy-loaded route in `app.routes.ts`, protected by `authGuard`.
- **Frontend — nav bar:** add a "Map" navigation link in `AppComponent`.
- **Frontend — `ExifPanelComponent`:** when `locationName` is non-null, display it as a "Location" row in the panel; when both lat/lon are non-null, render a small embedded Leaflet map tile (200×150 px) showing the exact coordinates, replacing the plain decimal text.

## Capabilities

### New Capabilities

- `gps-map-view`: Display a Leaflet.js map panel in the EXIF viewer showing the asset's shoot location, and a full `/map` route rendering clustered photo pins for the current folder or album; clicking a pin navigates to the asset in the gallery viewer. Reverse-geocoding via Nominatim translates GPS coordinates to a human-readable city/country label shown in the EXIF panel.

### Modified Capabilities

- `exif-metadata-panel`: `ExifPanelComponent` gains a "Location" row (city/country string from `locationName`) and a small embedded Leaflet mini-map when GPS coordinates are present; the raw decimal coordinate fields are still shown below the mini-map.

## Impact

- **`domain/model/AssetExif.java`**: new nullable `String locationName` field.
- **`domain/port/out/GeocodingPort.java`** *(new)*: driven port interface for reverse geocoding.
- **`domain/port/in/asset/GetAssetsMapUseCase.java`** *(new)*: use-case interface returning map pins.
- **`domain/model/MapPin.java`** *(new)*: domain model with `assetId`, `gpsLatitude`, `gpsLongitude`, `thumbnailUrl`.
- **`application/usecase/asset/GetAssetExifUseCaseImpl.java`**: inject `GeocodingPort`; populate `locationName` when GPS fields are non-null.
- **`application/usecase/asset/GetAssetsMapUseCaseImpl.java`** *(new)*: query assets with non-null GPS; return paginated `MapPin` list.
- **`infrastructure/service/NominatimGeocodingAdapter.java`** *(new)*: `RestClient`-based adapter implementing `GeocodingPort`.
- **`infrastructure/web/dto/ExifMetadataDto.java`**: add `locationName` field.
- **`infrastructure/web/dto/MapPinDto.java`** *(new)*: HTTP DTO for map pin.
- **`infrastructure/web/controller/AssetController.java`**: new `GET /api/assets/map` endpoint.
- **`infrastructure/web/mapper/AssetDtoMapper.java`**: add `toMapPinDto` method.
- **`core/models/map-pin.model.ts`** *(new)*: TypeScript interface.
- **`core/models/exif-metadata.model.ts`**: add `locationName: string | null`.
- **`core/services/asset.service.ts`**: new `getMapPins(folderPath, page)` method.
- **`features/map/map-view.component.ts/html/scss`** *(new)*: standalone Leaflet map route component.
- **`shared/components/exif-panel/exif-panel.component.ts/html/scss`**: add location row and embedded mini-map.
- **`app.routes.ts`**: new `/map` lazy route.
- **`app.component.html`**: new "Map" nav link.
- **`package.json`**: add `leaflet`, `@types/leaflet`, `leaflet.markercluster`, `@types/leaflet.markercluster`.
- **No breaking API changes** — existing endpoints are unchanged; `locationName` is a new nullable field added to an existing response body.
