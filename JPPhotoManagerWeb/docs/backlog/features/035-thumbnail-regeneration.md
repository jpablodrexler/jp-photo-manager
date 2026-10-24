# Feature 35 — thumbnail-regeneration

**As a** administrator, **I want** to regenerate thumbnails for the whole library or one folder, **so that** corrupted, resized or wrongly rotated thumbnails are fixed in one action.

## Acceptance Criteria
- AC1: Given no folder path, when I call the endpoint, then all thumbnails are deleted and regenerated.
- AC2: Given a folder path, when I call the endpoint, then only that folder's thumbnails are regenerated.
- AC3: Given one thumbnail fails, when the operation runs, then it continues with the rest.
- AC4: Given a VIEWER, when they call the endpoint, then it is refused.
- AC5: Given the operation runs, when each asset is processed, then a progress event is streamed.

## Technical notes

Add `POST /api/assets/regenerate-thumbnails` (optionally scoped by `folderPath` query param) that deletes existing `.bin` thumbnail files via `ThumbnailPort` and re-generates them through `StoragePort`; covers corrupted thumbnails, thumbnail size changes, and retroactive EXIF-rotation correction; reuses existing infrastructure adapters with no schema change

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
