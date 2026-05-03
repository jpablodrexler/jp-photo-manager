# exif-metadata-panel

Specifies how the application extracts, persists, and displays EXIF metadata for catalogued image assets.

---

### Requirement: EXIF metadata is extracted and persisted during cataloging

The catalog process SHALL extract up to 13 EXIF fields from each image file and persist them in the `asset_exif` table linked to the corresponding `Asset` row. The fields are: `cameraMake`, `cameraModel`, `dateTaken`, `fNumber`, `exposureTime`, `isoSpeed`, `focalLength`, `flash`, `exposureProgram`, `whiteBalance`, `meteringMode`, `gpsLatitude`, `gpsLongitude`. Any field absent from the file's EXIF segment SHALL be stored as `NULL`.

#### Scenario: JPEG file with full EXIF cataloged for the first time
- **GIVEN** a JPEG image file with a complete EXIF APP1 segment exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a row is inserted into `asset_exif` linked to the new `Asset`, with at least `cameraMake`, `cameraModel`, and `dateTaken` populated from the file's EXIF tags

#### Scenario: PNG file with no EXIF cataloged
- **GIVEN** a PNG image file (no EXIF segment) exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a row is inserted into `asset_exif` linked to the new `Asset`, with all 13 fields set to `NULL`

#### Scenario: File is re-cataloged
- **GIVEN** an asset and its `asset_exif` row already exist from a previous catalog run
- **WHEN** the catalog process re-processes the same file
- **THEN** the existing `asset_exif` row is replaced with freshly extracted values

#### Scenario: Asset is deleted
- **GIVEN** an `Asset` row and its corresponding `asset_exif` row exist
- **WHEN** the `Asset` row is deleted
- **THEN** the `asset_exif` row is also deleted automatically via `ON DELETE CASCADE`

---

### Requirement: Backend exposes persisted EXIF metadata for a catalogued asset

The system SHALL provide a `GET /api/assets/{id}/exif` endpoint that reads the `asset_exif` row from the database and returns it as JSON. The response SHALL include all 13 fields, with absent fields returned as `null`. No disk I/O is performed at query time.

#### Scenario: Asset has EXIF data in the database
- **GIVEN** a catalogued asset with a populated `asset_exif` row
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `200 OK` with a JSON body where the non-null fields match the values stored in `asset_exif`

#### Scenario: Asset has no EXIF row (cataloged before this feature)
- **GIVEN** a catalogued asset with no corresponding `asset_exif` row (e.g., cataloged before this feature was deployed)
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `200 OK` with a JSON body where all 13 fields are `null`

#### Scenario: Asset ID does not exist
- **GIVEN** no asset with the requested ID exists in the `assets` table
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `404 Not Found`

#### Scenario: Unauthenticated request
- **GIVEN** no valid JWT cookie is present in the request
- **WHEN** a client calls `GET /api/assets/{id}/exif`
- **THEN** the response is `401 Unauthorized`

---

### Requirement: GPS coordinates are stored and returned as signed decimal degrees

When a JPEG file contains GPS EXIF tags, the catalog process SHALL convert the raw DMS (degrees, minutes, seconds) rational values to signed decimal degrees before persisting them. Latitude SHALL be negative for the southern hemisphere (`S` reference). Longitude SHALL be negative for the western hemisphere (`W` reference).

#### Scenario: GPS data present in cataloged file
- **GIVEN** a JPEG asset with GPS EXIF tags for a location in the northern and western hemispheres (e.g. 37°46'N, 122°25'W)
- **WHEN** the catalog process indexes the file
- **THEN** `gps_latitude` in `asset_exif` is approximately `37.77` (positive) and `gps_longitude` is approximately `-122.42` (negative)

#### Scenario: GPS data returned by the API
- **GIVEN** a catalogued asset whose `asset_exif` row has `gps_latitude = 37.77` and `gps_longitude = -122.42`
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response contains `"gpsLatitude": 37.77` and `"gpsLongitude": -122.42`

#### Scenario: GPS data absent
- **GIVEN** a JPEG asset with no GPS EXIF tags
- **WHEN** the catalog process indexes the file
- **THEN** `gps_latitude` and `gps_longitude` in `asset_exif` are both `NULL`

---

### Requirement: A malformed EXIF tag during cataloging does not abort the catalog or lose other fields

If one EXIF tag is unreadable or malformed, the catalog process SHALL store `NULL` for that field and continue extracting and storing the remaining tags. The catalog process SHALL NOT fail or skip the file because of a malformed EXIF tag.

#### Scenario: One tag is malformed during cataloging
- **GIVEN** a JPEG file whose EXIF segment contains a malformed `fNumber` rational tag but valid `cameraMake` and `cameraModel` tags
- **WHEN** the catalog process indexes the file
- **THEN** the `asset_exif` row has `f_number = NULL`, `camera_make` and `camera_model` populated correctly, and the file is fully indexed as an `Asset`

---

### Requirement: Gallery viewer displays an EXIF metadata panel

The gallery viewer SHALL include an info icon button in the toolbar. Clicking it SHALL toggle a collapsible side panel alongside the full-size image. The panel SHALL show only non-null EXIF fields with human-readable labels. When no EXIF data is available (all fields null), the panel SHALL display a "No EXIF data available" message. The panel SHALL show a loading spinner while the backend request is in flight.

#### Scenario: Opening the EXIF panel for an asset with data
- **GIVEN** the user is in viewer mode for a JPEG asset that has a populated `asset_exif` row
- **WHEN** the user clicks the info (ℹ) icon button
- **THEN** a side panel appears next to the image showing at least the camera name and date taken

#### Scenario: Opening the EXIF panel for an asset without data
- **GIVEN** the user is in viewer mode for an asset whose `asset_exif` row has all-null fields (e.g., a PNG)
- **WHEN** the user clicks the info icon button
- **THEN** the panel appears and displays "No EXIF data available"

#### Scenario: Closing the EXIF panel
- **GIVEN** the EXIF panel is open
- **WHEN** the user clicks the close (✕) button inside the panel
- **THEN** the panel is hidden

#### Scenario: Panel hides when leaving the viewer
- **GIVEN** the EXIF panel is open in viewer mode
- **WHEN** the user clicks the grid icon to return to thumbnail mode
- **THEN** the panel is hidden and `showExifPanel` is reset to `false`

---

### Requirement: EXIF data is fetched at most once per asset per gallery session

The `ExifPanelComponent` SHALL cache the EXIF response keyed by `assetId` for the lifetime of the gallery route. Re-opening the panel for the same asset SHALL display the cached result without issuing a new HTTP request.

#### Scenario: Toggling the panel for the same asset twice
- **GIVEN** the user has already opened the EXIF panel for asset 42 (result cached from the DB-backed API)
- **WHEN** the user closes and re-opens the panel for asset 42
- **THEN** only one `GET /api/assets/42/exif` request is issued in total
