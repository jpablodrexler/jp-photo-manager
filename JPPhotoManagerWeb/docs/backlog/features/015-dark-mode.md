# Feature 15 — dark-mode

**As a** signed-in user, **I want** a dark mode that remembers my choice, **so that** the gallery is comfortable to look at in any lighting.

## Acceptance Criteria
- AC1: Given the toolbar, when I am signed in, then a toggle shows a dark-mode icon in the light theme and a light-mode icon in the dark theme; it is hidden when signed out.
- AC2: Given I press the toggle, when the theme switches, then it flips between light and dark.
- AC3: Given a stored local preference, when the app loads, then it is applied; with none, the system's light or dark preference is used.
- AC4: Given I log in, when my server preference loads, then it overrides the local one; if the request fails, then the local preference is kept.
- AC5: Given I toggle the theme, when I am signed in, then the preference is saved to the backend, creating or updating my row; a failed save does not revert the local theme; nothing is sent when signed out.
- AC6: Given the preference API, when I retrieve it, then I get my saved value or the default; an unauthenticated request is rejected.

## Technical notes

Angular Material dark palette toggle stored in `localStorage` with `prefers-color-scheme` fallback; preference persisted per user on the backend; toolbar toggle button

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
