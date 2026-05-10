# saved-search-presets

Specifies how users save the current gallery filter criteria as named presets and restore them with a single interaction, so that frequently used filter combinations do not need to be re-entered manually.

---

### Requirement: Users can save the current gallery filters as a named preset

`POST /api/search-presets` SHALL accept `{ "name": string, "search"?: string, "dateFrom"?: string, "dateTo"?: string, "minRating"?: integer }` and persist the preset scoped to the authenticated user. The response SHALL be `201 Created` with a `SearchPresetDto` including the new `presetId`. An empty or missing `name` SHALL return `400 Bad Request`.

#### Scenario: Save current filters as a preset

- **GIVEN** the gallery filter has `search="vacation"` and `minRating=3`
- **WHEN** `POST /api/search-presets` with body `{ "name": "Vacation 3-star", "search": "vacation", "minRating": 3 }`
- **THEN** the response is `201 Created`; the body contains `presetId`, `name: "Vacation 3-star"`, `search: "vacation"`, `minRating: 3`

#### Scenario: Preset with empty name is rejected

- **GIVEN** any authenticated user
- **WHEN** `POST /api/search-presets` with body `{ "name": "" }`
- **THEN** the response is `400 Bad Request`

#### Scenario: Partial preset (only some filter fields)

- **GIVEN** only `dateFrom` is set in the filter
- **WHEN** `POST /api/search-presets` with body `{ "name": "2024 photos", "dateFrom": "2024-01-01" }`
- **THEN** the response is `201 Created`; the DTO has `dateFrom: "2024-01-01"` and `search`, `dateTo`, `minRating` are absent or null

---

### Requirement: Users can list their saved presets

`GET /api/search-presets` SHALL return a list of all presets belonging to the authenticated user, ordered by `created_at` descending (most recently saved first). Each entry in the list SHALL include `presetId`, `name`, `createdAt`, and all decoded filter fields. Presets belonging to other users SHALL NOT appear.

#### Scenario: User sees only their own presets

- **GIVEN** user Alice has 2 presets; user Bob has 1 preset
- **WHEN** Alice calls `GET /api/search-presets`
- **THEN** the response is `200 OK`; the list contains exactly Alice's 2 presets; Bob's preset is absent

#### Scenario: Empty preset list when none saved

- **GIVEN** the authenticated user has no saved presets
- **WHEN** `GET /api/search-presets`
- **THEN** the response is `200 OK` with an empty array

---

### Requirement: Users can delete a saved preset

`DELETE /api/search-presets/{id}` SHALL delete the preset with the given ID if it belongs to the authenticated user. The response SHALL be `204 No Content`. Attempting to delete a preset belonging to another user or a non-existent preset SHALL return `404 Not Found`.

#### Scenario: Delete an existing preset

- **GIVEN** preset 7 belongs to the authenticated user
- **WHEN** `DELETE /api/search-presets/7`
- **THEN** the response is `204 No Content`; `GET /api/search-presets` no longer includes preset 7

#### Scenario: Delete a preset belonging to another user returns 404

- **GIVEN** preset 8 belongs to user Bob; Alice is authenticated
- **WHEN** Alice calls `DELETE /api/search-presets/8`
- **THEN** the response is `404 Not Found`

#### Scenario: Delete a non-existent preset returns 404

- **GIVEN** no preset with ID 9999 exists
- **WHEN** `DELETE /api/search-presets/9999`
- **THEN** the response is `404 Not Found`

---

### Requirement: The gallery filter toolbar includes a preset selector dropdown

The gallery filter toolbar SHALL include a `MatSelect` dropdown listing the authenticated user's saved presets by name. Selecting a preset from the dropdown SHALL populate all filter fields (`searchTerm`, `dateFrom`, `dateTo`, `minRating`) with the preset's stored values and trigger a gallery reload from page 0.

#### Scenario: Selecting a preset restores all filter fields

- **GIVEN** the gallery has no active filters and the dropdown shows preset "Vacation 3-star" (`search="vacation"`, `minRating=3`)
- **WHEN** the user selects "Vacation 3-star" from the dropdown
- **THEN** the search input shows "vacation"; the minimum-rating selector shows 3 filled stars; `GET /api/assets` is called with `search=vacation&minRating=3`

#### Scenario: Selecting a preset with a date range restores date fields

- **GIVEN** preset "2024 photos" has `dateFrom: "2024-01-01"` and `dateTo: "2024-12-31"`
- **WHEN** the user selects "2024 photos" from the dropdown
- **THEN** the date-from picker shows 2024-01-01; the date-to picker shows 2024-12-31; `GET /api/assets` is called with the date range parameters

---

### Requirement: The gallery filter toolbar includes a "Save as preset" button

A bookmark-add icon button in the gallery filter toolbar SHALL open a dialog prompting for a preset name. Confirming with a non-empty name SHALL call `POST /api/search-presets` with the current filter values. On success a snack bar SHALL show "Preset saved" and the new preset SHALL appear immediately in the dropdown.

#### Scenario: Save current filters via the toolbar button

- **GIVEN** the gallery filter has `search="birthday"` and `dateFrom="2024-06-01"`
- **WHEN** the user clicks the save-preset button, types "Birthday 2024", and confirms
- **THEN** `POST /api/search-presets` is called with `{ "name": "Birthday 2024", "search": "birthday", "dateFrom": "2024-06-01" }`; "Preset saved" snack bar appears; the dropdown lists "Birthday 2024"

#### Scenario: Cancel save dialog leaves presets unchanged

- **GIVEN** the save-preset dialog is open
- **WHEN** the user cancels the dialog
- **THEN** no API call is made; the preset list is unchanged

---

### Requirement: Presets can be deleted from the dropdown

Each entry in the preset dropdown SHALL include a close/delete icon. Clicking the delete icon SHALL call `DELETE /api/search-presets/{id}`, remove the entry from the dropdown, and show a "Preset deleted" snack bar. The currently selected folder and filter state SHALL remain unchanged.

#### Scenario: Delete a preset from the dropdown

- **GIVEN** the dropdown shows presets "Vacation 3-star" and "2024 photos"; "Vacation 3-star" is selected
- **WHEN** the user clicks the delete icon next to "2024 photos"
- **THEN** `DELETE /api/search-presets/{id}` is called for "2024 photos"; "2024 photos" is removed from the dropdown; the active filter state (still "Vacation 3-star") is unchanged; "Preset deleted" snack bar appears

---

### Requirement: Preset endpoints require authentication

All `/api/search-presets/**` endpoints SHALL require a valid JWT cookie. Unauthenticated requests SHALL receive `401 Unauthorized`.

#### Scenario: Unauthenticated access to search presets

- **GIVEN** no valid JWT cookie is present
- **WHEN** `GET /api/search-presets` is called
- **THEN** the response is `401 Unauthorized`
