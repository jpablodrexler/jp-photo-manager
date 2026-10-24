# Feature 37 — asset-description

**As a** photo library owner, **I want** to add a description to a photo, **so that** I can note who or what is in it and find it later.

## Acceptance Criteria
- AC1: Given the EXIF panel in the viewer, when I edit the description field and save, then it is stored via the PATCH endpoint.
- AC2: Given a description, when I send null, then it is cleared.
- AC3: Given a description longer than 2000 characters, when I save, then it is rejected.
- AC4: Given the full-text search, when it is available, then the description is part of what is searched.

## Technical notes

Add a `description` VARCHAR column to `assets` (new Flyway migration); expose `PATCH /api/assets/{id}/description`; display an editable text field in the EXIF panel in the viewer; the description field feeds directly into the `full-text-search` (#31) index once both are in place

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
