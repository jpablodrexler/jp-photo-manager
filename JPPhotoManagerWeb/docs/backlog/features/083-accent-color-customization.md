# Feature 83 — accent-color-customization

**As a** signed-in user, **I want** to choose the accent colour of the top bar from eight presets, **so that** the app looks the way I like.

## Acceptance Criteria
- AC1: Given a first load with no stored preference, when the app renders, then the default colour is shown.
- AC2: Given a stored colour, when the app loads, then it is applied; the CSS variable updates immediately when I pick another and the PWA theme-color tag follows.
- AC3: Given the picker, when it renders, then eight swatches are shown with their colour and an accessible label, the active one marked with a check.
- AC4: Given I click a swatch, when it applies, then the theme service sets it, the preference is saved to local storage and it is restored on init.
- AC5: Given the desktop and mobile layouts, when I use them, then the picker is in the toolbar menu on desktop and inline in the hamburger menu on mobile.
- AC6: Given I toggle light and dark, when the theme changes, then the accent colour persists and the stored key is not overwritten.

## Technical notes

Allow users to pick one of eight predefined accent colours for the top bar; replaces the hardcoded `#2e7d32` in `app.component.scss` with `var(--accent-color)`; `ThemeService` gains `setAccentColor()` and `accentColor$`; choice is persisted to `localStorage` and applied instantly including updating the PWA `<meta name="theme-color">` tag; a new `AccentColorPickerComponent` (swatch row) is embedded in the desktop toolbar as a `MatMenu` behind a palette icon button, and inline in the mobile hamburger menu; predefined palette: Forest Green, Ocean Blue, Deep Purple, Teal, Rust Orange, Slate Grey, Crimson Red, Indigo; no backend changes, no schema migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
