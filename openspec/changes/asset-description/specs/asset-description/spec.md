# asset-description

Users can add and edit a free-text description for each asset. The description is saved to the database and displayed in the EXIF panel.

---

## ADDED Requirements

### Requirement: Assets have an editable description field

Each asset SHALL have a `description` field (up to 2000 characters) stored in `asset_exif`. The field SHALL be nullable (no description is the default state).

#### Scenario: Description is saved via PATCH endpoint

- **GIVEN** an asset with `assetId = 42` and no description
- **WHEN** `PATCH /api/assets/42/description` is called with body `{ "description": "Sunset at the beach" }`
- **THEN** the response is `200 OK` and the description is persisted; subsequent `GET /api/assets/42/exif` returns `"description": "Sunset at the beach"`

#### Scenario: Description is cleared by sending null

- **GIVEN** an asset with description "Sunset at the beach"
- **WHEN** `PATCH /api/assets/42/description` is called with body `{ "description": null }`
- **THEN** the description is set to null and `GET /api/assets/42/exif` returns `"description": null`

#### Scenario: Description longer than 2000 characters is rejected

- **GIVEN** a description string of 2001 characters
- **WHEN** `PATCH /api/assets/42/description` is called with that body
- **THEN** the response is `400 Bad Request`

### Requirement: ExifPanelComponent displays and allows editing the description

The `ExifPanelComponent` SHALL display the `description` field as an editable `<textarea>`. Saving SHALL occur on blur.

#### Scenario: User edits description in the EXIF panel

- **GIVEN** the EXIF panel is open for an asset
- **WHEN** the user types a description in the text area and moves focus away
- **THEN** the description is saved via `PATCH /api/assets/{id}/description` and the panel reflects the saved value
