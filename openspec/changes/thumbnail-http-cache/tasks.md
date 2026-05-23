## 1. Backend — Add Cache-Control header

- [ ] 1.1 In `AssetController.getThumbnail()` (or equivalent method), add `.header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")` to the `ResponseEntity` builder
- [ ] 1.2 Verify the method returns a `ResponseEntity<byte[]>` with `Content-Type: image/jpeg` and the new header

## 2. Backend Tests

- [ ] 2.1 In `AssetControllerTest` (using `@WebMvcTest`), add a test that calls `GET /api/assets/{id}/thumbnail` and asserts the `Cache-Control` header equals `public, max-age=31536000, immutable`

## 3. Testing and Commit

- [ ] 3.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 3.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 3.3 Commit all changes (only after both test suites pass)
