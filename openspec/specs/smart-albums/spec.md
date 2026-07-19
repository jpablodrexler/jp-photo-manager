# smart-albums

Specifies how albums can be optionally backed by a stored filter expression (`filter_json`) so that their contents are computed dynamically at query time from all catalogued folders instead of being read from the static `album_assets` join table. The filter shape reuses the same `{ search, dateFrom, dateTo, minRating }` structure already used by saved search presets.

---

## ADDED Requirements

### Requirement: The `albums` table has a nullable `filter_json JSONB` column

The database schema SHALL be extended by Flyway migration `V14__add_smart_albums.sql` which adds `filter_json JSONB NULL` to the `albums` table. A partial index `ix_albums_filter_json_not_null` SHALL be created on rows where `filter_json IS NOT NULL`. Existing albums SHALL have `filter_json = NULL` and SHALL continue to behave as static albums. No data in the `album_assets` table is modified by this migration.

#### Scenario: Migration runs on an existing database

- **GIVEN** a PostgreSQL database at schema version V13 with existing static albums and `album_assets` rows
- **WHEN** Flyway applies `V14__add_smart_albums.sql`
- **THEN** the `albums` table has a new `filter_json` column of type `JSONB`; all existing rows have `filter_json = NULL`; the `album_assets` table is unchanged; schema version advances to V14

---

### Requirement: Creating an album with `filterJson` makes it a smart album

`POST /api/albums` SHALL accept an optional `filterJson` object in the request body containing any combination of `search` (string), `dateFrom` (ISO date string `YYYY-MM-DD`), `dateTo` (ISO date string `YYYY-MM-DD`), and `minRating` (integer 1–5). When `filterJson` is present and at least one of its fields is non-null, the album SHALL be stored with a non-null `filter_json` column value and treated as a smart album. When `filterJson` is absent or null, the album is a static album. A `filterJson` object where all fields are null or missing SHALL return `400 Bad Request`.

#### Scenario: Create a smart album with a minimum rating filter

- **GIVEN** an authenticated user with some catalogued assets, some rated 4 or 5 stars
- **WHEN** the client calls `POST /api/albums` with body `{ "name": "Top Picks", "filterJson": { "minRating": 4 } }`
- **THEN** the response is `201 Created`; the returned `AlbumSummaryDto` has `name: "Top Picks"` and `filterJson: { "minRating": 4 }`; the `albums` table row has a non-null `filter_json` JSONB value; no row is inserted into `album_assets`

#### Scenario: Create a smart album with multiple filter criteria

- **GIVEN** an authenticated user
- **WHEN** the client calls `POST /api/albums` with body `{ "name": "Vacation 2024", "filterJson": { "search": "vacation", "dateFrom": "2024-01-01", "dateTo": "2024-12-31" } }`
- **THEN** the response is `201 Created` with `filterJson` containing all three criteria

#### Scenario: Create a static album without filterJson

- **GIVEN** an authenticated user
- **WHEN** the client calls `POST /api/albums` with body `{ "name": "Manual Collection" }` (no `filterJson`)
- **THEN** the response is `201 Created` with `filterJson: null`; the album is a static album

#### Scenario: Create album with empty filterJson is rejected

- **GIVEN** an authenticated user
- **WHEN** the client calls `POST /api/albums` with body `{ "name": "Bad Smart", "filterJson": {} }`
- **THEN** the response is `400 Bad Request` because `filterJson` is present but all fields are absent/null

---

### Requirement: Updating an album can set, change, or clear its filter

`PUT /api/albums/{id}` SHALL accept the optional `filterJson` field in the request body. Sending a non-null `filterJson` with at least one criterion SHALL set or replace the album's stored filter, converting a static album to smart mode or updating an existing smart filter. Sending `filterJson: null` explicitly SHALL clear the stored filter, reverting the album to static mode. The `album_assets` join table rows SHALL NOT be modified in either direction of the toggle.

#### Scenario: Convert a static album to smart mode

- **GIVEN** album 10 is a static album with `filterJson = null` and two manually added assets in `album_assets`
- **WHEN** the client calls `PUT /api/albums/10` with body `{ "name": "Auto Vacation", "filterJson": { "search": "vacation" } }`
- **THEN** the response is `200 OK` with `filterJson: { "search": "vacation" }`; `GET /api/albums/10` now returns assets matching the `search=vacation` filter across all folders; the two `album_assets` rows are still present in the database but are not included in the response

#### Scenario: Revert a smart album to static mode

- **GIVEN** album 10 is a smart album with `filterJson = { "search": "vacation" }` and two `album_assets` rows from before the conversion
- **WHEN** the client calls `PUT /api/albums/10` with body `{ "name": "Vacation Static", "filterJson": null }`
- **THEN** the response is `200 OK` with `filterJson: null`; `GET /api/albums/10` now returns the two statically added assets from `album_assets`

#### Scenario: Update the filter criteria of a smart album

- **GIVEN** album 12 is a smart album with `filterJson = { "minRating": 3 }`
- **WHEN** the client calls `PUT /api/albums/12` with body `{ "name": "Top Rated", "filterJson": { "minRating": 5 } }`
- **THEN** the response is `200 OK` with `filterJson: { "minRating": 5 }`; `GET /api/albums/12` now returns only 5-star assets

---

### Requirement: GET /api/albums/{id} returns dynamically computed assets for smart albums

When `GET /api/albums/{id}` is called for a smart album (one where `filter_json` is non-null), the paginated assets in the response SHALL be sourced by running the stored filter criteria against all catalogued non-deleted folders using the same `AssetFilter` query path used by `GET /api/assets`. The `filter_json` criteria SHALL be applied as follows: `search` as a case-insensitive filename substring match; `dateFrom` and `dateTo` as inclusive bounds on `file_creation_date_time`; `minRating` as a minimum inclusive bound on the `rating` column. Assets with `deleted_at IS NOT NULL` SHALL be excluded. The `assetCount` (total items matching the filter) SHALL reflect the filtered total, not the count of `album_assets` rows.

#### Scenario: Smart album returns assets matching the stored filter across all folders

- **GIVEN** a smart album with `filterJson = { "minRating": 4 }` and the catalog contains 12 assets rated 4 or 5 stars spread across three different folders
- **WHEN** the client calls `GET /api/albums/{id}?page=0`
- **THEN** the response is `200 OK`; `assets.totalItems` is 12; `assets.items` contains up to 50 assets all having `rating >= 4`; assets from all three folders appear

#### Scenario: Smart album count excludes soft-deleted assets

- **GIVEN** a smart album with `filterJson = { "search": "holiday" }` and the catalog contains 8 assets with "holiday" in the filename, of which 2 are soft-deleted (`deleted_at IS NOT NULL`)
- **WHEN** the client calls `GET /api/albums/{id}?page=0`
- **THEN** `assets.totalItems` is 6; the 2 deleted assets are absent from `assets.items`

#### Scenario: Smart album pagination works correctly

- **GIVEN** a smart album with `filterJson = { "dateFrom": "2023-01-01" }` and 120 matching assets in the catalog; page size is 50
- **WHEN** the client calls `GET /api/albums/{id}?page=0`
- **THEN** `assets.totalItems` is 120; `assets.totalPages` is 3; `assets.items` contains 50 assets
- **WHEN** the client calls `GET /api/albums/{id}?page=2`
- **THEN** `assets.items` contains 20 assets; `assets.pageIndex` is 2

#### Scenario: Static album is unaffected by smart album changes

- **GIVEN** a static album with no `filterJson` and 5 assets added via `album_assets`
- **WHEN** the client calls `GET /api/albums/{id}?page=0`
- **THEN** exactly the 5 statically added assets are returned; behavior is identical to pre-smart-album implementation

---

### Requirement: Album list includes filterJson in summary responses

`GET /api/albums` SHALL return `AlbumSummaryDto` objects that include the `filterJson` field. For static albums this field SHALL be null. For smart albums it SHALL contain the stored filter object. The `assetCount` field in the summary SHALL reflect the current dynamic count for smart albums (computed via the filter query) and the static join-table count for static albums.

#### Scenario: Album list exposes filterJson for smart albums

- **GIVEN** the user has one static album ("Manual") and one smart album ("Top Picks" with `filterJson = { "minRating": 5 }`)
- **WHEN** the client calls `GET /api/albums`
- **THEN** the response contains two items; "Manual" has `filterJson: null`; "Top Picks" has `filterJson: { "minRating": 5 }`

#### Scenario: assetCount for a smart album reflects dynamic result count

- **GIVEN** a smart album with `filterJson = { "minRating": 4 }` and 15 assets currently rated 4 or 5 stars in the catalog
- **WHEN** the client calls `GET /api/albums`
- **THEN** the smart album's `assetCount` is 15

#### Scenario: assetCount updates as catalog changes

- **GIVEN** a smart album with `filterJson = { "search": "beach" }` and `assetCount: 7`; a new asset named `beach_sunset.jpg` is catalogued
- **WHEN** the client calls `GET /api/albums` again
- **THEN** the smart album's `assetCount` is 8

---

### Requirement: Assets cannot be manually added to or removed from a smart album

`POST /api/albums/{id}/assets` and `DELETE /api/albums/{id}/assets` SHALL return `422 Unprocessable Entity` when called on an album that has a non-null `filter_json`. The response body SHALL include a machine-readable error code `SMART_ALBUM_MEMBERSHIP_FORBIDDEN` and a human-readable message.

#### Scenario: Adding assets to a smart album is rejected

- **GIVEN** album 7 is a smart album with `filterJson = { "minRating": 3 }`
- **WHEN** the client calls `POST /api/albums/7/assets` with body `{ "assetIds": [101, 102] }`
- **THEN** the response is `422 Unprocessable Entity` with body `{ "code": "SMART_ALBUM_MEMBERSHIP_FORBIDDEN", "message": "Cannot manually add assets to a smart album" }`; no rows are inserted into `album_assets`

#### Scenario: Removing assets from a smart album is rejected

- **GIVEN** album 7 is a smart album
- **WHEN** the client calls `DELETE /api/albums/7/assets` with body `{ "assetIds": [101] }`
- **THEN** the response is `422 Unprocessable Entity` with body `{ "code": "SMART_ALBUM_MEMBERSHIP_FORBIDDEN", "message": "Cannot manually remove assets from a smart album" }`

---

### Requirement: The frontend AlbumsComponent badges smart albums

The `AlbumsComponent` album-card grid SHALL display a "Smart" `MatChip` or badge on cards where `filterJson` is non-null, positioned at the top-right of the card. Hovering the badge SHALL show a tooltip summarising the stored filter criteria (e.g. "Min rating: 4", "Search: vacation"). Static albums SHALL show no badge.

#### Scenario: Smart albums have a visual badge in the list

- **GIVEN** the user has a static album "Favourites" and a smart album "Top Picks"
- **WHEN** the user navigates to `/albums`
- **THEN** "Favourites" has no smart badge; "Top Picks" shows a "Smart" chip; hovering the chip shows the filter summary

#### Scenario: Create-album form has a "Make smart" toggle

- **GIVEN** the create-album form is visible in `AlbumsComponent`
- **WHEN** the user activates the "Make smart" toggle
- **THEN** filter input fields appear: a text input for "Search", date pickers for "Date from" and "Date to", and a star-rating selector for "Min rating"; the user can fill any subset of these fields before confirming

#### Scenario: Confirming create with smart toggle builds correct request

- **GIVEN** the user has entered album name "Unrated 2025", activated the smart toggle, and set `dateFrom = 2025-01-01`, `minRating = 0` (left unset)
- **WHEN** the user confirms
- **THEN** `POST /api/albums` is called with body `{ "name": "Unrated 2025", "filterJson": { "dateFrom": "2025-01-01" } }`; only non-null filter fields are sent

---

### Requirement: The frontend AlbumDetailComponent adapts its UI for smart albums

When `AlbumDetailComponent` is loaded for a smart album, it SHALL display a banner or info chip below the toolbar reading "Smart album — contents are populated automatically based on: [filter summary]". The per-asset "Remove" button SHALL be hidden. The paginated grid otherwise renders identically using the same `ThumbnailComponent`. For static albums the UI is unchanged.

#### Scenario: Smart album detail shows mode indicator

- **GIVEN** the user navigates to `/albums/7` where album 7 is a smart album with `filterJson = { "minRating": 4 }`
- **WHEN** `AlbumDetailComponent` finishes loading
- **THEN** a banner reads "Smart album — contents are populated automatically based on: Min rating: 4"; no "Remove" button is shown on any asset card

#### Scenario: Static album detail shows remove button

- **GIVEN** the user navigates to `/albums/5` where album 5 is a static album
- **WHEN** `AlbumDetailComponent` finishes loading
- **THEN** no smart-album banner is shown; each asset card has a "Remove" button

#### Scenario: AlbumDetailComponent allows editing filter criteria of a smart album

- **GIVEN** the user is viewing a smart album detail page for album 7 with `filterJson = { "minRating": 4 }`
- **WHEN** the user clicks the "Edit filter" button in the toolbar
- **THEN** a dialog opens pre-populated with `minRating = 4`; the user can change criteria and confirm; `PUT /api/albums/7` is called with the updated `filterJson`; the page reloads with new results

---

### Requirement: The "Add to album" dialog in the gallery disables smart albums as targets

The `AddToAlbumDialogComponent` opened from the gallery thumbnail context menu SHALL list smart albums as disabled `MatOption` entries with a visual indicator and tooltip "Smart album — managed automatically". Only static albums SHALL be selectable as targets for the manual add operation.

#### Scenario: Smart album is shown but disabled in "Add to album" dialog

- **GIVEN** the user has static album "Favourites" and smart album "Top Pics"
- **WHEN** the user opens "Add to album" on a gallery asset
- **THEN** "Favourites" is selectable; "Top Pics" is rendered as a disabled option with the tooltip text "Smart album — managed automatically"; selecting "Top Pics" is not possible

---

### Requirement: Smart album endpoints require authentication

All `/api/albums/**` endpoints (including the new smart-album create/update path) SHALL continue to require a valid JWT cookie, consistent with the existing virtual-albums security requirement. Unauthenticated requests SHALL receive `401 Unauthorized`.

#### Scenario: Unauthenticated request to create a smart album

- **GIVEN** no valid JWT cookie is present
- **WHEN** the client calls `POST /api/albums` with a `filterJson` body
- **THEN** the response is `401 Unauthorized`
