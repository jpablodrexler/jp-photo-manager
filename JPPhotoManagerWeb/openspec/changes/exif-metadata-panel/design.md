## Context

The backend's `StorageServiceImpl.getImageRotation()` already opens a JPEG file via `Imaging.getMetadata()` and navigates to `TiffImageMetadata` to read the orientation tag. Apache Commons Imaging (already on the classpath) provides `ExifTagConstants` and `GpsTagConstants` that expose all remaining EXIF fields with no additional dependencies.

`CatalogAssetsServiceImpl` already reads each image file to compute its hash, generate a thumbnail, and detect orientation. EXIF extraction can be integrated into this same per-file pass at zero additional I/O cost, since `Imaging.getMetadata()` reads only the EXIF segment.

The `Asset` JPA entity is the aggregate root for a catalogued image. A one-to-one `AssetExif` child entity keeps EXIF fields in a separate, narrower table, preserving the existing `assets` table schema and allowing the `GET /api/assets` list to remain efficient.

The Angular `GalleryComponent` already manages a `viewMode` toggle between `'thumbnails'` and `'viewer'`. The viewer renders the current asset's full image. Adding a boolean `showExifPanel` flag and an `ExifPanelComponent` slot extends this pattern without restructuring the component.

## Goals / Non-Goals

**Goals:**

- Persist 13 EXIF fields per catalogued image in a dedicated `asset_exif` table during the catalog process.
- Expose the persisted data via `GET /api/assets/{id}/exif` (DB read, no disk I/O at query time).
- Display non-null fields in a collapsible Material side panel in the gallery viewer.
- Return gracefully (all-null response) for non-JPEG files that carry no EXIF.

**Non-Goals:**

- Re-reading EXIF from disk at query time.
- Writing or editing EXIF tags.
- Extracting EXIF from non-JPEG formats (PNG, GIF, BMP carry no EXIF; their `asset_exif` rows are stored with all-null fields).
- Displaying a map for GPS coordinates (coordinates are shown as decimal values only).
- Indexing EXIF fields for search or filtering.

## Decisions

### 1. Persist EXIF during cataloging — not on demand

**Decision:** `CatalogAssetsServiceImpl` calls `storageService.getExifMetadata(filePath)` for each file in the same pass that generates the thumbnail and computes the hash. The result is persisted in `asset_exif` via `AssetExifRepository`. `GET /api/assets/{id}/exif` reads from the database and performs no disk I/O.

**Rationale:** EXIF extraction via Apache Commons Imaging reads only the JPEG APP1 segment, adding negligible overhead to the catalog pass. Storing the result in the database makes the viewer endpoint fast (a single indexed join query), independent of file availability, and consistent across re-renders. If the original file is moved or deleted after cataloging, EXIF remains queryable.

**Alternative considered:** Reading EXIF from disk on each `GET /api/assets/{id}/exif` call. Rejected because it couples the viewer API to file availability and adds disk I/O on every panel open. The catalog-time approach is strictly better given that files are already being fully processed at that stage.

### 2. Separate `asset_exif` table — not columns on `assets`

**Decision:** EXIF fields live in a new `asset_exif` table with a one-to-one unique foreign key to `assets`, rather than adding 13 nullable columns directly to the `assets` table.

**Rationale:** The `assets` table is already wide (19 columns) and is queried in bulk for pagination. Adding 13 nullable columns widens every row even for assets with no EXIF (PNG, GIF). A separate child table keeps the hot path lean: `GET /api/assets` queries only `assets`; EXIF is fetched only on demand via the dedicated endpoint.

**Alternative considered:** Adding nullable columns directly to `assets`. Rejected to avoid schema bloat on the primary query path and to make the EXIF feature independently removable.

### 3. New `GET /api/assets/{id}/exif` endpoint — do not embed in asset list

**Decision:** EXIF is exposed at a dedicated sub-resource endpoint, not inlined into the `AssetDto` returned by `GET /api/assets`.

**Rationale:** The asset list is fetched in batches of 100. Embedding EXIF into each list item would JOIN the `asset_exif` table on every page load, most of which are never inspected in the viewer. The dedicated endpoint is called only when the user opens the viewer and clicks the info button.

**Alternative considered:** Adding an `?includeExif=true` query parameter to `GET /api/assets`. Rejected as it complicates the existing endpoint and makes the batch JOIN unavoidable for callers who set the flag.

### 4. `ExifMetadata` as a domain value-object record; `AssetExif` as the JPA entity

**Decision:** `StorageService.getExifMetadata()` returns an `ExifMetadata` Java record (a pure value type, no JPA annotations) located in `domain/service/`. `AssetExif` is a JPA `@Entity` in `domain/entity/` that `CatalogAssetsServiceImpl` populates from the record and persists. `PhotoManagerFacadeImpl.getAssetExif()` reads `AssetExif` from the repository and maps back to an `ExifMetadata` record before returning.

**Rationale:** Keeps the `StorageService` contract free of JPA concerns. The record serves as the boundary type between file I/O and persistence, and between the facade and the API controller — consistent with how the rest of the codebase separates entities from DTOs.

### 5. All EXIF tag reads are individually fault-tolerant during cataloging

**Decision:** Each tag extraction in `StorageServiceImpl.getExifMetadata()` is wrapped in its own null-check or try-catch. A missing or malformed tag stores `null` for that field in `asset_exif`; the remaining fields are stored normally. Catalog processing is never aborted by a malformed EXIF tag.

**Rationale:** EXIF conformance varies widely across camera manufacturers. A defensive per-field approach maximises the data stored even from partially-compliant files, consistent with how `getImageRotation()` handles missing orientation tags.

### 6. GPS converted to decimal degrees at catalog time

**Decision:** GPS DMS rational arrays are converted to signed decimal degrees in `StorageServiceImpl.getExifMetadata()` before the `ExifMetadata` record is returned. The `asset_exif` table stores `gps_latitude` and `gps_longitude` as `DOUBLE PRECISION` decimal values.

**Rationale:** Storing pre-converted decimal values eliminates repeated DMS parsing at query time. Decimal degrees are the standard format for downstream display and potential future map integration.

### 7. Catalog re-run replaces existing EXIF row

**Decision:** When `CatalogAssetsServiceImpl` processes a file that already has an `asset_exif` row (re-catalog scenario), the existing row is deleted and replaced with a freshly extracted one.

**Rationale:** Ensures EXIF data stays consistent if a file is replaced in-place with different content between catalog runs. The `ON DELETE CASCADE` constraint on the FK handles cleanup automatically when an `Asset` is deleted.

## Risks / Trade-offs

- **Catalog performance** — EXIF extraction adds one `Imaging.getMetadata()` call per file. This reads only the EXIF segment (not full pixels) and is negligible for JPEG; for non-JPEG files the call returns immediately with no data. No measurable impact on catalog throughput is expected.
- **Schema migration required** — `V7__add_asset_exif.sql` must be applied before the updated application starts. Flyway handles this automatically; no manual intervention needed.
- **Existing catalogued assets have no EXIF row** — assets catalogued before this feature is deployed will have no `asset_exif` row. `GET /api/assets/{id}/exif` returns `200` with all-null fields in this case. A re-catalog populates the missing rows.
- **Panel layout on narrow screens** — the side-by-side viewer + panel layout requires a minimum viewport width. Below a breakpoint the panel should stack below the image. This is addressed in the component's SCSS with a media query.

## Data Flow

### Catalog time (write path)

```
CatalogAssetsServiceImpl.processFile(filePath)
  → storageService.getExifMetadata(filePath)      [disk I/O, EXIF segment only]
    → Imaging.getMetadata(file) → parse 13 tags
    → ExifMetadata record (all-null for non-JPEG)
  → new AssetExif(asset, exifMetadata)
  → assetExifRepository.save(assetExif)           [INSERT into asset_exif]
```

### Query time (read path)

```
User clicks ℹ in viewer toolbar
  → GalleryComponent.toggleExifPanel()            [frontend]
    → showExifPanel = true
      → ExifPanelComponent [assetId] [visible]
        → cache miss → AssetService.getExifMetadata(assetId)
          → GET /api/assets/{id}/exif             [HTTP, JWT cookie]
            → AssetController.getExifMetadata()   [backend]
              → PhotoManagerFacadeImpl.getAssetExif(id)
                → assetExifRepository.findByAsset_AssetId(id)  [DB — indexed]
                → map AssetExif → ExifMetadata record
              → toExifDto(exif) → ExifMetadataDto
            → 200 JSON
          → Observable<ExifMetadata> → cache
        → render non-null fields in MatList
```

## File Change List

**New files:**
- `backend/.../resources/db/migration/V7__add_asset_exif.sql`
- `backend/.../domain/entity/AssetExif.java`
- `backend/.../domain/repository/AssetExifRepository.java`
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

**Modified files:**
- `backend/.../domain/service/StorageService.java`
- `backend/.../infrastructure/service/StorageServiceImpl.java`
- `backend/.../infrastructure/service/CatalogAssetsServiceImpl.java`
- `backend/.../application/PhotoManagerFacade.java`
- `backend/.../application/PhotoManagerFacadeImpl.java`
- `backend/.../api/AssetController.java`
- `frontend/src/app/core/services/asset.service.ts`
- `frontend/src/app/features/gallery/gallery.component.ts`
- `frontend/src/app/features/gallery/gallery.component.html`
- `frontend/src/app/features/gallery/gallery.component.scss`
