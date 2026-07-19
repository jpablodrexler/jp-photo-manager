# star-ratings

Specifies how users assign a 0–5 star rating to assets, how ratings are stored and exposed via the API, and how the gallery supports filtering and sorting by rating.

---

### Requirement: Every asset has a persistent 0–5 star rating

The `assets` table SHALL have a `rating SMALLINT NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5)` column. A rating of 0 means unrated. `GET /api/assets` SHALL include a `rating` field in every `AssetDto` in the response. On fresh catalog, all assets start with `rating = 0`.

#### Scenario: Newly cataloged asset has rating 0

- **GIVEN** a folder is cataloged for the first time
- **WHEN** `GET /api/assets?folderPath=/photos/2024&page=0` is called
- **THEN** every asset in the `items` array has `rating: 0`

---

### Requirement: Users can rate an asset via a dedicated PATCH endpoint

`PATCH /api/assets/{id}/rating` SHALL accept `{ "rating": <integer 0-5> }` and set the asset's rating to that value. The response SHALL be `204 No Content`. Ratings outside 0–5 SHALL be rejected with `400 Bad Request`.

#### Scenario: Rating an asset

- **GIVEN** asset 42 has `rating = 0`
- **WHEN** `PATCH /api/assets/42/rating` with body `{ "rating": 4 }`
- **THEN** the response is `204 No Content`; a subsequent `GET /api/assets` for the same folder shows asset 42 with `rating: 4`

#### Scenario: Rating above 5 is rejected

- **GIVEN** any asset exists
- **WHEN** `PATCH /api/assets/42/rating` with body `{ "rating": 6 }`
- **THEN** the response is `400 Bad Request`

#### Scenario: Rating below 0 is rejected

- **GIVEN** any asset exists
- **WHEN** `PATCH /api/assets/42/rating` with body `{ "rating": -1 }`
- **THEN** the response is `400 Bad Request`

#### Scenario: Setting rating to 0 marks asset as unrated

- **GIVEN** asset 42 has `rating = 5`
- **WHEN** `PATCH /api/assets/42/rating` with body `{ "rating": 0 }`
- **THEN** the response is `204 No Content`; asset 42 has `rating: 0`

---

### Requirement: The gallery can filter assets by minimum rating

`GET /api/assets` SHALL accept an optional `minRating` query param (integer 1–5). When provided, only assets with `rating >= minRating` SHALL be returned. When absent or 0, no rating filter is applied.

#### Scenario: Filter returns only assets at or above the minimum rating

- **GIVEN** assets 10 (rating 5), 11 (rating 3), 12 (rating 1), 13 (rating 0) are in folder `/photos/2024`
- **WHEN** `GET /api/assets?folderPath=/photos/2024&page=0&minRating=3`
- **THEN** the response contains assets 10 and 11 only; assets 12 and 13 are absent

#### Scenario: No filter when minRating is absent

- **GIVEN** assets with various ratings are in a folder
- **WHEN** `GET /api/assets?folderPath=/photos/2024&page=0` (no `minRating` param)
- **THEN** all assets regardless of rating are returned

#### Scenario: minRating=1 excludes only unrated assets

- **GIVEN** assets 20 (rating 0), 21 (rating 1) are in a folder
- **WHEN** `GET /api/assets?folderPath=/photos/2024&page=0&minRating=1`
- **THEN** the response contains asset 21; asset 20 is absent

---

### Requirement: The gallery can sort assets by rating descending

`GET /api/assets` with `sort=RATING` SHALL return assets ordered by `rating` descending (highest first). Assets with the same rating are ordered by file name ascending as a tiebreaker.

#### Scenario: Sort by rating places highest-rated assets first

- **GIVEN** assets A (rating 5), B (rating 1), C (rating 3) are in a folder
- **WHEN** `GET /api/assets?folderPath=/photos/2024&page=0&sort=RATING`
- **THEN** the `items` array order is A (5), C (3), B (1)

---

### Requirement: The gallery thumbnail shows a star rating and allows rating from the grid

Each thumbnail card SHALL display the asset's current rating as filled/empty star icons below the image. Clicking a star on a thumbnail SHALL call `PATCH /api/assets/{id}/rating` and update the displayed rating without reloading the page. Clicking the currently active star SHALL set the rating to 0 (toggle off).

#### Scenario: Thumbnail displays current rating

- **GIVEN** asset 42 has `rating = 3`
- **WHEN** the gallery thumbnail grid renders
- **THEN** asset 42's card shows 3 filled stars and 2 empty stars

#### Scenario: Clicking a star rates the asset

- **GIVEN** asset 42 has `rating = 0` and its thumbnail is visible
- **WHEN** the user clicks the 4th star on asset 42's thumbnail card
- **THEN** `PATCH /api/assets/42/rating` is called with `{ "rating": 4 }`; the thumbnail now shows 4 filled stars; no full page reload occurs

#### Scenario: Clicking the active star clears the rating

- **GIVEN** asset 42 has `rating = 4` and its thumbnail is visible
- **WHEN** the user clicks the 4th star on asset 42's thumbnail card
- **THEN** `PATCH /api/assets/42/rating` is called with `{ "rating": 0 }`; the thumbnail shows 0 filled stars

---

### Requirement: The full-size viewer shows a star rating widget in the toolbar

When viewing an asset in full-size viewer mode, the toolbar SHALL display 5 clickable star icon buttons. Clicking a star SHALL call `PATCH /api/assets/{id}/rating` and update the displayed stars immediately.

#### Scenario: Viewer toolbar shows current rating

- **GIVEN** asset 42 has `rating = 5` and is open in the viewer
- **WHEN** the viewer toolbar renders
- **THEN** all 5 star buttons are in a filled state

#### Scenario: Navigating to the next asset shows its rating

- **GIVEN** asset 42 (rating 5) is open; the user presses next to view asset 43 (rating 2)
- **WHEN** asset 43 is displayed
- **THEN** the viewer toolbar shows 2 filled stars and 3 empty stars

---

### Requirement: The filter toolbar includes a minimum-rating selector

The gallery filter toolbar (added by the search-and-filter feature) SHALL include a row of 5 clickable star icons representing the minimum rating filter. The selected star and all stars below it SHALL appear filled; stars above SHALL appear empty. Clicking a filled star that is the current minimum SHALL reset the filter to 0 (any rating). Changing the minimum rating SHALL reload the current page from page 0.

#### Scenario: Selecting minimum rating 3 filters the grid

- **GIVEN** the gallery shows assets with various ratings
- **WHEN** the user clicks the 3rd filter star
- **THEN** `GET /api/assets` is called with `minRating=3`; only assets with rating ≥ 3 appear in the grid

#### Scenario: Clicking the active filter star clears the rating filter

- **GIVEN** the minimum rating filter is set to 3
- **WHEN** the user clicks the 3rd filter star again
- **THEN** `GET /api/assets` is called with no `minRating` param; all assets appear

---

### Requirement: Ratings are excluded from the soft-delete recycle bin display

Rating information SHALL be included in the `AssetDto` returned by `GET /api/recycle-bin`. The recycle bin SHALL display the rating of soft-deleted assets in the same thumbnail format as the gallery. Rating SHALL be preserved through soft delete and restore — it SHALL NOT be reset on restore.

#### Scenario: Rating is preserved after soft delete and restore

- **GIVEN** asset 42 has `rating = 5`
- **WHEN** asset 42 is soft-deleted (`DELETE /api/assets?assetIds=42&deleteFiles=false`) and then restored (`POST /api/recycle-bin/restore` with `{ "assetIds": [42] }`)
- **THEN** asset 42 has `rating = 5` in the gallery
