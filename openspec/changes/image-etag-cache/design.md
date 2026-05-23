## Context

The `GET /api/assets/{id}/image` endpoint reads the full image file from disk and returns it as a `ResponseEntity<byte[]>`. Full-size images range from a few hundred KB to several MB. The `Asset` entity has a `hash` field (SHA-256 hex string) computed during cataloging. No HTTP caching headers are currently set.

## Goals / Non-Goals

**Goals:**
- Return `ETag: "<hash>"` on every 200 response
- Return `Cache-Control: private, max-age=3600` on every 200 response
- Return `304 Not Modified` when the client sends `If-None-Match` matching the current hash
- Short-circuit the expensive file read for 304 responses

**Non-Goals:**
- Adding `ETag` to the thumbnail endpoint (handled by `thumbnail-http-cache` using `Cache-Control: immutable`)
- `Last-Modified` / `If-Modified-Since` support
- Shared (`public`) caching for full images

## Decisions

### 1. Use stored `hash` as the ETag source (not `ShallowEtagHeaderFilter`)

**Decision:** Read the `hash` field from the `Asset` entity and use it directly as the ETag value, checking `If-None-Match` before reading the file.

**Rationale:** Spring's `ShallowEtagHeaderFilter` computes an ETag by hashing the entire response body. It still reads the full image file into memory before computing the hash — providing no I/O savings for 304 responses. Using the stored `hash` instead allows the 304 decision to be made after a single database lookup, without touching the file at all.

**Alternative considered:** `ShallowEtagHeaderFilter`. Rejected because it provides no I/O benefit and requires wrapping the response, making it harder to combine with explicit `Cache-Control` headers.

### 2. `Cache-Control: private, max-age=3600`

**Decision:** Use `private` (not `public`) with a 1-hour TTL for full-size images.

**Rationale:** Full-size images may be large and sensitive. `private` restricts caching to the browser only. A 1-hour TTL combined with ETag-based revalidation gives a good balance: after 1 hour the browser revalidates with `If-None-Match`, typically receiving a lightweight `304`.

**Alternative considered:** `public, max-age=31536000, immutable` (same as thumbnails). Rejected because full images are potentially large and sensitive, making CDN storage a higher-risk choice.

### 3. ETag format

**Decision:** Use the hex SHA-256 hash directly as the ETag, formatted as a quoted string per HTTP spec: `ETag: "abc123..."`.

**Rationale:** The hash is already computed and stored. No additional processing is needed.

### 4. 304 short-circuit before file I/O

**Decision:** The controller checks `If-None-Match` immediately after loading the `Asset` entity and returns `304` before calling the storage service to read the file.

**Rationale:** The primary benefit of ETag caching is avoiding the expensive file read. The controller already loads the `Asset` by ID (a fast indexed lookup). Comparing the hash costs nanoseconds. The file read (potentially several MB from disk) is the expensive operation to skip.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Hash mismatch if file is replaced without re-cataloging | Low | Re-cataloging updates the hash; direct file replacement without cataloging is unsupported |
| 1-hour TTL may serve stale content after asset deletion | Low | Soft-delete updates `deletedAt` but the browser cache holds the bytes — acceptable since the image data itself is unchanged |
