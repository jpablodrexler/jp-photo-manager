## Why

When browsing photos in the gallery viewer, users have no way to inspect the technical metadata embedded in the image file. EXIF data — camera make/model, exposure settings, GPS coordinates, and date taken — is essential context for photographers evaluating or organising their shots. The backend already parses EXIF rotation via Apache Commons Imaging but discards all other tags. Surfacing this data in a collapsible side panel in the viewer completes the photo inspection workflow. Storing EXIF in the database during cataloging keeps the viewer endpoint fast (no disk I/O at query time) and ensures data is available even if the original file is later moved or deleted.

## What Changes

- Add a `getExifMetadata(String filePath)` method to the `StorageService` interface and its `StorageServiceImpl` implementation, extracting 13 EXIF fields using Apache Commons Imaging.
- Add a new `AssetExif` JPA entity in `domain/entity/` representing a one-to-one relationship with `Asset`, holding the 13 EXIF fields.
- Add a new `AssetExifRepository` Spring Data JPA interface in `domain/repository/`.
- Add a Flyway migration `V7__add_asset_exif.sql` creating the `asset_exif` table with a unique foreign key to `assets`.
- Update `CatalogAssetsServiceImpl` to call `storageService.getExifMetadata()` for each indexed file and persist an `AssetExif` row linked to the newly created `Asset`.
- Add a `getAssetExif(Long assetId)` method to `PhotoManagerFacade` and `PhotoManagerFacadeImpl` that reads the `AssetExif` row from the database and maps it to an `ExifMetadata` record.
- Add a `GET /api/assets/{id}/exif` endpoint in `AssetController` returning an `ExifMetadataDto`.
- Add a new `ExifMetadata` Java record in the domain layer and an `ExifMetadataDto` in the API layer.
- Add a `getExifMetadata(assetId)` method to the Angular `AssetService`.
- Create a standalone `ExifPanelComponent` (Angular Material) that fetches and displays EXIF fields for the currently viewed asset.
- Wire the panel into `GalleryComponent` via an info-icon toggle button in the viewer toolbar.

## Capabilities

### New Capabilities

- `exif-metadata-panel`: Extract and persist EXIF metadata from image files during cataloging, and display it in a collapsible side panel in the gallery viewer.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`V7__add_asset_exif.sql`** *(new)*: creates the `asset_exif` table (15 columns: PK, FK to `assets`, 13 nullable EXIF fields).
- **`AssetExif.java`** *(new)*: JPA entity with `@OneToOne` relationship to `Asset`.
- **`AssetExifRepository.java`** *(new)*: Spring Data JPA interface with `findByAsset_AssetId` and `deleteByAsset_AssetId`.
- **`ExifMetadata.java`** *(new)*: domain value-object record with 13 nullable fields; used by `StorageService` as the return type during file extraction.
- **`StorageService.java`**: new `getExifMetadata` method signature.
- **`StorageServiceImpl.java`**: implementation reading 13 EXIF tags (make, model, date taken, f-number, exposure time, ISO, focal length, flash, exposure program, white balance, metering mode, GPS lat/lon).
- **`CatalogAssetsServiceImpl.java`**: calls `storageService.getExifMetadata()` per file and persists `AssetExif` via `AssetExifRepository`.
- **`PhotoManagerFacade.java`**: new `getAssetExif` method signature.
- **`PhotoManagerFacadeImpl.java`**: reads `AssetExif` from DB via repository and maps to `ExifMetadata` record.
- **`ExifMetadataDto.java`** *(new)*: API DTO mirroring the 13 fields.
- **`AssetController.java`**: new `GET /api/assets/{id}/exif` endpoint + `toExifDto` mapper.
- **`asset.service.ts`**: new `getExifMetadata` method.
- **`exif-metadata.model.ts`** *(new)*: TypeScript interface.
- **`ExifPanelComponent`** *(new)*: standalone Angular Material component.
- **`GalleryComponent`**: new `showExifPanel` state, info-icon button, panel slot in viewer template.
- **No breaking API changes** — all existing endpoints are unchanged.
