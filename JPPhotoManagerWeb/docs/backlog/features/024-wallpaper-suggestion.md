# Feature 24 — wallpaper-suggestion

**As a** photo library owner, **I want** a wallpaper suggestion matching my screen, **so that** I find a photo that fits my desktop without cropping.

## Acceptance Criteria
- AC1: Given a new asset, when it is cataloged, then its aspect ratio is stored; the migration backfills existing rows.
- AC2: Given the screen size, when I request a suggestion, then a random non-deleted asset at least as large as the screen with an aspect ratio within ±0.02 is returned; with no match, 404.
- AC3: Given the dialog, when a match exists, then the recommended image is shown with a download button; with no match, a friendly message.

## Technical notes

Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
