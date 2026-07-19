## 1. Backend — ETag and Cache-Control on image endpoint

- [ ] 1.1 In `AssetController.getImage()` (or equivalent method), load the `Asset` entity by id to retrieve its `hash` field
- [ ] 1.2 Read the `If-None-Match` request header; if it equals `"\"" + hash + "\""`, return `ResponseEntity.status(HttpStatus.NOT_MODIFIED).build()` without reading the image file
- [ ] 1.3 If no match, read the image bytes from storage and return:
  ```java
  ResponseEntity.ok()
      .header(HttpHeaders.ETAG, "\"" + hash + "\"")
      .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
      .contentType(mediaType)
      .body(bytes)
  ```

## 2. Backend Tests

- [ ] 2.1 Add a test that calls `GET /api/assets/{id}/image` without `If-None-Match` and asserts `200 OK`, `ETag` header present, `Cache-Control: private, max-age=3600`
- [ ] 2.2 Add a test that calls `GET /api/assets/{id}/image` with a matching `If-None-Match` and asserts `304 Not Modified`, empty body, and that the storage service `readImage()` method was NOT called
- [ ] 2.3 Add a test that calls `GET /api/assets/{id}/image` with a non-matching `If-None-Match` and asserts `200 OK` with the updated `ETag`

## 3. Testing and Commit

- [ ] 3.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 3.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 3.3 Commit all changes (only after both test suites pass)
