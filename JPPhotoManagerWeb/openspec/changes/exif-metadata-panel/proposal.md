## Why

When browsing photos in the gallery viewer, users have no way to inspect the technical metadata embedded in the image file. EXIF data — camera make/model, exposure settings, GPS coordinates, and date taken — is essential context for photographers evaluating or organising their shots. The backend already parses EXIF rotation via Apache Commons Imaging but discards all other tags. Surfacing this data in a collapsible side panel in the viewer completes the photo inspection workflow without requiring any schema changes.

## What Changes

- Add a `getExifMetadata(String filePath)` method to the `StorageService` interface and its `StorageServiceImpl` implementation, extracting 13 EXIF fields using Apache Commons Imaging.
- Add a `getAssetExif(Long assetId)` method to `PhotoManagerFacade` and `PhotoManagerFacadeImpl` that looks up the asset's file path and delegates to `StorageService`.
- Add a `GET /api/assets/{id}/exif` endpoint in `AssetController` returning an `ExifMetadataDto`.
- Add a new `ExifMetadata` Java record in the domain layer and an `ExifMetadataDto` in the API layer.
- Add a `getExifMetadata(assetId)` method to the Angular `AssetService`.
- Create a standalone `ExifPanelComponent` (Angular Material) that fetches and displays EXIF fields for the currently viewed asset.
- Wire the panel into `GalleryComponent` via an info-icon toggle button in the viewer toolbar.

## Capabilities

### New Capabilities

- `exif-metadata-panel`: Read EXIF metadata from image files on demand and display it in a collapsible side panel in the gallery viewer.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`StorageService.java`**: new `getExifMetadata` method signature.
- **`StorageServiceImpl.java`**: implementation reading 13 EXIF tags (make, model, date taken, f-number, exposure time, ISO, focal length, flash, exposure program, white balance, metering mode, GPS lat/lon).
- **`PhotoManagerFacade.java`**: new `getAssetExif` method signature.
- **`PhotoManagerFacadeImpl.java`**: delegates to `StorageService` after resolving the asset path.
- **`AssetController.java`**: new `GET /api/assets/{id}/exif` endpoint + `toExifDto` mapper.
- **`ExifMetadata.java`** *(new)*: domain record with 13 nullable fields.
- **`ExifMetadataDto.java`** *(new)*: API DTO mirroring the record.
- **`asset.service.ts`**: new `getExifMetadata` method.
- **`exif-metadata.model.ts`** *(new)*: TypeScript interface.
- **`ExifPanelComponent`** *(new)*: standalone Angular Material component.
- **`GalleryComponent`**: new `showExifPanel` state, info-icon button, panel slot in viewer template.
- **No database schema changes** — EXIF is read from disk on demand and never persisted.
- **No breaking API changes** — all existing endpoints are unchanged.
