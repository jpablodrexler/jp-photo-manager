## ADDED Requirements

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
