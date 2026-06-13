# thumbnail-http-cache

The `GET /api/assets/{id}/thumbnail` endpoint returns a `Cache-Control: public, max-age=31536000, immutable` header on every response, enabling permanent browser and CDN caching of thumbnail images.

---

## Requirements

### Requirement: Thumbnail responses carry a permanent cache header

The `GET /api/assets/{id}/thumbnail` endpoint SHALL include `Cache-Control: public, max-age=31536000, immutable` in every `200 OK` response.

#### Scenario: Browser caches thumbnail on first request

- **WHEN** a client requests `GET /api/assets/42/thumbnail`
- **THEN** the response status is `200 OK`, the body contains the JPEG thumbnail bytes, and the response includes `Cache-Control: public, max-age=31536000, immutable`

#### Scenario: Cache header is present regardless of asset content

- **GIVEN** any valid non-deleted asset with a generated thumbnail
- **WHEN** `GET /api/assets/{id}/thumbnail` is called
- **THEN** the response always includes the `Cache-Control: public, max-age=31536000, immutable` header

#### Scenario: 404 does not carry the cache header

- **GIVEN** no asset with id 999 exists
- **WHEN** a client requests `GET /api/assets/999/thumbnail`
- **THEN** the response status is `404 Not Found` and no `Cache-Control: public, max-age=31536000, immutable` header is present
