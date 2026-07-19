# wallpaper-suggestion

A `GET /api/assets/wallpaper-suggestion` endpoint returns a random non-deleted asset whose dimensions meet or exceed the caller's screen dimensions and whose aspect ratio is within ±0.02 of the screen's aspect ratio. The frontend reads the screen dimensions, calls the endpoint, and shows the result with a download button.

---

## ADDED Requirements

### Requirement: aspect_ratio column is persisted at catalog time

The `assets` table SHALL contain an `aspect_ratio FLOAT` column populated during cataloging as `pixel_width / pixel_height`. Rows with `pixel_height = 0` SHALL have `aspect_ratio = NULL`.

#### Scenario: New asset is cataloged with aspect_ratio

- **GIVEN** an image with dimensions 1920×1080
- **WHEN** the catalog service processes it
- **THEN** the `asset` row has `aspect_ratio ≈ 1.7778`

#### Scenario: V15 migration backfills existing rows

- **WHEN** the V15 Flyway migration runs
- **THEN** all existing `assets` rows with `pixel_height > 0` have `aspect_ratio` populated; rows with `pixel_height = 0` have `aspect_ratio = NULL`

### Requirement: Wallpaper suggestion endpoint returns a matching asset

`GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` SHALL return a random non-deleted `AssetDto` where `pixel_width >= W`, `pixel_height >= H`, and `ABS(aspect_ratio - (W/H)) <= 0.02`. If no matching asset exists, the endpoint SHALL return `404 Not Found`.

#### Scenario: Matching asset is returned

- **GIVEN** an asset with dimensions 1920×1080 exists in the catalog
- **WHEN** `GET /api/assets/wallpaper-suggestion?screenWidth=1920&screenHeight=1080` is called
- **THEN** the response is `200 OK` with the matching `AssetDto`

#### Scenario: No matching asset returns 404

- **GIVEN** no asset meets the dimension and ratio criteria
- **WHEN** `GET /api/assets/wallpaper-suggestion?screenWidth=7680&screenHeight=4320` is called
- **THEN** the response is `404 Not Found`

#### Scenario: Soft-deleted assets are excluded

- **GIVEN** the only matching asset has `deleted_at` set
- **WHEN** the endpoint is called with matching dimensions
- **THEN** the response is `404 Not Found`

### Requirement: Frontend shows the suggestion with a download button

The gallery toolbar SHALL include a "Suggest wallpaper" action that reads the screen dimensions, calls the endpoint, and shows the result in a dialog with a download button.

#### Scenario: Suggestion dialog shows the recommended image

- **GIVEN** a matching asset exists
- **WHEN** the user clicks "Suggest wallpaper" in the gallery toolbar
- **THEN** a dialog opens showing the asset thumbnail, filename, and dimensions, with a "Download" button linking to `GET /api/assets/{id}/image`

#### Scenario: No match shows a friendly message

- **GIVEN** no asset matches the screen dimensions
- **WHEN** the user clicks "Suggest wallpaper"
- **THEN** the dialog shows "No wallpaper found matching your screen resolution"
