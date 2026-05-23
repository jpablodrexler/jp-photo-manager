## Why

The `GET /api/assets/{id}/thumbnail` endpoint serves JPEG thumbnails with no HTTP caching headers. Every gallery load re-requests every visible thumbnail from the server, even though thumbnails are never modified after creation. Adding `Cache-Control: public, max-age=31536000, immutable` allows browsers and CDNs to cache thumbnails indefinitely, eliminating redundant network requests on repeat visits.

## What Changes

- Add `Cache-Control: public, max-age=31536000, immutable` header to the thumbnail endpoint response in `AssetController`
- Add a unit test verifying the header is present in the response

## Capabilities

### New Capabilities

- `thumbnail-http-cache`: The thumbnail endpoint returns a permanent, immutable `Cache-Control` header enabling browser and CDN caching.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — add `Cache-Control` header to the thumbnail response
- `JPPhotoManagerWeb/backend/src/test/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetControllerTest.java` — assert header presence
