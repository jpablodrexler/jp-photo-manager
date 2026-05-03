# exif-metadata-panel

Specifies how the application reads and displays EXIF metadata from image files in the gallery viewer.

---

### Requirement: Backend exposes EXIF metadata for a catalogued asset

The system SHALL provide a `GET /api/assets/{id}/exif` endpoint that reads EXIF metadata directly from the image file on disk and returns it as JSON. The response SHALL include up to 13 fields: `cameraMake`, `cameraModel`, `dateTaken`, `fNumber`, `exposureTime`, `isoSpeed`, `focalLength`, `flash`, `exposureProgram`, `whiteBalance`, `meteringMode`, `gpsLatitude`, `gpsLongitude`. Any field absent from the file's EXIF segment SHALL be returned as `null`.

#### Scenario: EXIF data present in a JPEG file
- **GIVEN** a catalogued asset whose file is a JPEG with a complete EXIF APP1 segment
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `200 OK` with a JSON body where at least `cameraMake`, `cameraModel`, and `dateTaken` are non-null strings/values matching the file's EXIF tags

#### Scenario: Asset is a PNG with no EXIF
- **GIVEN** a catalogued asset whose file is a PNG
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `200 OK` with a JSON body where all 13 fields are `null`

#### Scenario: Asset ID does not exist
- **GIVEN** no asset with the requested ID exists in the database
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `404 Not Found`

#### Scenario: Asset file is missing from disk
- **GIVEN** a catalogued asset whose file has been deleted from the filesystem after cataloging
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `404 Not Found`

#### Scenario: Unauthenticated request
- **GIVEN** no valid JWT cookie is present in the request
- **WHEN** a client calls `GET /api/assets/{id}/exif`
- **THEN** the response is `401 Unauthorized`

---

### Requirement: GPS coordinates are returned as decimal degrees

When a JPEG file contains GPS EXIF tags, the system SHALL convert the raw DMS (degrees, minutes, seconds) rational values to signed decimal degrees. Latitude SHALL be negative for south hemisphere (`S` reference). Longitude SHALL be negative for west hemisphere (`W` reference).

#### Scenario: GPS data present
- **GIVEN** a JPEG asset with GPS EXIF tags for a location in the northern and western hemispheres (e.g. 37°46'N, 122°25'W)
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** `gpsLatitude` is approximately `37.77` (positive) and `gpsLongitude` is approximately `-122.42` (negative)

#### Scenario: GPS data absent
- **GIVEN** a JPEG asset with no GPS EXIF tags
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** both `gpsLatitude` and `gpsLongitude` are `null`

---

### Requirement: A malformed EXIF tag does not abort the response

If one EXIF tag is unreadable or malformed, the system SHALL still return values for all other tags that could be read successfully.

#### Scenario: One tag is malformed
- **GIVEN** a JPEG whose EXIF segment contains a malformed `fNumber` rational tag but valid `cameraMake` and `cameraModel` tags
- **WHEN** an authenticated user calls `GET /api/assets/{id}/exif`
- **THEN** the response is `200 OK` with `fNumber` as `null` and `cameraMake`/`cameraModel` populated correctly

---

### Requirement: Gallery viewer displays an EXIF metadata panel

The gallery viewer SHALL include an info icon button in the toolbar. Clicking it SHALL toggle a collapsible side panel alongside the full-size image. The panel SHALL show only non-null EXIF fields with human-readable labels. When no EXIF data is available, the panel SHALL display a "No EXIF data available" message. The panel SHALL show a loading spinner while the backend request is in flight.

#### Scenario: Opening the EXIF panel for an image with data
- **GIVEN** the user is in viewer mode for a JPEG asset that has EXIF metadata
- **WHEN** the user clicks the info (ℹ) icon button
- **THEN** a side panel appears next to the image showing at least the camera name and date taken

#### Scenario: Opening the EXIF panel for an image without data
- **GIVEN** the user is in viewer mode for a PNG asset with no EXIF metadata
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
- **GIVEN** the user has already opened the EXIF panel for asset 42 (result cached)
- **WHEN** the user closes and re-opens the panel for asset 42
- **THEN** only one `GET /api/assets/42/exif` request is issued in total
