# Feature 16 — keyboard-shortcuts

**As a** photo library owner, **I want** keyboard shortcuts for navigation, rating and deleting, with a help overlay, **so that** I work through my library quickly without the mouse.

## Acceptance Criteria
- AC1: Given focus is not in a text field, when I press G, A or D, then the app navigates to the gallery, albums or duplicates from any route; pressing G on the gallery does nothing.
- AC2: Given the viewer, when I press 1 to 5, then the current asset is rated; pressing the same digit again clears the rating.
- AC3: Given thumbnails mode, when exactly one asset is selected, then digits rate it; with none or several selected, they are ignored.
- AC4: Given selected assets, when I press Delete, then they are soft-deleted; with none selected it does nothing.
- AC5: Given the search box is not focused, when I press /, then it takes focus.
- AC6: Given focus is in a text input or chip autocomplete, when I press any shortcut, then it is suppressed.
- AC7: Given I press ?, when on the gallery or home route, then a help overlay lists all bindings and closes with Escape.
- AC8: Given the service, when it starts and is destroyed, then it subscribes only after init and tears down its subscription.

## Technical notes

Global `KeyboardService` mapping shortcuts (`G` gallery, `A` albums, `D` duplicates, `1–5` rating, `Del` soft-delete, `/` search focus); `?` overlay listing all bindings

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
