## Why

The gallery selection toolbar lets users move and delete groups of selected assets, but there is no way to download selected photos to the local machine from the browser. Exporting photos currently requires direct filesystem or SFTP access to the server, which excludes non-technical users and defeats the purpose of the web interface. Adding a single "Download" button that streams the selected images as a ZIP archive closes this gap and makes the browser interface self-contained for the most common photo-export workflow.

## What Changes

- Add a `POST /api/assets/download` endpoint to `AssetController` that accepts a JSON body `{"assetIds": [...]}`, resolves each asset via `AssetRepository`, reads the file bytes using `StorageService.readFileBytes()`, packs them into a `ZipOutputStream` written directly to the `HttpServletResponse` output stream, and returns the ZIP with `Content-Type: application/zip` and `Content-Disposition: attachment; filename="photos.zip"`.
- Add a `DownloadAssetsRequest` DTO record in `api/dto/` with a `List<Long> assetIds` field annotated with `@NotEmpty` and `@Size(max = 500)`.
- Add a `downloadAssets(List<Long> assetIds, HttpServletResponse response)` method signature to `PhotoManagerFacade` and its implementation `PhotoManagerFacadeImpl`.
- In `PhotoManagerFacadeImpl.downloadAssets()`: resolve each asset ID via `assetRepository.findAllById(assetIds)`, build a filename-collision map (deduplicate by prefixing `{assetId}_` when two assets share the same `fileName`), iterate over found assets, call `storageService.readFileBytes(asset.getFullPath())`, and write each entry to a shared `ZipOutputStream` on the response; log a warning and skip any asset whose file cannot be read.
- Add a configurable `photomanager.max-download-assets` property (default `500`) to `application.yml`; validate the request size against this limit in the controller and return `400 Bad Request` when exceeded.
- Add a `downloadAssets(assetIds: number[]): Observable<Blob>` method to the Angular `AssetService` using `HttpClient.post` with `responseType: 'blob'`.
- Add a `downloadSelected()` method to `GalleryComponent` that collects `Array.from(selectedAssets)`, calls `assetService.downloadAssets(ids)`, shows a `MatSnackBar` "Preparing download…" message while the request is in flight, and on success creates a temporary `<a>` element with `URL.createObjectURL(blob)`, sets `download="photos.zip"`, programmatically clicks it, then revokes the object URL.
- Add a "Download" `mat-icon-button` (icon: `download`) to the gallery selection toolbar, visible when `selectedCount >= 1` alongside the existing Move and Delete buttons.

## Capabilities

### New Capabilities

- `bulk-download-zip`: Download a selection of gallery assets as a single ZIP archive directly from the browser. The backend streams the ZIP without writing a temporary file to disk; the browser triggers a native save-file dialog for `photos.zip`.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`DownloadAssetsRequest.java`** *(new)*: DTO in `api/dto/` — a `@Data` Lombok class with `@NotEmpty @Size(max = 500) List<Long> assetIds`; carries Bean Validation constraints used in the controller handler.
- **`PhotoManagerFacade.java`**: new `void downloadAssets(List<Long> assetIds, HttpServletResponse response) throws IOException;` method signature.
- **`PhotoManagerFacadeImpl.java`**: implementation — fetches assets via `assetRepository.findAllById(assetIds)`, builds a deduplication map for ZIP entry names (prefix `{assetId}_` on collision), writes each readable file into a `ZipOutputStream` wrapping `response.getOutputStream()`, logs a warning for missing or unreadable files and continues, sets response headers before the first `ZipEntry` is written.
- **`AssetController.java`**: new `POST /api/assets/download` handler — validates `@Valid @RequestBody DownloadAssetsRequest`, compares request size against `${photomanager.max-download-assets:500}`, returns `400` if exceeded, otherwise delegates to `facade.downloadAssets(request.getAssetIds(), response)`; returns no response body (bytes flow directly into the response stream).
- **`application.yml`**: new property `photomanager.max-download-assets: 500`.
- **`asset.service.ts`**: new `downloadAssets(assetIds: number[]): Observable<Blob>` method posting to `POST /api/assets/download` with `{ responseType: 'blob' }`.
- **`gallery.component.ts`**: new `downloadSelected()` method; imports updated to include `MatSnackBar` (already present) and no new modules.
- **`gallery.component.html`**: new "Download" button in the selection toolbar section rendered when `selectedCount >= 1`.
- **No existing API contracts broken** — all existing endpoints remain unchanged.
