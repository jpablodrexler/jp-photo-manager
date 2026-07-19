# keyboard-shortcuts

Specifies the global `KeyboardService` shortcut system: navigation shortcuts (`G`, `A`, `D`), gallery-scoped shortcuts (`1`–`5`/`0` for rating, `Del` for soft-delete, `/` for search focus), the `?` help overlay, and input-suppression behaviour that prevents shortcuts from firing while typing in form fields.

---

## ADDED Requirements

### Requirement: Shortcuts are suppressed when focus is inside a text-input element

The keyboard service SHALL NOT execute any shortcut when the keyboard event originates from an `<input>`, `<textarea>`, or any element that has the `contenteditable` attribute set. This suppression applies to all shortcuts without exception, including navigation, rating, delete, search-focus, and help overlay shortcuts.

#### Scenario: Navigation shortcut is suppressed while typing in the search field

- **GIVEN** the user has focus inside the gallery search `<input>`
- **WHEN** the user presses `G`
- **THEN** the application does not navigate and the character `g` is typed into the input as normal

#### Scenario: Rating shortcut is suppressed while a chip autocomplete input is focused

- **GIVEN** the user has focus inside the tag-filter `<input>` (a `[contenteditable]` or `<input>` inside a Material chip set)
- **WHEN** the user presses `3`
- **THEN** no rating action is triggered and the character `3` is typed normally

#### Scenario: Shortcut fires normally when focus is on a non-input element

- **GIVEN** focus is on the document body or a non-interactive element (e.g. a thumbnail card that has no native text input)
- **WHEN** the user presses `G`
- **THEN** the application navigates to `/gallery`

---

### Requirement: `G` key navigates to the gallery route

Pressing `G` (case-insensitive) while authenticated and not focused in a text-input SHALL navigate the application to `/gallery` using the Angular `Router`.

#### Scenario: `G` navigates to gallery from the home route

- **GIVEN** the user is authenticated and the current route is `/home`
- **AND** focus is not inside any text input
- **WHEN** the user presses `G`
- **THEN** the application navigates to `/gallery`

#### Scenario: `G` navigates to gallery from the duplicates route

- **GIVEN** the user is authenticated and the current route is `/duplicates`
- **AND** focus is not inside any text input
- **WHEN** the user presses `G`
- **THEN** the application navigates to `/gallery`

#### Scenario: `G` pressed while already on the gallery route

- **GIVEN** the user is on `/gallery`
- **AND** focus is not inside any text input
- **WHEN** the user presses `G`
- **THEN** the application remains on `/gallery` with no visible error

---

### Requirement: `A` key navigates to the albums route

Pressing `A` (case-insensitive) while authenticated and not focused in a text-input SHALL navigate the application to `/albums`.

#### Scenario: `A` navigates to albums from the gallery route

- **GIVEN** the user is authenticated and the current route is `/gallery`
- **AND** focus is not inside any text input
- **WHEN** the user presses `A`
- **THEN** the application navigates to `/albums`

#### Scenario: `A` navigates to albums from the home route

- **GIVEN** the user is authenticated and the current route is `/home`
- **AND** focus is not inside any text input
- **WHEN** the user presses `A`
- **THEN** the application navigates to `/albums`

---

### Requirement: `D` key navigates to the duplicates route

Pressing `D` (case-insensitive) while authenticated and not focused in a text-input SHALL navigate the application to `/duplicates`.

#### Scenario: `D` navigates to duplicates from the gallery route

- **GIVEN** the user is authenticated and the current route is `/gallery`
- **AND** focus is not inside any text input
- **WHEN** the user presses `D`
- **THEN** the application navigates to `/duplicates`

#### Scenario: `D` navigates to duplicates from the albums route

- **GIVEN** the user is authenticated and the current route is `/albums`
- **AND** focus is not inside any text input
- **WHEN** the user presses `D`
- **THEN** the application navigates to `/duplicates`

---

### Requirement: `1`–`5` keys rate the current or single selected asset while in the gallery

While the gallery component is mounted, pressing `1`, `2`, `3`, `4`, or `5` SHALL rate the current asset. In viewer or slideshow mode the current asset is `assets[currentViewerIndex]`. In thumbnails mode, if exactly one asset is selected, that asset is rated. If no asset is selected or more than one is selected in thumbnails mode, the keypress SHALL be ignored. Pressing a digit equal to the asset's current rating SHALL clear the rating (toggle to 0), matching the existing `rateAsset` toggle logic.

#### Scenario: Rating the current asset in viewer mode

- **GIVEN** the gallery is in viewer mode with `currentViewerIndex = 2`
- **AND** focus is not inside any text input
- **WHEN** the user presses `4`
- **THEN** `rateAsset` is called with `assets[2]` and star value `4`

#### Scenario: Rating toggled off when the same digit is pressed again

- **GIVEN** the gallery is in viewer mode and the current asset has `rating = 3`
- **AND** focus is not inside any text input
- **WHEN** the user presses `3`
- **THEN** `rateAsset` is called with star value `3`, which the existing toggle logic converts to a rating of `0`

#### Scenario: Rating a single selected asset in thumbnails mode

- **GIVEN** the gallery is in thumbnails mode and exactly one asset is selected
- **AND** focus is not inside any text input
- **WHEN** the user presses `2`
- **THEN** `rateAsset` is called with the selected asset and star value `2`

#### Scenario: Rating ignored when no asset is selected in thumbnails mode

- **GIVEN** the gallery is in thumbnails mode and no asset is selected
- **AND** focus is not inside any text input
- **WHEN** the user presses `5`
- **THEN** no `rateAsset` call is made

#### Scenario: Rating ignored when multiple assets are selected in thumbnails mode

- **GIVEN** the gallery is in thumbnails mode and two or more assets are selected
- **AND** focus is not inside any text input
- **WHEN** the user presses `1`
- **THEN** no `rateAsset` call is made

---

### Requirement: `Del` key soft-deletes selected assets while in the gallery

While the gallery component is mounted, pressing the `Delete` key SHALL trigger the same soft-delete action as the existing toolbar "Remove from catalog" button, removing the selected assets from the catalog without deleting their files on disk. If no assets are selected the keypress SHALL be ignored. The action SHALL call `deleteAssets` with `deleteFiles = false`.

#### Scenario: `Del` soft-deletes selected assets

- **GIVEN** the gallery has two assets selected
- **AND** focus is not inside any text input
- **WHEN** the user presses `Delete`
- **THEN** `assetService.deleteAssets` is called with the selected asset IDs and `deleteFiles = false`; the selection is cleared and assets are reloaded

#### Scenario: `Del` ignored when no assets are selected

- **GIVEN** the gallery has no assets selected
- **AND** focus is not inside any text input
- **WHEN** the user presses `Delete`
- **THEN** no delete action is triggered

#### Scenario: `Del` is suppressed while focus is inside a text input

- **GIVEN** the user has focus inside the gallery search `<input>`
- **WHEN** the user presses `Delete`
- **THEN** no delete action is triggered and the character at the cursor is deleted as normal

---

### Requirement: `/` key focuses the gallery search input

While the gallery component is mounted, pressing `/` SHALL focus the search `<input>` element and call `event.preventDefault()` to suppress the browser's native find-in-page shortcut. After focus is applied the search field SHALL be ready to accept text input immediately.

#### Scenario: `/` focuses the search field

- **GIVEN** the gallery is displayed and focus is on the document body
- **WHEN** the user presses `/`
- **THEN** the gallery search `<input>` has DOM focus and the browser find-in-page dialog does NOT open

#### Scenario: `/` is suppressed when the search input is already focused

- **GIVEN** the user already has focus inside the gallery search `<input>`
- **WHEN** the user presses `/`
- **THEN** the input-suppression guard fires first and the character `/` is typed into the search field; no second focus call is made

---

### Requirement: `?` key opens the keyboard shortcuts help overlay

Pressing `?` (Shift+/) from any authenticated route SHALL open a full-screen Angular Material dialog listing all registered keyboard shortcuts. The dialog SHALL group shortcuts under two headings: "Global" and "Gallery". Each row SHALL show the key or key combination, a short description, and the scope. The dialog SHALL close when the user presses `Escape` or clicks outside it.

#### Scenario: `?` opens the help overlay from the gallery route

- **GIVEN** the user is authenticated and on `/gallery`
- **AND** focus is not inside any text input
- **WHEN** the user presses `?`
- **THEN** a Material dialog opens displaying a table of keyboard shortcuts

#### Scenario: `?` opens the help overlay from the home route

- **GIVEN** the user is authenticated and on `/home`
- **AND** focus is not inside any text input
- **WHEN** the user presses `?`
- **THEN** a Material dialog opens displaying a table of keyboard shortcuts

#### Scenario: Help overlay displays all expected bindings

- **GIVEN** the help overlay is open
- **WHEN** the user views the dialog content
- **THEN** the dialog contains entries for: `G` (Navigate to Gallery), `A` (Navigate to Albums), `D` (Navigate to Duplicates), `/` (Focus search), `Del` (Remove from catalog), `1`–`5` (Rate asset), `?` (Show shortcuts), `Space` (Pause/resume slideshow), `←`/`→` (Previous/next in viewer), `Esc` (Exit slideshow)

#### Scenario: Help overlay closes with Escape

- **GIVEN** the help overlay dialog is open
- **WHEN** the user presses `Escape`
- **THEN** the dialog closes

---

### Requirement: `KeyboardService` is initialised once by `AppComponent` and cleaned up on destroy

`KeyboardService.init(router)` SHALL be called exactly once from `AppComponent.ngOnInit()`. The service SHALL subscribe to `fromEvent<KeyboardEvent>(document, 'keydown')` and keep the subscription alive for the lifetime of the application shell. When `AppComponent` is destroyed the service SHALL unsubscribe from the `document` keydown stream.

#### Scenario: Service subscribes on init

- **GIVEN** `AppComponent` has been initialised
- **WHEN** a `keydown` event fires on the document
- **THEN** `KeyboardService` processes the event

#### Scenario: Service does not subscribe before init is called

- **GIVEN** `KeyboardService` has been constructed but `init()` has not been called
- **WHEN** a `keydown` event fires on the document
- **THEN** no shortcut action is executed

#### Scenario: Subscription is torn down when the component is destroyed

- **GIVEN** `AppComponent` is destroyed (e.g. during test teardown)
- **WHEN** a `keydown` event fires on the document after destruction
- **THEN** no shortcut action is executed because the RxJS subscription has been unsubscribed
