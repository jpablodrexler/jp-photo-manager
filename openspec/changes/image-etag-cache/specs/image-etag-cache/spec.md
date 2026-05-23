# image-etag-cache

The `GET /api/assets/{id}/image` endpoint returns an `ETag` header derived from the asset's stored SHA-256 hash and supports `If-None-Match` conditional requests, enabling `304 Not Modified` responses that avoid re-downloading unchanged images.

---

## ADDED Requirements

### Requirement: Image responses carry an ETag and Cache-Control header

The `GET /api/assets/{id}/image` endpoint SHALL include `ETag: "<sha256hash>"` and `Cache-Control: private, max-age=3600` in every `200 OK` response.

#### Scenario: Client receives ETag on first request

- **WHEN** a client requests `GET /api/assets/42/image`
- **THEN** the response status is `200 OK`, the body contains the image bytes, the response includes `ETag: "<sha256hash>"`, and `Cache-Control: private, max-age=3600`

### Requirement: Conditional requests receive 304 when content is unchanged

The endpoint SHALL return `304 Not Modified` with no body when the `If-None-Match` header matches the current asset hash.

#### Scenario: Client sends matching If-None-Match

- **GIVEN** the client previously received `ETag: "abc123..."` for asset 42
- **WHEN** the client requests `GET /api/assets/42/image` with `If-None-Match: "abc123..."`
- **THEN** the response status is `304 Not Modified` and the body is empty

#### Scenario: Client sends non-matching If-None-Match

- **GIVEN** the client has a stale ETag `"oldvalue"` for asset 42
- **WHEN** the client requests `GET /api/assets/42/image` with `If-None-Match: "oldvalue"`
- **THEN** the response status is `200 OK`, the body contains the current image bytes, and the response includes the current `ETag`

#### Scenario: 304 does not read the image file

- **GIVEN** the client sends a matching `If-None-Match`
- **WHEN** the server processes the request
- **THEN** the storage service is not called to read the image file
