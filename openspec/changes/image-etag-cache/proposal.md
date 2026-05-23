## Why

The `GET /api/assets/{id}/image` endpoint serves full-size image files with no HTTP caching headers. Every time a user opens the viewer, the browser re-downloads the complete image regardless of whether the content has changed. The `Asset` entity already stores a SHA-256 `hash` of the image content, making it trivial to implement conditional caching via `ETag` without any schema change.

## What Changes

- Add `ETag: "<sha256hash>"` and `Cache-Control: private, max-age=3600` to the `GET /api/assets/{id}/image` response
- Handle `If-None-Match` request header: return `304 Not Modified` when the ETag matches, avoiding a full file read
- Add unit tests covering both the 200 (new content) and 304 (cached) code paths

## Capabilities

### New Capabilities

- `image-etag-cache`: The full-size image endpoint returns an `ETag` derived from the asset's stored SHA-256 hash and supports conditional `If-None-Match` requests, enabling `304 Not Modified` responses.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — add ETag and Cache-Control logic to the image endpoint
- `JPPhotoManagerWeb/backend/src/test/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetControllerTest.java` — add 200+ETag and 304 tests
