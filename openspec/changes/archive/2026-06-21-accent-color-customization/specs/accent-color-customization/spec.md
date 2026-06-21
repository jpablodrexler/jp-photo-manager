# accent-color-customization

Specifies the accent colour personalisation feature: a predefined palette of eight colours the user can apply to the top bar, persisted to `localStorage`, reflected immediately on the `--accent-color` CSS variable and the PWA `<meta name="theme-color">` tag.

---

## ADDED Requirements

### Requirement: The top bar background colour is driven by the `--accent-color` CSS variable

The `.app-toolbar` element SHALL use `var(--accent-color)` as its `background-color`. The hardcoded value `#2e7d32` SHALL NOT appear in `app.component.scss`. Both `html.theme-dark` and `html.theme-light` SHALL declare `--accent-color: #2e7d32` as the default, so the visual appearance is unchanged when no custom colour has been selected.

#### Scenario: Default colour renders correctly on first load (no stored preference)

- **GIVEN** `localStorage` contains no value for `photomanager_accent_color`
- **WHEN** the application loads
- **THEN** the top bar displays with background colour `#2e7d32` (Forest Green)

#### Scenario: Stored colour is applied on load

- **GIVEN** `localStorage` contains `photomanager_accent_color = '#1565c0'`
- **WHEN** the application loads and `ThemeService.init()` runs
- **THEN** `document.documentElement.style` has `--accent-color` set to `'#1565c0'` and the top bar renders in Ocean Blue

---

### Requirement: `ThemeService.setAccentColor()` applies the colour, updates the meta tag, and persists

Calling `setAccentColor(color)` SHALL atomically: set `--accent-color` on `document.documentElement` via `style.setProperty`, update `<meta name="theme-color">` content to `color`, write `color` to `localStorage` under key `photomanager_accent_color`, and emit `color` on `accentColor$`.

#### Scenario: CSS variable is updated immediately

- **GIVEN** the application is running
- **WHEN** `themeService.setAccentColor('#6a1b9a')` is called
- **THEN** `document.documentElement.style.getPropertyValue('--accent-color')` returns `'#6a1b9a'`

#### Scenario: PWA meta tag is updated

- **GIVEN** `<meta name="theme-color" content="#2e7d32">` is present in the document
- **WHEN** `themeService.setAccentColor('#6a1b9a')` is called
- **THEN** the meta tag's `content` attribute equals `'#6a1b9a'`

#### Scenario: Preference is persisted to localStorage

- **GIVEN** the application is running
- **WHEN** `themeService.setAccentColor('#00695c')` is called
- **THEN** `localStorage.getItem('photomanager_accent_color')` returns `'#00695c'`

#### Scenario: `accentColor$` emits the new colour

- **GIVEN** a subscriber is listening to `themeService.accentColor$`
- **WHEN** `themeService.setAccentColor('#bf360c')` is called
- **THEN** the subscriber receives `'#bf360c'`

---

### Requirement: `ThemeService.init()` restores a previously saved accent colour

During initialisation `ThemeService.init()` SHALL read `localStorage.getItem('photomanager_accent_color')` and, if a value is present, call `setAccentColor()` with that value before the first frame is rendered. If no value is present the CSS variable SHALL remain at the stylesheet-level default (`#2e7d32`) without an inline style override.

#### Scenario: Saved colour is restored on init

- **GIVEN** `localStorage` contains `photomanager_accent_color = '#283593'`
- **WHEN** `themeService.init()` is called
- **THEN** `document.documentElement.style.getPropertyValue('--accent-color')` returns `'#283593'`

#### Scenario: No inline override when no colour is saved

- **GIVEN** `localStorage` has no `photomanager_accent_color` key
- **WHEN** `themeService.init()` is called
- **THEN** `document.documentElement.style.getPropertyValue('--accent-color')` returns `''` (empty string — the stylesheet default applies)

---

### Requirement: `AccentColorPickerComponent` renders exactly eight colour swatches

The component SHALL render one circular `<button class="swatch">` per palette entry. The palette SHALL contain exactly eight entries: Forest Green (`#2e7d32`), Ocean Blue (`#1565c0`), Deep Purple (`#6a1b9a`), Teal (`#00695c`), Rust Orange (`#bf360c`), Slate Grey (`#37474f`), Crimson Red (`#b71c1c`), and Indigo (`#283593`). Each button SHALL have an `aria-label` equal to the colour's human-readable label.

#### Scenario: Eight swatches are rendered

- **GIVEN** `AccentColorPickerComponent` is mounted
- **WHEN** the template renders
- **THEN** exactly 8 elements with class `swatch` are present in the DOM

#### Scenario: Each swatch has the correct background colour

- **GIVEN** `AccentColorPickerComponent` is mounted
- **WHEN** the template renders
- **THEN** the first swatch has `background-color: rgb(46, 125, 50)` and the second swatch has `background-color: rgb(21, 101, 192)`

#### Scenario: Each swatch has an accessible aria-label

- **GIVEN** `AccentColorPickerComponent` is mounted
- **WHEN** the template renders
- **THEN** the first swatch has `aria-label="Forest Green"` and the eighth has `aria-label="Indigo"`

---

### Requirement: The active swatch is visually marked and others are not

The swatch whose `value` matches the current `ThemeService.accentColor$` emission SHALL have the CSS class `active` applied. All other swatches SHALL NOT have this class. Only the active swatch SHALL display the check-mark icon.

#### Scenario: Active swatch shows a check icon

- **GIVEN** `ThemeService.accentColor$` emits `'#2e7d32'`
- **WHEN** `AccentColorPickerComponent` renders
- **THEN** the Forest Green swatch has class `active` and its `.check-icon` is visible
- **AND** no other swatch has class `active`

#### Scenario: Active swatch updates when the colour changes

- **GIVEN** `AccentColorPickerComponent` is mounted and Ocean Blue (`#1565c0`) is the active colour
- **WHEN** the user clicks the Deep Purple swatch (`#6a1b9a`)
- **THEN** the Deep Purple swatch gains class `active` and the Ocean Blue swatch loses it

---

### Requirement: Clicking a swatch calls `ThemeService.setAccentColor()` with the swatch's colour value

Clicking any swatch button SHALL invoke the component's `select(color)` method, which SHALL call `this.themeService.setAccentColor(color)`. No other side effects SHALL occur from a swatch click.

#### Scenario: Click delegates to ThemeService

- **GIVEN** `AccentColorPickerComponent` is mounted with a spy on `ThemeService.setAccentColor`
- **WHEN** the user clicks the Ocean Blue swatch
- **THEN** `themeService.setAccentColor` is called exactly once with argument `'#1565c0'`

#### Scenario: Clicking the already-active swatch calls setAccentColor again

- **GIVEN** Forest Green is the current accent colour
- **WHEN** the user clicks the Forest Green swatch
- **THEN** `themeService.setAccentColor` is called with `'#2e7d32'` (idempotent — no error)

---

### Requirement: The colour picker is accessible from the desktop toolbar via a palette icon button

On desktop (`!isMobile`), a `<button mat-icon-button>` with a `palette` Material Icon and `aria-label="Change accent color"` SHALL appear in the toolbar. Clicking it SHALL open a `MatMenu` containing `<app-accent-color-picker>`. The button SHALL be positioned immediately before the existing light/dark toggle button.

#### Scenario: Palette button is visible on desktop

- **GIVEN** the user is authenticated and the viewport is desktop width
- **WHEN** the top bar renders
- **THEN** a button with aria-label "Change accent color" and icon `palette` is visible in the toolbar

#### Scenario: Clicking the palette button opens the colour picker menu

- **GIVEN** the palette icon button is visible in the toolbar
- **WHEN** the user clicks it
- **THEN** a `MatMenu` overlay appears containing the eight colour swatches

---

### Requirement: The colour picker is accessible from the mobile hamburger menu

On mobile (`isMobile`), the accent colour picker swatch row SHALL appear inside `<mat-menu #navMenu>` immediately after the existing light/dark toggle menu item. It SHALL be rendered as an inline `<div>` (not a `<button mat-menu-item>`) so the swatches retain their circular appearance.

#### Scenario: Swatch row appears in the mobile menu

- **GIVEN** the user is authenticated and the viewport is mobile width
- **WHEN** the user opens the hamburger navigation menu
- **THEN** a row of eight coloured circular buttons is visible below the theme toggle item

#### Scenario: Tapping a swatch in the mobile menu applies the colour

- **GIVEN** the mobile navigation menu is open
- **WHEN** the user taps the Teal swatch
- **THEN** `themeService.setAccentColor('#00695c')` is called and the top bar background changes to Teal

---

### Requirement: Accent colour changes are independent of light/dark theme switches

Toggling between light and dark mode SHALL NOT reset the accent colour. The `--accent-color` CSS variable is set via an inline style on `<html>` and is not removed or overwritten by `ThemeService.applyTheme()`.

#### Scenario: Accent colour persists across theme toggle

- **GIVEN** the user has set the accent colour to Indigo (`#283593`)
- **WHEN** the user toggles from dark to light mode
- **THEN** the top bar background remains Indigo (`#283593`)

#### Scenario: Light/dark toggle does not overwrite localStorage accent key

- **GIVEN** `localStorage` contains `photomanager_accent_color = '#283593'`
- **WHEN** `themeService.toggle()` is called
- **THEN** `localStorage.getItem('photomanager_accent_color')` still returns `'#283593'`
