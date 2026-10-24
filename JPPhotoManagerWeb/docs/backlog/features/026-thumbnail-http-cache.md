# Feature 26 — thumbnail-http-cache

**As a** photo library owner, **I want** thumbnails cached permanently by the browser, **so that** repeat gallery loads do not re-fetch every thumbnail.

## Acceptance Criteria
- AC1: Given a thumbnail request, when it succeeds, then the response carries Cache-Control: public, max-age=31536000, immutable.
- AC2: Given any asset content, when the thumbnail is served, then the header is present.
- AC3: Given an unknown asset, when the thumbnail returns 404, then it carries no cache header.

## Technical notes

Add `Cache-Control: public, max-age=31536000, immutable` to the `GET /api/assets/{id}/thumbnail` response; thumbnails are content-addressed by asset ID and never mutated once written, making them safe for permanent browser and CDN caching; currently every gallery load re-fetches every thumbnail from disk with no cache headers set

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
