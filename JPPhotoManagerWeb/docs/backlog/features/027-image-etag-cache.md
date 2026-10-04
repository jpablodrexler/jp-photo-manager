# Feature 27 — image-etag-cache

Add `ETag` (derived from the SHA-256 `hash` already stored on the `Asset` entity) and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response; enables conditional `If-None-Match` requests so the browser receives a `304 Not Modified` instead of re-downloading the full image on repeat views; currently no cache headers are set on this endpoint
