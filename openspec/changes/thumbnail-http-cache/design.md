## Context

The `GET /api/assets/{id}/thumbnail` endpoint in `AssetController` reads a `.bin` thumbnail file from disk and returns a `ResponseEntity<byte[]>` with `Content-Type: image/jpeg`. Thumbnails are 200×150 px JPEG files generated once during cataloging and stored under the thumbnails directory. Once written, a thumbnail file is never overwritten — it is keyed by `assetId` and the file content does not change unless the `thumbnail-regeneration` feature deletes and recreates it.

## Goals / Non-Goals

**Goals:**
- Add `Cache-Control: public, max-age=31536000, immutable` to the thumbnail response
- Document the known limitation around thumbnail regeneration

**Non-Goals:**
- Adding `ETag` to thumbnails (covered by `image-etag-cache` for full images)
- Cache invalidation infrastructure
- CDN configuration

## Decisions

### 1. `Cache-Control: public, max-age=31536000, immutable`

**Decision:** Use `Cache-Control: public, max-age=31536000, immutable` (one year, immutable) on the thumbnail endpoint.

**Rationale:** Thumbnails are content-addressed by `assetId`. The same URL (`/api/assets/{id}/thumbnail`) always returns the same bytes once the thumbnail is created. `public` allows proxy caches and CDNs to store the response. `max-age=31536000` (one year in seconds) is the recommended value for permanently cacheable resources. `immutable` tells browsers not to revalidate within the max-age window, eliminating conditional requests.

**Alternative considered:** `Cache-Control: private, max-age=3600` (1 hour). Rejected because thumbnails are small, not sensitive, and truly immutable — a year-long public cache is safe and substantially reduces server load.

### 2. `public` visibility for authenticated content

**Decision:** Use `public` despite the app requiring authentication.

**Rationale:** This is a self-hosted app where the CDN (if any) sits behind the authentication layer. Thumbnail content is low-sensitivity. Using `public` enables the browser disk cache to be shared across browser profiles on the same device.

**Alternative considered:** `private` (only browser cache, no CDN). Acceptable but unnecessarily conservative given the deployment model.

### 3. Known limitation with thumbnail-regeneration

**Decision:** Document the stale-cache risk and defer mitigation to `thumbnail-regeneration` (#35).

**Rationale:** If a thumbnail is regenerated (corrupt file, size change), the URL stays the same but browsers serve the stale cached version until max-age expires. The `thumbnail-regeneration` feature can add a `?v={version}` query parameter to bust caches; implementing that here would be premature coupling.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Stale thumbnails after regeneration | Low | Documented; `thumbnail-regeneration` (#35) can add cache-busting via URL versioning |
| `public` for authenticated resources | Low | Thumbnails are low-sensitivity; self-hosted deployment model makes CDN interception unlikely |
