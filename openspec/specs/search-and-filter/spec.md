# search-and-filter

Specifies how the gallery filters its asset list by filename substring and creation-date range, applied server-side and composed with the existing sort and pagination parameters.

---

### Requirement: The GET /api/assets endpoint accepts optional filename and date-range filters

`GET /api/assets` SHALL accept three additional optional query parameters: `search` (case-insensitive filename substring), `dateFrom` (ISO date `YYYY-MM-DD`, inclusive lower bound on `file_creation_date_time`), and `dateTo` (ISO date `YYYY-MM-DD`, inclusive upper bound). When all three are absent, the endpoint SHALL behave identically to its current implementation. When one or more are present, only assets satisfying all provided constraints SHALL be returned. Filters SHALL compose with the existing `sort` and `page` parameters.

#### Scenario: Filename search returns matching assets

- **GIVEN** a folder contains assets `IMG_0001.jpg`, `vacation_beach.jpg`, and `work_document.jpg`
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&search=vacation`
- **THEN** the response is `200 OK`; the `items` array contains only `vacation_beach.jpg`; the other two assets are absent

#### Scenario: Filename search is case-insensitive

- **GIVEN** an asset named `Holiday_2024.JPG` exists in the folder
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&search=holiday`
- **THEN** the response includes `Holiday_2024.JPG` (case-insensitive match)

#### Scenario: Date range lower bound filters assets

- **GIVEN** a folder contains assets with `file_creation_date_time` on 2023-06-15, 2024-03-01, and 2024-09-20
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&dateFrom=2024-01-01`
- **THEN** the response includes only the 2024-03-01 and 2024-09-20 assets; the 2023-06-15 asset is absent

#### Scenario: Date range upper bound filters assets

- **GIVEN** a folder contains assets with creation dates 2023-06-15, 2024-03-01, and 2024-09-20
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&dateTo=2023-12-31`
- **THEN** the response includes only the 2023-06-15 asset

#### Scenario: Combined search and date range

- **GIVEN** a folder contains `vacation_2023.jpg` (created 2023-07-10) and `vacation_2024.jpg` (created 2024-07-10)
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&search=vacation&dateFrom=2024-01-01`
- **THEN** only `vacation_2024.jpg` is returned

#### Scenario: No filters returns all assets (backward compatibility)

- **GIVEN** a folder contains 50 assets
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&page=0&sort=FILE_NAME` (no `search`, `dateFrom`, or `dateTo`)
- **THEN** the response is `200 OK` with the same result as before this change — 50 assets on page 0 with no filtering applied

---

### Requirement: Filters apply to the current page and total count

The `totalItems` and `totalPages` fields in the paginated response SHALL reflect the count of assets matching all applied filters, not the total count of assets in the folder. Pagination applied on top of filters SHALL work correctly across multiple pages.

#### Scenario: Total count reflects filtered result

- **GIVEN** a folder has 200 assets, 15 of which contain "beach" in the filename
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&search=beach&page=0`
- **THEN** `totalItems` is 15; `totalPages` is 1 (assuming page size ≥ 15)

#### Scenario: Multi-page filtered results

- **GIVEN** a folder has 200 assets, 120 of which were created after 2023-01-01; page size is 100
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&dateFrom=2023-01-01&page=0`
- **THEN** `totalItems` is 120, `totalPages` is 2, `items` contains 100 assets
- **WHEN** the client then calls `page=1`
- **THEN** `items` contains the remaining 20 assets

---

### Requirement: The gallery shows a filter toolbar with a search field and date pickers

The `GalleryComponent` thumbnail view SHALL display a filter toolbar row beneath the main toolbar, containing a text input for filename search, two `MatDatepicker` fields (Date from, Date to), and a clear-all button. The filter toolbar SHALL be visible only in thumbnails mode (not in viewer mode).

#### Scenario: Search input is debounced

- **GIVEN** the user is viewing a catalogued folder
- **WHEN** the user types "beach" character by character into the search field
- **THEN** only one `GET /api/assets` request is issued (after a 400 ms pause), not one per keystroke; the gallery grid updates to show matching assets

#### Scenario: Date picker triggers immediate filter

- **GIVEN** the user is viewing a folder
- **WHEN** the user selects a date from the `MatDatepicker` for "Date from"
- **THEN** `GET /api/assets` is called immediately (no debounce) with `dateFrom` set to the selected date; the gallery grid updates

#### Scenario: Clear button resets all filters

- **GIVEN** the user has entered `searchTerm = "holiday"`, `dateFrom = 2024-01-01`, and `dateTo = 2024-12-31`
- **WHEN** the user clicks the clear-filters button (`filter_alt_off` icon)
- **THEN** all three filter fields are reset to empty/null; `GET /api/assets` is called with no filter parameters; the full asset list for the current folder is shown

---

### Requirement: Changing the folder resets all active filters

When the user selects a different folder in `FolderNavComponent`, all filter state (search term, dateFrom, dateTo) SHALL be cleared before loading the new folder's assets. The new request SHALL contain no filter parameters.

#### Scenario: Folder change clears filters

- **GIVEN** the user has `search = "summer"` active while viewing folder A
- **WHEN** the user selects folder B from the navigation tree
- **THEN** `searchTerm`, `dateFrom`, and `dateTo` are reset to empty/null; `GET /api/assets?folderPath=<folder B>` is called with no filter parameters; the gallery shows all assets from folder B

---

### Requirement: Changing any filter resets pagination to page 0

When the search term, dateFrom, or dateTo value changes, `pageIndex` SHALL be reset to 0 before the filtered request is issued. This ensures the user always sees the first page of results for the new filter, not a potentially out-of-range page.

#### Scenario: Filter change resets to page 0

- **GIVEN** the user is on page 2 of a folder with sort=FILE_NAME and no filters
- **WHEN** the user types a search term
- **THEN** the next `GET /api/assets` request uses `page=0`; the pagination display resets to page 1

---

### Requirement: Assets without a creation date are excluded by date-range filters

Assets whose `file_creation_date_time` is `NULL` in the database SHALL be excluded when a `dateFrom` or `dateTo` filter is active. They SHALL be included when no date filter is applied.

#### Scenario: Null creation date excluded by date filter

- **GIVEN** a folder has three assets: two with known creation dates and one with `file_creation_date_time = NULL`
- **WHEN** the client calls `GET /api/assets?folderPath=/photos&dateFrom=2020-01-01`
- **THEN** only the two assets with non-null creation dates that satisfy the filter are returned; the asset with a null date is absent

---

### Requirement: The GET /api/assets endpoint accepts an optional tags filter
`GET /api/assets` SHALL accept an optional `tags` query parameter containing a comma-separated list of tag names. When `tags` is present, only assets that have **all** of the listed tags SHALL be returned (AND semantics). The `tags` filter SHALL compose with all existing filter parameters (`search`, `dateFrom`, `dateTo`, `minRating`, `sort`, `page`). When `tags` is absent, the endpoint SHALL behave identically to its current implementation.

#### Scenario: Single tag filter returns matching assets
- **WHEN** `GET /api/assets?folderPath=/photos&tags=vacation` is called
- **THEN** only assets tagged `"vacation"` are included in the response; untagged assets and assets with different tags are absent

#### Scenario: Multiple tags filter uses AND semantics
- **WHEN** `GET /api/assets?folderPath=/photos&tags=vacation,family` is called
- **THEN** only assets that have **both** `"vacation"` and `"family"` tags are returned; assets with only one of the two are absent

#### Scenario: Tags filter composes with search filter
- **WHEN** `GET /api/assets?folderPath=/photos&search=beach&tags=vacation` is called
- **THEN** only assets whose filename contains `"beach"` AND that are tagged `"vacation"` are returned

#### Scenario: No matching assets returns empty list
- **WHEN** `GET /api/assets?folderPath=/photos&tags=nonexistent-tag` is called
- **THEN** the response is `200 OK` with an empty `items` list and `totalItems: 0`

#### Scenario: Absent tags parameter preserves existing behavior
- **WHEN** `GET /api/assets?folderPath=/photos&page=0` is called with no `tags` parameter
- **THEN** the response is identical to the pre-tagging behavior; no filtering by tag is applied

---

### Requirement: The gallery filter toolbar includes a tag selector
The gallery filter toolbar SHALL display a tag multi-select control alongside the existing search field and date pickers. The control SHALL show previously selected tags as removable chips and offer autocomplete suggestions from `GET /api/tags?q=<input>`. Selecting or removing a tag SHALL immediately reload the asset list with the updated tag filter applied.

#### Scenario: User selects a tag from autocomplete
- **WHEN** the user types `"vac"` in the tag selector and selects `"vacation"` from the suggestions
- **THEN** `GET /api/assets` is called with `tags=vacation`; the gallery shows only tagged assets; a `"vacation"` chip appears in the filter bar

#### Scenario: User removes a tag filter chip
- **WHEN** the user clicks × on the `"vacation"` chip in the filter bar
- **THEN** `GET /api/assets` is called without `tags=vacation`; the gallery reloads with the tag filter removed

#### Scenario: Multiple tag chips compose as AND
- **WHEN** the user has `"vacation"` and `"family"` chips active in the filter bar
- **THEN** `GET /api/assets` is called with `tags=vacation,family`; only assets with both tags are shown

#### Scenario: Clearing all filters removes tag chips
- **WHEN** the user clicks the clear-filters button while tag chips are active
- **THEN** all tag chips are removed; `GET /api/assets` is called with no `tags` parameter
