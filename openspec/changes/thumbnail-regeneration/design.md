## Context

The thumbnail infrastructure consists of: `ThumbnailPort` (domain interface for reading/writing thumbnail binaries), `StoragePort` (domain interface for image reading and thumbnail generation), and `StorageServiceImpl` (infrastructure implementation that uses Java `ImageIO` to generate 200×150 JPEG thumbnails). The `asset_exif` table stores `imageRotation` which can be used to apply rotation during regeneration.

## Goals / Non-Goals

**Goals:**
- Delete existing thumbnail files for selected assets via `ThumbnailPort`
- Regenerate thumbnails via `StoragePort` (same code path as initial cataloging)
- Optionally scope to a `folderPath` query parameter
- Stream progress via `SseEmitter`
- Apply EXIF rotation during regeneration (retroactive correction)

**Non-Goals:**
- Re-extracting EXIF metadata (that requires re-cataloging)
- Regenerating thumbnails for soft-deleted assets
- Changing the thumbnail dimensions (a separate configuration concern)

## Decisions

### 1. Delete-then-regenerate per asset

**Decision:** For each asset in scope, call `thumbnailPort.deleteThumbnail(assetId)` then `storagePort.generateThumbnail(asset)`. Proceed to the next asset regardless of individual failures (log errors, report count).

**Rationale:** A per-asset approach allows partial success — if one thumbnail fails (e.g., corrupt source file), others continue. The existing catalog service uses the same pattern.

### 2. SSE progress streaming

**Decision:** The endpoint returns an `SseEmitter` and sends a progress event after each asset is processed. The frontend reuses the existing SSE handling pattern (same as catalog/sync/convert).

**Rationale:** Regenerating thousands of thumbnails can take minutes. SSE provides real-time feedback without polling.

### 3. Optional `folderPath` scoping

**Decision:** If `folderPath` is provided, only assets in that folder (non-recursive, matching `folder_path = :folderPath`) are regenerated. If omitted, all non-deleted assets are regenerated.

**Rationale:** Users may want to regenerate only one folder (e.g., after a vacation import) without processing the entire catalog.

### 4. `@PreAuthorize("hasRole('ADMIN')")`

**Decision:** Thumbnail regeneration is an administrative operation — restrict it to `ADMIN` role (consistent with catalog and sync operations).

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Source image file missing (asset record exists but file deleted) | Low | Log the error and skip the asset; continue with others |
| Regeneration runs concurrently with a catalog operation | Low | Document that users should not run both simultaneously; a future distributed lock could prevent this |
| Browser caches stale thumbnails after regeneration (see `thumbnail-http-cache` #26) | Medium | After regeneration, append `?v={timestamp}` to thumbnail URLs to bust caches if `thumbnail-http-cache` is implemented |
