# Feature 27 — image-etag-cache

**As a** photo library owner, **I want** full-size images served with an ETag, **so that** viewing an image again does not download it again.

## Acceptance Criteria
- AC1: Given the first request, when the image is served, then the response has an ETag derived from the file hash and Cache-Control: private, max-age=3600.
- AC2: Given a matching If-None-Match, when I request the image, then 304 Not Modified is returned without reading the file.
- AC3: Given a non-matching If-None-Match, when I request the image, then the full image is returned.

## Technical notes

Add `ETag` (derived from the SHA-256 `hash` already stored on the `Asset` entity) and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response; enables conditional `If-None-Match` requests so the browser receives a `304 Not Modified` instead of re-downloading the full image on repeat views; currently no cache headers are set on this endpoint

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
