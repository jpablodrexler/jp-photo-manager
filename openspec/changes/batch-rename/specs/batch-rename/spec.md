# batch-rename

Pattern-based batch rename for multi-selected assets in the gallery. The user selects one or more assets, opens the Batch Rename Dialog, enters a pattern using tokens `{date:yyyy-MM-dd}`, `{index:03d}`, `{original}`, and `{ext}`, reviews a live preview table of old → new filenames, and confirms to apply the rename. The backend renames each file on disk and updates `file_name` in the `assets` table. No Flyway migration is required.

---

## ADDED Requirements

### Requirement: Gallery selection menu exposes a Rename action
When one or more assets are selected in the gallery thumbnail list, the selection actions menu SHALL include a **Rename selected…** item. The item SHALL be absent (or disabled) when no assets are selected or when the gallery is in viewer or slideshow mode. Clicking the item SHALL open the Batch Rename Dialog.

#### Scenario: Rename action appears when assets are selected
- **WHEN** the user selects one or more assets in the gallery
- **THEN** the selection actions menu contains a "Rename selected…" item

#### Scenario: Rename action is absent when no assets are selected
- **WHEN** no assets are selected in the gallery
- **THEN** the "Rename selected…" item is not present in the actions menu

#### Scenario: Rename action not shown in viewer or slideshow mode
- **WHEN** the user is in viewer or slideshow mode
- **THEN** the "Rename selected…" item is not visible

---

### Requirement: Batch Rename Dialog accepts a pattern and shows a live preview
Clicking **Rename selected…** SHALL open a modal dialog. The dialog SHALL contain a text input for the rename pattern, a description of available tokens, and a `MatTable` showing the old filename and the resolved new filename for every selected asset. The preview table SHALL update within 400 ms of the user stopping typing. The dialog SHALL have an **Apply** button (disabled until the pattern is non-empty and the preview contains no errors) and a **Cancel** button.

#### Scenario: Dialog opens with empty pattern and empty preview
- **WHEN** the user clicks "Rename selected…"
- **THEN** the dialog opens; the pattern input is empty; the preview table shows each selected asset's current filename in the "Old name" column and an empty or placeholder value in the "New name" column

#### Scenario: Live preview populates as the user types a valid pattern
- **WHEN** the user types `{date:yyyy-MM-dd}_{index:03d}.{ext}` in the pattern input
- **THEN** within 400 ms the "New name" column shows the resolved filename for each asset (e.g., `2024-06-15_001.jpg`)

#### Scenario: Apply button disabled when pattern is empty
- **WHEN** the pattern input is empty
- **THEN** the Apply button is disabled

#### Scenario: Apply button enabled when preview is valid
- **WHEN** the pattern resolves to a unique, non-empty filename for every selected asset and no collision errors are reported
- **THEN** the Apply button is enabled

#### Scenario: Apply button disabled when preview reports a collision
- **GIVEN** two or more selected assets share the same file creation date and the pattern is `{date:yyyy-MM-dd}.{ext}`
- **WHEN** the live preview is fetched
- **THEN** the preview table shows an error indicator for the colliding rows and the Apply button is disabled

#### Scenario: Cancel closes the dialog without renaming
- **WHEN** the user clicks Cancel or presses Escape
- **THEN** the dialog closes; no rename request is made; all asset filenames remain unchanged

---

### Requirement: Pattern tokens resolve to correct values
The system SHALL support the following pattern tokens:

| Token | Resolved value |
|---|---|
| `{date:FORMAT}` | File creation date formatted with `FORMAT` (Java `DateTimeFormatter` pattern) |
| `{index:PAD}` | 1-based position of the asset in the selection, zero-padded to the width specified by `PAD` (e.g., `03d` → `001`) |
| `{original}` | Original filename without extension |
| `{ext}` | Lowercase file extension without the leading dot |

An unknown token SHALL be returned as a literal string in the resolved name.

#### Scenario: `{date:yyyy-MM-dd}` resolves to the asset creation date
- **GIVEN** an asset with `fileCreationDateTime = 2024-06-15T10:30:00`
- **WHEN** the pattern `{date:yyyy-MM-dd}_photo.{ext}` is previewed
- **THEN** the new name resolves to `2024-06-15_photo.jpg` (assuming extension `jpg`)

#### Scenario: `{index:03d}` resolves to the 1-based padded position
- **GIVEN** three selected assets ordered by their position in the preview list
- **WHEN** the pattern `photo_{index:03d}.{ext}` is previewed
- **THEN** the new names resolve to `photo_001.jpg`, `photo_002.jpg`, `photo_003.jpg`

#### Scenario: `{original}` resolves to the base filename without extension
- **GIVEN** an asset with `fileName = "IMG_4587.jpg"`
- **WHEN** the pattern `{original}_edited.{ext}` is previewed
- **THEN** the new name resolves to `IMG_4587_edited.jpg`

#### Scenario: `{ext}` resolves to the lowercase extension without dot
- **GIVEN** an asset with `fileName = "DSC001.JPG"`
- **WHEN** the pattern `scan_{index:01d}.{ext}` is previewed
- **THEN** the extension token resolves to `jpg` (lowercase)

#### Scenario: Invalid `{date:...}` format string returns 400
- **WHEN** `POST /api/assets/rename` is called with `pattern = "{date:INVALID!!!}"` and `applied = false`
- **THEN** the response is `400 Bad Request`

---

### Requirement: Backend rename endpoint supports preview and apply modes
`POST /api/assets/rename` SHALL accept `{ assetIds: number[], pattern: string, applied: boolean }` and return `{ previews: [{ assetId, oldName, newName }], applied: boolean }`. When `applied` is `false` the endpoint SHALL resolve the pattern for each asset and return the preview list without modifying any file or DB record. When `applied` is `true` the endpoint SHALL rename each file on disk and update `file_name` in the `assets` table, then return the same preview list with `applied: true`.

#### Scenario: Preview-only call returns resolved names without renaming
- **WHEN** `POST /api/assets/rename` is called with `applied: false`, valid `assetIds`, and a valid pattern
- **THEN** the response is `200 OK` with `applied: false` and a `previews` array containing one entry per asset with `oldName` and `newName` populated; no file on disk is modified

#### Scenario: Apply call renames files and updates the DB
- **WHEN** `POST /api/assets/rename` is called with `applied: true`, valid `assetIds`, and a valid pattern
- **THEN** each file is renamed on disk to its resolved new name; the `file_name` column is updated in the `assets` table; the response is `200 OK` with `applied: true` and the `previews` array

#### Scenario: Collision within the batch returns 400
- **GIVEN** two assets in the batch would resolve to the same new filename
- **WHEN** `POST /api/assets/rename` is called with `applied: false` or `applied: true`
- **THEN** the response is `400 Bad Request` with error code `ASSET_NAME_COLLISION`; no files are renamed

#### Scenario: Collision with an existing asset in the folder returns 400
- **GIVEN** the resolved new name of an asset matches the `file_name` of another asset already in the same folder (not in the batch)
- **WHEN** `POST /api/assets/rename` is called with `applied: true`
- **THEN** the response is `400 Bad Request` with error code `ASSET_NAME_COLLISION`; no files are renamed

#### Scenario: Empty asset ID list returns 400
- **WHEN** `POST /api/assets/rename` is called with `assetIds: []`
- **THEN** the response is `400 Bad Request`

#### Scenario: Pattern containing no tokens is valid
- **WHEN** `POST /api/assets/rename` is called with pattern `"vacation"` and a single asset
- **THEN** the response is `200 OK`; the preview shows `newName = "vacation.jpg"` (the extension of the original file is appended if `{ext}` is absent)

---

### Requirement: Applying the rename updates the gallery
After the user confirms the rename in the Batch Rename Dialog, the system SHALL close the dialog, reload the current gallery folder's asset list, clear the selection, and display a confirmation snackbar. On failure (e.g., collision error from the backend) an error snackbar SHALL be shown and the gallery state SHALL remain unchanged.

#### Scenario: Successful rename reloads the gallery
- **WHEN** the user clicks Apply in the Batch Rename Dialog and the backend responds with `200 OK`
- **THEN** the dialog closes; the gallery reloads; the selection is cleared; a snackbar reads "Renamed N asset(s)"

#### Scenario: Failed rename shows error snackbar
- **WHEN** `POST /api/assets/rename` returns `400 Bad Request`
- **THEN** the dialog closes; an error snackbar reads "Rename failed: [reason]"; the gallery state is unchanged
