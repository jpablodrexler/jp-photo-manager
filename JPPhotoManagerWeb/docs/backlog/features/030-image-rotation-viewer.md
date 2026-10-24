# Feature 30 — image-rotation-viewer

**As a** photo library owner, **I want** portrait photos displayed upright, **so that** I do not have to tilt my head to look at them.

## Acceptance Criteria
- AC1: Given a portrait photo with a stored rotation, when the thumbnail grid renders, then it is rotated upright; a photo with no rotation has no rotation style.
- AC2: Given the viewer, when a portrait photo opens, then the full-size image is upright.
- AC3: Given the four rotation values, when they are applied, then they map to the correct degrees.

## Technical notes

Apply the `imageRotation` field already stored on `Asset` as a CSS `transform: rotate()` in `ThumbnailComponent` and the full-size viewer `<img>`; photos taken in portrait orientation are currently displayed sideways because the raw file is served without orientation correction; no backend change and no new API call are required

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
