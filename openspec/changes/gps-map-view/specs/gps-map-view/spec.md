# gps-map-view

Specifies the GPS map panel in the EXIF viewer and the dedicated `/map` route with clustered photo pins. GPS coordinates are already stored in `asset_exif` by the `exif-metadata-panel` improvement. This spec covers reverse geocoding via `GeocodingPort`, the mini-map embedded in the EXIF panel, the map pins API, and the `/map` route navigation.

---

## ADDED Requirements

### Requirement: GeocodingPort provides reverse geocoding as a driven port

The system SHALL expose a `GeocodingPort` interface in `domain/port/out/` with a single method `Optional<String> reverseGeocode(double lat, double lon)`. A `NominatimGeocodingAdapter` in `infrastructure/service/` SHALL implement this interface by calling the Nominatim reverse-geocoding API. If the external service is unavailable or returns an error, the adapter SHALL return `Optional.empty()` without propagating an exception.

#### Scenario: Successful reverse geocoding of a known coordinate

- **GIVEN** a valid GPS coordinate pair (e.g. latitude `48.8566`, longitude `2.3522`)
- **WHEN** `NominatimGeocodingAdapter.reverseGeocode(48.8566, 2.3522)` is called
- **THEN** the method returns an `Optional` containing a non-empty string (e.g. `"Paris, FR"`)

#### Scenario: Nominatim returns an error response

- **GIVEN** Nominatim responds with a `500 Internal Server Error`
- **WHEN** `NominatimGeocodingAdapter.reverseGeocode(lat, lon)` is called
- **THEN** the method returns `Optional.empty()` and no exception is thrown

#### Scenario: Nominatim is unreachable (connection timeout)

- **GIVEN** the Nominatim host is unreachable and the connection times out
- **WHEN** `NominatimGeocodingAdapter.reverseGeocode(lat, lon)` is called
- **THEN** the method returns `Optional.empty()` and no exception propagates to the caller

#### Scenario: Coordinate maps to a location with only `town` (no `city`)

- **GIVEN** a coordinate that Nominatim resolves to an address where `address.city` is absent but `address.town` is present
- **WHEN** `NominatimGeocodingAdapter.reverseGeocode(lat, lon)` is called
- **THEN** the method returns an `Optional` containing a string using the town name (e.g. `"Versailles, FR"`)

---

### Requirement: GetAssetExifUseCase populates locationName from GPS coordinates

When `GetAssetExifUseCaseImpl` retrieves an `AssetExif` record whose `gpsLatitude` and `gpsLongitude` are both non-null, it SHALL call `GeocodingPort.reverseGeocode(lat, lon)` and set the returned value (or `null` if `Optional.empty()`) on the `locationName` field of the returned `AssetExif` domain object. When GPS coordinates are absent, `locationName` SHALL remain `null` without calling `GeocodingPort`.

#### Scenario: Asset has GPS coordinates — geocoding succeeds

- **GIVEN** an `AssetExif` record with `gpsLatitude = 51.5074` and `gpsLongitude = -0.1278`
- **WHEN** `GetAssetExifUseCaseImpl.execute(assetId)` is called
- **THEN** `GeocodingPort.reverseGeocode(51.5074, -0.1278)` is called exactly once
- **AND** the returned `AssetExif` has `locationName` set to the value returned by `GeocodingPort`

#### Scenario: Asset has GPS coordinates — geocoding returns empty

- **GIVEN** an `AssetExif` record with non-null `gpsLatitude` and `gpsLongitude`
- **AND** `GeocodingPort.reverseGeocode(lat, lon)` returns `Optional.empty()`
- **WHEN** `GetAssetExifUseCaseImpl.execute(assetId)` is called
- **THEN** the returned `AssetExif` has `locationName = null`

#### Scenario: Asset has no GPS coordinates

- **GIVEN** an `AssetExif` record with `gpsLatitude = null` and `gpsLongitude = null`
- **WHEN** `GetAssetExifUseCaseImpl.execute(assetId)` is called
- **THEN** `GeocodingPort.reverseGeocode` is NOT called
- **AND** the returned `AssetExif` has `locationName = null`

---

### Requirement: GET /api/assets/{id}/exif includes locationName

The `GET /api/assets/{id}/exif` endpoint SHALL include a `locationName` field in its JSON response. When the asset has GPS coordinates and reverse geocoding succeeds, `locationName` SHALL be a non-null string. In all other cases it SHALL be `null`.

#### Scenario: Response includes locationName for a GPS-tagged asset

- **GIVEN** a catalogued asset whose `asset_exif` row has non-null `gps_latitude` and `gps_longitude`
- **AND** `GeocodingPort` returns `"Tokyo, JP"` for those coordinates
- **WHEN** an authenticated user calls `GET /api/assets/{assetId}/exif`
- **THEN** the JSON response contains `"locationName": "Tokyo, JP"`

#### Scenario: Response has null locationName when geocoding unavailable

- **GIVEN** a catalogued asset with GPS coordinates
- **AND** `GeocodingPort` returns `Optional.empty()`
- **WHEN** an authenticated user calls `GET /api/assets/{assetId}/exif`
- **THEN** the JSON response contains `"locationName": null`

#### Scenario: Response has null locationName when no GPS data

- **GIVEN** a catalogued asset whose `asset_exif` row has `gps_latitude = NULL`
- **WHEN** an authenticated user calls `GET /api/assets/{assetId}/exif`
- **THEN** the JSON response contains `"locationName": null`

---

### Requirement: GET /api/assets/map returns paginated GPS-tagged map pins

The system SHALL provide a `GET /api/assets/map` endpoint that returns a paginated list of map pin objects for assets in a specified folder (or album) that have non-null GPS coordinates. Each map pin SHALL contain `assetId`, `gpsLatitude`, `gpsLongitude`, and `thumbnailUrl`. Only non-deleted assets are included. The endpoint SHALL require authentication.

#### Scenario: Folder contains GPS-tagged assets

- **GIVEN** a folder with 5 assets, of which 3 have non-null `gps_latitude` and `gps_longitude` in `asset_exif`
- **WHEN** an authenticated user calls `GET /api/assets/map?folderPath=/photos&page=0`
- **THEN** the response is `200 OK` with a JSON body containing exactly 3 map pin objects, each with `assetId`, `gpsLatitude`, `gpsLongitude`, and `thumbnailUrl`

#### Scenario: Folder contains no GPS-tagged assets

- **GIVEN** a folder whose assets all have `gps_latitude = NULL` in `asset_exif`
- **WHEN** an authenticated user calls `GET /api/assets/map?folderPath=/photos&page=0`
- **THEN** the response is `200 OK` with an empty items array and `total = 0`

#### Scenario: Pagination works correctly

- **GIVEN** a folder with 150 GPS-tagged assets
- **WHEN** an authenticated user calls `GET /api/assets/map?folderPath=/photos&page=0`
- **THEN** the response contains at most 100 map pins and the `total` field reflects 150

#### Scenario: Unauthenticated request

- **GIVEN** no valid JWT cookie is present
- **WHEN** a client calls `GET /api/assets/map?folderPath=/photos`
- **THEN** the response is `401 Unauthorized`

#### Scenario: Request with albumId

- **GIVEN** an album with assets, 4 of which have GPS coordinates
- **WHEN** an authenticated user calls `GET /api/assets/map?albumId=7&page=0`
- **THEN** the response contains exactly 4 map pin objects for those assets

---

### Requirement: ExifPanelComponent displays location name and embedded mini-map

When the EXIF panel is opened for an asset with non-null `gpsLatitude` and `gpsLongitude`, the panel SHALL display a "Location" row showing `locationName` (if non-null) and a Leaflet mini-map tile (approximately 200×150 px) centred on the asset's GPS coordinates with a single marker pin. The mini-map SHALL show OpenStreetMap tiles and support basic scroll-to-zoom. When GPS coordinates are absent, neither the location row nor the mini-map SHALL be rendered. When the EXIF panel component is destroyed, the Leaflet map instance SHALL be cleaned up.

#### Scenario: EXIF panel opens for asset with GPS and locationName

- **GIVEN** the user is in viewer mode for a JPEG asset with `gpsLatitude = 48.8566`, `gpsLongitude = 2.3522`, and `locationName = "Paris, FR"`
- **WHEN** the user clicks the info icon to open the EXIF panel
- **THEN** a "Location" row showing "Paris, FR" is visible in the panel
- **AND** a mini-map tile centred on lat 48.8566 / lon 2.3522 is rendered with a marker pin

#### Scenario: EXIF panel opens for asset with GPS but no locationName

- **GIVEN** the user is in viewer mode for an asset with GPS coordinates but `locationName = null` (e.g. geocoding was unavailable)
- **WHEN** the user clicks the info icon to open the EXIF panel
- **THEN** no "Location" text row is displayed
- **AND** the mini-map tile is still rendered showing the raw coordinates

#### Scenario: EXIF panel opens for asset without GPS data

- **GIVEN** the user is in viewer mode for an asset with `gpsLatitude = null`
- **WHEN** the user clicks the info icon to open the EXIF panel
- **THEN** no "Location" row is rendered
- **AND** no mini-map tile is rendered

#### Scenario: Mini-map is cleaned up when panel closes

- **GIVEN** the EXIF panel is open and a Leaflet mini-map is rendered
- **WHEN** the user closes the EXIF panel (ExifPanelComponent is destroyed)
- **THEN** `miniMapInstance.remove()` is called and the Leaflet instance is released

---

### Requirement: /map route renders a full-page clustered Leaflet map

The `/map` route SHALL render a full-page Leaflet map showing clustered marker pins for GPS-tagged assets in the currently active folder (read from query param `folderPath`) or album (read from query param `albumId`). The map SHALL load the first page of pins from `GET /api/assets/map` on initialisation and load additional pages as the user scrolls or requests more. Clusters SHALL expand on click. The route SHALL be protected by `authGuard`. A "Map" navigation link SHALL be added to the top navigation bar.

#### Scenario: Map route renders pins for a folder

- **GIVEN** the user navigates to `/map?folderPath=/photos`
- **AND** the folder has 8 GPS-tagged assets
- **WHEN** the page loads
- **THEN** the Leaflet map is rendered with 8 marker pins (or clusters grouping nearby pins)

#### Scenario: Map route shows empty state when no GPS-tagged assets exist

- **GIVEN** the user navigates to `/map?folderPath=/no-gps-photos`
- **AND** the folder has no GPS-tagged assets
- **WHEN** the page loads
- **THEN** the Leaflet map is rendered with an informational message: "No photos with GPS data in this folder"

#### Scenario: Clicking a pin navigates to the gallery viewer for that asset

- **GIVEN** the map is showing a pin for an asset with `assetId = 42` in folder `/photos`
- **WHEN** the user clicks the pin (expanding a cluster if necessary)
- **THEN** the Angular router navigates to `/gallery?folderPath=/photos&assetId=42`

#### Scenario: /map route requires authentication

- **GIVEN** no valid JWT cookie is present
- **WHEN** the browser navigates to `/map`
- **THEN** the user is redirected to `/login`

#### Scenario: Map route loads pins for an album

- **GIVEN** the user navigates to `/map?albumId=3`
- **AND** the album contains 12 GPS-tagged assets
- **WHEN** the page loads
- **THEN** the Leaflet map renders 12 pins or clusters

---

### Requirement: GalleryComponent responds to assetId query parameter

When `GalleryComponent` is navigated to with an `assetId` query parameter, it SHALL locate the asset with that ID in the current page, select it, and open viewer mode. If the asset is not present in the first page, it SHALL make additional requests until the asset is found or all pages are exhausted, at which point it SHALL open the asset in viewer mode using a direct `GET /api/assets/{id}` call to retrieve its data.

#### Scenario: Gallery opens viewer for a specific asset from map navigation

- **GIVEN** the map has navigated to `/gallery?folderPath=/photos&assetId=42`
- **WHEN** `GalleryComponent` initialises
- **THEN** the component loads assets for folder `/photos`, finds asset 42, and opens it in viewer mode
