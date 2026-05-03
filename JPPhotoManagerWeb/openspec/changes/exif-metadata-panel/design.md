## Context

The backend's `StorageServiceImpl.getImageRotation()` already opens a JPEG file via `Imaging.getMetadata()` and navigates to `TiffImageMetadata` to read the orientation tag. Apache Commons Imaging (already on the classpath) provides `ExifTagConstants` and `GpsTagConstants` that expose all remaining EXIF fields with no additional dependencies.

The `Asset` JPA entity stores file paths via its `Folder` relationship and `fileName` field. The `getFullPath()` transient method assembles the absolute path, making it straightforward to resolve the file for any asset ID.

The Angular `GalleryComponent` already manages a `viewMode` toggle between `'thumbnails'` and `'viewer'`. The viewer renders the current asset's full image. Adding a boolean `showExifPanel` flag and an `ExifPanelComponent` slot extends this pattern without restructuring the component.

## Goals / Non-Goals

**Goals:**

- Expose 13 EXIF fields via `GET /api/assets/{id}/exif`.
- Display non-null fields in a collapsible Material side panel in the gallery viewer.
- Return gracefully (all-null response) for non-JPEG files that carry no EXIF.
- Keep EXIF data out of the database — read from disk on every request.

**Non-Goals:**

- Persisting EXIF data to the database or indexing it for search.
- Writing or editing EXIF tags.
- Extracting EXIF from non-JPEG formats (PNG, GIF, BMP have no EXIF; they receive an all-null response).
- Displaying a map for GPS coordinates (coordinates are shown as decimal values only).
- Caching EXIF responses on the server (client-side caching via HTTP is sufficient).

## Decisions

### 1. Read EXIF on demand — do not persist

**Decision:** `GET /api/assets/{id}/exif` reads the file from disk on each call. No new database columns or migrations are introduced.

**Rationale:** EXIF data is immutable for a given file. Reading it on demand avoids a schema migration, eliminates stale-data risk if the file is replaced, and keeps the catalog process unchanged. File I/O for a single metadata read is fast (Apache Commons Imaging reads only the EXIF segment, not the full image).

**Alternative considered:** Storing all 13 fields as nullable columns in the `assets` table and populating them during cataloging. Rejected because it requires a Flyway migration, changes the catalog service, and adds nullable columns to an already wide table.

### 2. New `GET /api/assets/{id}/exif` endpoint — do not embed in asset list

**Decision:** EXIF is exposed at a dedicated sub-resource endpoint, not inlined into the `AssetDto` returned by `GET /api/assets`.

**Rationale:** The asset list is fetched in batches of 100. Embedding EXIF into each list item would add 13 nullable fields per asset and perform 100 file reads per page load — most of which are never viewed. A dedicated endpoint is fetched only when the user opens the viewer and clicks the info button.

**Alternative considered:** Adding an `?includeExif=true` query parameter to `GET /api/assets`. Rejected as it complicates the existing endpoint and the opt-in flag would be misleading for batch loads.

### 3. `ExifMetadata` as a domain record, `ExifMetadataDto` as the API DTO

**Decision:** Introduce a Java record `ExifMetadata` in `domain/service/` (co-located with `StorageService`) and a separate `@Data` class `ExifMetadataDto` in `api/dto/`. `AssetController.toExifDto()` maps between them.

**Rationale:** Follows the existing layering convention where domain types never leak into the API layer. The record is a pure value type with no JPA annotations, keeping the domain clean.

### 4. All EXIF tag reads are individually fault-tolerant

**Decision:** Each tag extraction in `StorageServiceImpl.getExifMetadata()` is wrapped in its own null-check or try-catch. A missing or malformed tag for one field does not prevent the remaining fields from being read.

**Rationale:** EXIF conformance varies widely across camera manufacturers. A defensive per-field approach ensures maximum data is surfaced even from partially-compliant files, consistent with how `getImageRotation()` handles missing orientation tags.

### 5. GPS converted to decimal degrees

**Decision:** GPS coordinates stored in EXIF as DMS rational arrays (degrees, minutes, seconds + hemisphere reference) are converted to signed decimal degrees in `StorageServiceImpl`. The DTO exposes `Double gpsLatitude` and `Double gpsLongitude`.

**Rationale:** Decimal degrees are the standard format for downstream use (map links, display). Converting in the service layer keeps the DTO simple and avoids pushing DMS parsing to the frontend.

### 6. `ExifPanelComponent` fetches lazily and caches by asset ID

**Decision:** The panel component calls `assetService.getExifMetadata(assetId)` when it first becomes visible for a given asset. The result is stored in a `Map<number, ExifMetadata>` keyed by `assetId` within the component's lifetime. Re-opening the panel for the same asset does not re-fetch.

**Rationale:** Avoids redundant HTTP requests when the user toggles the panel open and closed for the same image. The cache is component-scoped (lives as long as the gallery route is active), which is appropriate given EXIF immutability.

## Risks / Trade-offs

- **Large JPEG files** — Apache Commons Imaging reads only the EXIF APP1 segment, not the full image pixels, so file size has negligible impact on response time.
- **Files moved or deleted after cataloging** — `getAssetExif` returns 404 if `Imaging.getMetadata()` throws, consistent with how `getAssetImage` handles the same case.
- **Panel layout on narrow screens** — the side-by-side viewer + panel layout requires a minimum viewport width. Below a breakpoint the panel should stack below the image. This is addressed in the component's SCSS with a media query.

## Data Flow

```
User clicks ℹ in viewer toolbar
  → GalleryComponent.toggleExifPanel()           [frontend]
    → showExifPanel = true
      → ExifPanelComponent [assetId] [visible]
        → cache miss → AssetService.getExifMetadata(assetId)
          → GET /api/assets/{id}/exif             [HTTP, JWT cookie]
            → AssetController.getExifMetadata()   [backend]
              → PhotoManagerFacadeImpl.getAssetExif(id)
                → AssetRepository.findById(id)    [DB]
                → StorageServiceImpl.getExifMetadata(asset.getFullPath())
                  → Imaging.getMetadata(file)     [disk I/O]
                  → parse 13 tags → ExifMetadata record
              → toExifDto(exif) → ExifMetadataDto
            → 200 JSON
          → Observable<ExifMetadata> → cache
        → render non-null fields in MatList
```

## File Change List

**New files:**
- `backend/.../domain/service/ExifMetadata.java`
- `backend/.../api/dto/ExifMetadataDto.java`
- `backend/.../test/.../StorageServiceImplExifTest.java`
- `backend/.../test/.../AssetControllerExifTest.java`
- `backend/.../test/.../ExifMetadataIntegrationTest.java`
- `frontend/src/app/core/models/exif-metadata.model.ts`
- `frontend/src/app/shared/components/exif-panel/exif-panel.component.ts`
- `frontend/src/app/shared/components/exif-panel/exif-panel.component.html`
- `frontend/src/app/shared/components/exif-panel/exif-panel.component.scss`
- `frontend/src/app/shared/components/exif-panel/exif-panel.component.cy.ts`
- `JPPhotoManagerWeb/openspec/specs/exif-metadata-panel/spec.md`

**Modified files:**
- `backend/.../domain/service/StorageService.java`
- `backend/.../infrastructure/service/StorageServiceImpl.java`
- `backend/.../application/PhotoManagerFacade.java`
- `backend/.../application/PhotoManagerFacadeImpl.java`
- `backend/.../api/AssetController.java`
- `frontend/src/app/core/services/asset.service.ts`
- `frontend/src/app/features/gallery/gallery.component.ts`
- `frontend/src/app/features/gallery/gallery.component.html`
- `frontend/src/app/features/gallery/gallery.component.scss`
