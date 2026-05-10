## ADDED Requirements

### Requirement: User can add a tag to an asset
The system SHALL allow a user to assign a tag (a non-empty, case-insensitive keyword) to an asset. If the tag name does not yet exist in the system it SHALL be created. Tag names SHALL be normalized to lowercase before storage. Assigning a tag that the asset already has SHALL be idempotent (no error, no duplicate).

#### Scenario: New tag assigned to asset
- **WHEN** `POST /api/assets/42/tags` is called with body `{ "name": "Vacation" }`
- **THEN** the response is `201 Created`; the tag `"vacation"` is stored and linked to asset 42

#### Scenario: Existing tag assigned to a second asset
- **WHEN** `POST /api/assets/99/tags` is called with body `{ "name": "vacation" }` and the tag `"vacation"` already exists
- **THEN** the response is `201 Created`; no duplicate tag row is created; asset 99 is now linked to the existing `"vacation"` tag

#### Scenario: Duplicate assignment is idempotent
- **WHEN** `POST /api/assets/42/tags` is called with `{ "name": "vacation" }` and asset 42 already has that tag
- **THEN** the response is `200 OK` or `201 Created`; the system state is unchanged

#### Scenario: Empty tag name is rejected
- **WHEN** `POST /api/assets/42/tags` is called with `{ "name": "" }`
- **THEN** the response is `400 Bad Request`

---

### Requirement: User can remove a tag from an asset
The system SHALL allow a user to remove a tag assignment from an asset. If the removed assignment was the last one for that tag, the tag itself SHALL be deleted (no orphan tags). Removing a tag not assigned to the asset SHALL return a not-found response.

#### Scenario: Tag removed from asset
- **WHEN** `DELETE /api/assets/42/tags?name=vacation` is called and asset 42 has the tag `"vacation"`
- **THEN** the response is `204 No Content`; the `asset_tags` row is deleted

#### Scenario: Orphan tag is cleaned up
- **WHEN** `DELETE /api/assets/42/tags?name=rare-tag` is called and asset 42 is the only asset with `"rare-tag"`
- **THEN** the response is `204 No Content`; the `rare-tag` row is also deleted from the `tags` table

#### Scenario: Tag still used by other assets is not deleted
- **WHEN** `DELETE /api/assets/42/tags?name=vacation` is called and another asset also has `"vacation"`
- **THEN** the response is `204 No Content`; the `"vacation"` tag row remains in the `tags` table

#### Scenario: Removing a tag not assigned to the asset returns 404
- **WHEN** `DELETE /api/assets/42/tags?name=nonexistent` is called and asset 42 does not have that tag
- **THEN** the response is `404 Not Found`

---

### Requirement: System provides tag autocomplete
The system SHALL expose `GET /api/tags?q=<prefix>` that returns up to 20 tag names containing the query string (case-insensitive substring match). When `q` is empty or absent, the endpoint SHALL return up to 20 most recently created tags.

#### Scenario: Autocomplete returns matching tags
- **WHEN** `GET /api/tags?q=vac` is called and tags `"vacation"`, `"vaccinated"`, `"work"` exist
- **THEN** the response contains `["vacation", "vaccinated"]`; `"work"` is absent

#### Scenario: Empty query returns recent tags
- **WHEN** `GET /api/tags` is called with no `q` parameter and 5 tags exist
- **THEN** the response contains all 5 tag names

#### Scenario: Result is capped at 20
- **WHEN** `GET /api/tags?q=a` is called and 30 tags match
- **THEN** the response contains exactly 20 tag names

---

### Requirement: Asset response includes its tags
The `AssetDto` returned by `GET /api/assets` and related endpoints SHALL include a `tags` field containing the list of tag names assigned to that asset. If the asset has no tags the field SHALL be an empty list.

#### Scenario: Asset with tags includes tag list in response
- **WHEN** `GET /api/assets?folderPath=/photos&page=0` is called and one asset has tags `["vacation", "family"]`
- **THEN** that asset's DTO contains `"tags": ["vacation", "family"]`

#### Scenario: Asset with no tags returns empty list
- **WHEN** an asset has no tags assigned
- **THEN** its DTO contains `"tags": []`

---

### Requirement: User can view and edit tags for an asset in the gallery
The gallery SHALL display the tags of the currently viewed or selected asset in the EXIF/info panel as editable chip components. The user SHALL be able to add new tags via a chip input with autocomplete and remove existing tags by clicking the remove icon on a chip.

#### Scenario: Tags shown in EXIF panel
- **WHEN** the user opens the viewer for an asset that has tags `["vacation", "2024"]`
- **THEN** the EXIF panel displays two chips labeled `"vacation"` and `"2024"`

#### Scenario: User adds a tag via chip input
- **WHEN** the user types `"family"` in the tag chip input and presses Enter
- **THEN** `POST /api/assets/{id}/tags` is called; the new chip appears in the panel

#### Scenario: User removes a tag via chip
- **WHEN** the user clicks the × icon on the `"vacation"` chip
- **THEN** `DELETE /api/assets/{id}/tags?name=vacation` is called; the chip is removed from the panel

#### Scenario: Autocomplete suggestions appear while typing
- **WHEN** the user types `"va"` in the tag chip input
- **THEN** a dropdown shows matching tag suggestions fetched from `GET /api/tags?q=va`

---

### Requirement: User can bulk-tag selected assets
When one or more assets are selected in the gallery, the actions toolbar SHALL offer a "Tag selected" action that opens a dialog. The dialog SHALL allow the user to add tags to all selected assets and remove tags that all selected assets share. Changes SHALL be applied to all selected assets in a single backend call.

#### Scenario: Bulk-add tag to selected assets
- **WHEN** 3 assets are selected and the user adds tag `"to-print"` in the bulk-tag dialog
- **THEN** `POST /api/assets/tags/bulk` is called with the selected asset IDs and tag `"to-print"`; all 3 assets are tagged

#### Scenario: Bulk-remove common tag from selected assets
- **WHEN** 3 assets all share the tag `"draft"` and the user removes it in the bulk-tag dialog
- **THEN** `DELETE /api/assets/tags/bulk` is called; the tag is removed from all 3 assets
