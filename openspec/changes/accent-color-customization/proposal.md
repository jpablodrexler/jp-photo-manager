## Why

The application's top bar is permanently locked to a single shade of dark green (`#2e7d32`). Users who prefer a different brand colour, or who find the green clashes with their desktop environment, have no recourse: the value is hardcoded in `app.component.scss` and repeated in the PWA `theme-color` meta tag. The existing light/dark toggle already signals that the team values visual personalisation; extending that to the accent colour is the natural next step.

## What Changes

- **CSS variable `--accent-color`:** Introduced in both `html.theme-dark` and `html.theme-light` blocks in `styles.scss` with a default of `#2e7d32`. The hardcoded value in `app.component.scss` is replaced with `var(--accent-color)`.
- **`ThemeService` extension:** Two new methods — `setAccentColor(color: string)` and `accentColor$: Observable<string>` — manage the active accent colour. The service writes the chosen value to `document.documentElement.style.setProperty('--accent-color', color)`, updates the PWA `<meta name="theme-color">` tag, and persists the selection to `localStorage` under the key `photomanager_accent_color`.
- **`AccentColorPickerComponent`:** A new standalone component in `shared/components/accent-color-picker/` that renders a row of eight circular colour swatches. Clicking a swatch calls `ThemeService.setAccentColor()`. The active swatch is marked with a check-mark icon.
- **Toolbar integration:** The colour picker is added to the desktop toolbar immediately to the left of the existing light/dark toggle icon button. On mobile it appears as a new menu item in the hamburger menu, opening a bottom-sheet or inline swatch row within the menu.

### Predefined Palette (8 colours)

| Label | Hex |
|---|---|
| Forest Green (default) | `#2e7d32` |
| Ocean Blue | `#1565c0` |
| Deep Purple | `#6a1b9a` |
| Teal | `#00695c` |
| Rust Orange | `#bf360c` |
| Slate Grey | `#37474f` |
| Crimson Red | `#b71c1c` |
| Indigo | `#283593` |

## Capabilities

### New Capabilities

- `accent-color-customization`: Authenticated users can choose one of eight predefined accent colours for the top bar. The selection persists across sessions via `localStorage` and is applied immediately without a page reload.

### Modified Capabilities

- `theme-management`: `ThemeService` now manages both the light/dark mode and the accent colour. The accent colour is initialised from `localStorage` on `init()` alongside the existing theme mode.

## Impact

- `JPPhotoManagerWeb/frontend/src/styles.scss` — add `--accent-color: #2e7d32` to both `html.theme-dark` and `html.theme-light` blocks.
- `JPPhotoManagerWeb/frontend/src/app/app.component.scss` — replace `background-color: #2e7d32 !important` with `background-color: var(--accent-color) !important`.
- `JPPhotoManagerWeb/frontend/src/app/core/services/theme.service.ts` — add `setAccentColor()`, `accentColor$`, and accent-colour initialisation in `init()`.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/accent-color-picker.component.ts` — new standalone component.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/accent-color-picker.component.html` — swatch row template.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/accent-color-picker.component.scss` — circular swatch styles.
- `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/accent-color-picker.component.cy.ts` — Cypress component tests.
- `JPPhotoManagerWeb/frontend/src/app/core/services/theme.service.cy.ts` — Cypress unit tests for the new methods.
- `JPPhotoManagerWeb/frontend/src/app/app.component.html` — embed `<app-accent-color-picker>` in toolbar (desktop and mobile).
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — import `AccentColorPickerComponent`.
- No backend changes. No database migrations.
