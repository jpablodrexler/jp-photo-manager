## Context

The top bar background is set by `.app-toolbar { background-color: #2e7d32 !important; }` in `app.component.scss`. There is no dynamic path between the runtime theme state and this value. Angular Material's `$green-palette` controls Material component colours (buttons, ripples, focus rings) but does not govern the toolbar background, which is overridden at the component level with `!important`.

The existing `ThemeService` already owns all theme state: it writes a class (`theme-dark` / `theme-light`) to `document.documentElement`, persists to `localStorage`, and exposes `isDark$`. Extending it to manage accent colour follows the same pattern without introducing a second service.

The PWA `<meta name="theme-color" content="#2e7d32">` in `index.html` is static; it must be updated at runtime via `document.querySelector('meta[name="theme-color"]')?.setAttribute('content', color)` each time the accent changes.

There are two existing locations where the theme toggle is surfaced: a desktop icon button in the toolbar and a mobile menu item inside `#navMenu`. The colour picker must slot into both without disrupting the existing layout.

## Goals / Non-Goals

**Goals:**

- Replace the hardcoded `#2e7d32` in `app.component.scss` with `var(--accent-color)`.
- Add `--accent-color: #2e7d32` as the default value to both `html.theme-dark` and `html.theme-light` blocks in `styles.scss`.
- Extend `ThemeService` with `setAccentColor(color: string)`, `accentColor$: Observable<string>`, and accent-colour initialisation in `init()`.
- Persist the chosen colour to `localStorage` under `photomanager_accent_color`.
- Update the PWA `<meta name="theme-color">` tag on every accent colour change.
- Create `AccentColorPickerComponent` — a standalone swatch-row component in `shared/components/accent-color-picker/`.
- Integrate the picker into the desktop toolbar and the mobile hamburger menu.
- Provide 8 fixed predefined colours; no free-form hex input.

**Non-Goals:**

- Free-form colour input (`<input type="color">` or hex text field).
- Server-side persistence of the accent colour preference.
- Changing Angular Material's primary palette at runtime (only the toolbar background colour changes).
- Applying the accent colour to any element other than the top bar (e.g. buttons, FABs).
- Animations or transitions when switching accent colour.

## Decisions

### 1. CSS custom property `--accent-color` on `:root` / `html`

**Decision:** Declare `--accent-color: #2e7d32` inside both `html.theme-dark { … }` and `html.theme-light { … }` blocks in `styles.scss`. At runtime `ThemeService.setAccentColor()` calls `document.documentElement.style.setProperty('--accent-color', color)`, which sets an inline style on `<html>` that takes precedence over the rule-level default.

**Rationale:** CSS custom properties cascade normally. Setting the property on the element itself (inline style) is the highest specificity outside `!important`, so no selector trickery is needed. The component SCSS rule `background-color: var(--accent-color) !important` replaces the old hardcoded value and honours the `!important` needed to override Angular Material's toolbar background.

**Alternative considered:** Dynamically injecting a `<style>` tag with a new rule — less clean, creates DOM noise, and risks accumulating stale rules if the colour is changed repeatedly.

### 2. Extend `ThemeService` rather than a separate `AccentColorService`

**Decision:** Add `setAccentColor()`, `accentColor$`, and the `ACCENT_STORAGE_KEY` constant to `ThemeService`.

**Rationale:** Accent colour is a presentation preference, tightly coupled to the existing theme mechanism. A second service would split responsibility unnecessarily. `ThemeService.init()` already reads `localStorage`; it reads the accent key at the same time and applies the CSS variable before the first render, avoiding a flash of the default green.

**Alternative considered:** `AccentColorService` as a separate injectable — rejected because it would require injecting it wherever the theme toggle is already injected, and callers would need to coordinate two services.

### 3. `AccentColorPickerComponent` as a standalone shared component

**Decision:** Create `shared/components/accent-color-picker/accent-color-picker.component.ts` as a standalone Angular component. It defines the palette as a `readonly` class constant:

```typescript
readonly PALETTE = [
  { label: 'Forest Green', value: '#2e7d32' },
  { label: 'Ocean Blue',   value: '#1565c0' },
  { label: 'Deep Purple',  value: '#6a1b9a' },
  { label: 'Teal',         value: '#00695c' },
  { label: 'Rust Orange',  value: '#bf360c' },
  { label: 'Slate Grey',   value: '#37474f' },
  { label: 'Crimson Red',  value: '#b71c1c' },
  { label: 'Indigo',       value: '#283593' },
];
```

It injects `ThemeService` and exposes `accentColor$` from it. Each swatch is a `<button>` with `[style.background-color]` and a `mat-icon check` rendered conditionally when the swatch colour matches the active colour.

**Rationale:** A standalone component in `shared/` is reusable in both the desktop toolbar and the mobile menu without duplication. Defining the palette inside the component keeps the data co-located with its only consumer.

**Alternative considered:** Rendering the swatches inline in `app.component.html` — rejected because it would scatter the palette definition into the root template and make testing harder.

### 4. Desktop toolbar: icon button opens a `MatMenu` overlay

**Decision:** In `app.component.html`, add a `<button mat-icon-button [matMenuTriggerFor]="colorMenu">` with a `palette` icon, placed immediately before the existing dark/light toggle button. A `<mat-menu #colorMenu>` contains `<app-accent-color-picker />`.

**Rationale:** `MatMenu` is already used for the mobile hamburger menu in the same file. Reusing it means no new overlay or dialog dependency. The menu dismisses on outside-click and on Escape automatically. The `palette` Material icon clearly signals colour selection.

**Alternative considered:** Embedding swatches directly in the toolbar (always visible) — rejected because 8 coloured circles would clutter the already-dense toolbar, particularly on smaller desktop widths.

### 5. Mobile menu: inline `AccentColorPickerComponent` inside `#navMenu`

**Decision:** Inside `<mat-menu #navMenu="matMenu">`, add a non-button `<div>` entry containing `<app-accent-color-picker />` beneath the existing theme toggle menu item.

**Rationale:** On mobile the swatches are compact enough to sit inside the drawer menu without requiring a second overlay. A `<div>` rather than a `<button mat-menu-item>` avoids the full-width ripple style and lets the swatch buttons retain their circular appearance.

**Alternative considered:** Opening a second `MatMenu` from the mobile menu — creates unnecessary nesting and requires two taps to reach the colours.

### 6. PWA `<meta name="theme-color">` updated at runtime

**Decision:** In `ThemeService.setAccentColor()`, after writing the CSS variable, call:

```typescript
document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
  ?.setAttribute('content', color);
```

**Rationale:** The browser reads `theme-color` for the OS chrome (tab strip, address bar on mobile). Updating it keeps the browser chrome in sync with the toolbar. No SSR concern exists because this is a pure client-side Angular app.

**Alternative considered:** Removing the static meta tag from `index.html` and injecting it dynamically — equivalent in effect but more disruptive; keeping the static tag provides a sensible default before JavaScript runs.

## Risks / Trade-offs

**Accessibility: colour contrast**
→ Some palette colours (e.g. Teal `#00695c`, Slate Grey `#37474f`) may produce marginal contrast ratios against the white toolbar text at WCAG AA level. The picker does not include a contrast warning. Users are free to select any palette colour; the onus is on the palette curation. All eight colours in the proposed palette pass 4.5:1 contrast against white in manual testing.

**`!important` retained in `app.component.scss`**
→ `var(--accent-color) !important` is still needed to override Angular Material's toolbar background. This is not ideal but is unchanged from the current implementation and unavoidable without forking the Material theming setup.

**No server-side sync**
→ The colour is stored only in `localStorage`. Users who log in on a second device will see the default green until they set their preference again. Server-side sync is deferred.

**Static palette**
→ Eight colours cover common tastes but do not satisfy every user. A free-form picker is deliberately out of scope for this change and can be added as a follow-up.
