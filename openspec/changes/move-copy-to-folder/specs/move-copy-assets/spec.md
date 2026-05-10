## ADDED Requirements

### Requirement: Gallery selection menu exposes Move and Copy actions
When one or more assets are selected in the gallery, the actions menu SHALL include a **Move to folder…** and a **Copy to folder…** item. Both items SHALL be present only in thumbnail view (not in viewer or slideshow mode). Clicking either item SHALL open the Folder Picker Dialog.

#### Scenario: Move action appears when assets are selected
- **WHEN** the user selects one or more assets in the gallery thumbnail grid
- **THEN** the actions menu contains a "Move to folder…" item

#### Scenario: Copy action appears when assets are selected
- **WHEN** the user selects one or more assets in the gallery thumbnail grid
- **THEN** the actions menu contains a "Copy to folder…" item

#### Scenario: Actions not shown in viewer mode
- **WHEN** the user is in viewer or slideshow mode
- **THEN** the Move and Copy actions are not visible in the toolbar

---

### Requirement: Folder Picker Dialog lets the user choose a destination
Clicking **Move to folder…** or **Copy to folder…** SHALL open a modal dialog that displays the folder tree (via the existing `FolderNavComponent`). The dialog title SHALL reflect the chosen mode ("Move to Folder" or "Copy to Folder"). The dialog SHALL have a confirm button and a cancel button. The confirm button SHALL be disabled until the user selects a destination folder and that folder differs from the current gallery folder.

#### Scenario: Dialog opens with correct title for Move
- **WHEN** the user clicks "Move to folder…"
- **THEN** the dialog opens with title "Move to Folder" and a confirm button labelled "Move here"

#### Scenario: Dialog opens with correct title for Copy
- **WHEN** the user clicks "Copy to folder…"
- **THEN** the dialog opens with title "Copy to Folder" and a confirm button labelled "Copy here"

#### Scenario: Confirm button disabled before folder selection
- **WHEN** the dialog opens and no folder has been selected yet
- **THEN** the confirm button is disabled

#### Scenario: Confirm button disabled when destination equals source
- **WHEN** the user selects the same folder the assets are currently in
- **THEN** the confirm button remains disabled

#### Scenario: Confirm button enabled after valid folder selection
- **WHEN** the user selects a folder different from the current gallery folder
- **THEN** the confirm button becomes enabled

#### Scenario: Cancel closes dialog without taking action
- **WHEN** the user clicks Cancel or presses Escape
- **THEN** the dialog closes; no move or copy request is made; the gallery state is unchanged

---

### Requirement: Confirming Move transfers assets to the destination folder
When the user confirms a Move, the system SHALL call `POST /api/assets/move` with `preserveOriginal: false`, the selected asset IDs, and the destination folder path. On success, the selected assets SHALL be removed from the source folder, the gallery SHALL reload to reflect the change, the selection SHALL be cleared, and a confirmation snackbar SHALL be shown. On failure, an error snackbar SHALL be shown and the gallery state SHALL remain unchanged.

#### Scenario: Move succeeds
- **WHEN** the user confirms the Move in the Folder Picker Dialog
- **THEN** `POST /api/assets/move` is called with `preserveOriginal: false`; the dialog closes; the gallery reloads; a snackbar reads "Moved N asset(s) to [folder name]"; the selection is cleared

#### Scenario: Move fails
- **WHEN** `POST /api/assets/move` returns an error response
- **THEN** the dialog closes; an error snackbar reads "Failed to move assets"; the gallery state is unchanged

#### Scenario: In-progress feedback shown during move
- **WHEN** the move request is in-flight
- **THEN** a "Moving…" snackbar with no auto-dismiss is shown; it is dismissed when the request completes

---

### Requirement: Confirming Copy duplicates assets to the destination folder
When the user confirms a Copy, the system SHALL call `POST /api/assets/move` with `preserveOriginal: true`, the selected asset IDs, and the destination folder path. On success, the originals SHALL remain in the source folder, the gallery SHALL reload, the selection SHALL be cleared, and a confirmation snackbar SHALL be shown. On failure, an error snackbar SHALL be shown.

#### Scenario: Copy succeeds
- **WHEN** the user confirms the Copy in the Folder Picker Dialog
- **THEN** `POST /api/assets/move` is called with `preserveOriginal: true`; the dialog closes; the gallery reloads; a snackbar reads "Copied N asset(s) to [folder name]"; the selection is cleared

#### Scenario: Copy fails
- **WHEN** `POST /api/assets/move` returns an error response
- **THEN** the dialog closes; an error snackbar reads "Failed to copy assets"; the gallery state is unchanged

#### Scenario: Original assets remain after Copy
- **WHEN** the Copy completes successfully
- **THEN** the assets that were copied are still present in the source folder after the gallery reloads
