## Why

Photos currently enter the catalog only by being placed on the server filesystem and then cataloged via the scheduled or manually triggered catalog process. Users who want to add photos must have direct filesystem access to the server — there is no path for a browser-based upload. This forces even basic photo-management tasks to leave the web UI entirely.

Adding a drag-and-drop upload zone inside the gallery view closes that gap. Users can drop one or more image files onto the thumbnail grid (or click an "Upload" button) and have them appear as new catalog entries in the currently selected folder immediately — without any shell or file-manager involvement. The implementation reuses the existing `CatalogFolderServiceImpl.createAsset` indexing logic and `StorageService.copyFile`, keeping the new surface area small and the catalog consistent with files added any other way.

## What Changes

- Add a `POST /api/assets/upload` endpoint to `AssetController` accepting `multipart/form-data` with fields `file` (binary) and `folderPath` (string); return the new `AssetDto` on success.
- Add an `uploadAsset(folderPath, file)` method to `PhotoManagerFacade` and `PhotoManagerFacadeImpl` that validates the folder, writes the multipart bytes to a temp file via `StorageService.copyFile`, calls `CatalogFolderService.createAsset` to index the file, and returns the new `Asset`.
- Add an `UploadRequest` DTO record in `api/` to carry the validated `folderPath` alongside the multipart file.
- Add content-type and file-extension validation in the endpoint handler: accept JPEG, PNG, GIF, BMP, TIFF, and WEBP; reject anything else with `415 Unsupported Media Type`.
- Add a `DropZoneComponent` standalone Angular component in `features/gallery/` rendering the drag-and-drop overlay and the "Upload" file-input button with `MatProgressBar` per-file feedback.
- Add an `uploadAsset(folderPath, file)` method to `AssetService` that posts to `POST /api/assets/upload` using `HttpClient` with `reportProgress: true` and `observe: 'events'` to emit upload progress.
- Wire `DropZoneComponent` into `GalleryComponent`: handle `dragover`/`drop` events on the thumbnail grid host, pass `currentFolder` to the drop zone, and call `loadAssets()` once all files in a batch are uploaded.

## Capabilities

### New Capabilities

- `drag-and-drop-upload`: Upload image files directly from the browser into the currently selected catalog folder. Files are saved to the server filesystem, indexed (hash, thumbnail, `Asset` entity), and displayed in the gallery immediately after upload without requiring a full catalog run.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`UploadRequest.java`** *(new)*: thin DTO record in `api/` carrying `folderPath`; used only to centralise Bean Validation on the string field alongside the multipart `MultipartFile` parameter.
- **`PhotoManagerFacade.java`**: new `uploadAsset(String folderPath, MultipartFile file) throws IOException` method signature.
- **`PhotoManagerFacadeImpl.java`**: implementation — validates folder existence, writes multipart bytes to a temp file under `java.io.tmpdir`, calls `catalogFolderService.createAsset(folderPath, fileName)`, deletes the temp file on success or failure, returns the saved `Asset`.
- **`AssetController.java`**: new `POST /api/assets/upload` handler — resolves content-type and extension, delegates to `facade.uploadAsset`, maps the returned `Asset` to `AssetDto` via the existing `toDto` helper, returns `201 Created`.
- **`GlobalExceptionHandler.java`**: new handler for `UnsupportedMediaTypeException` returning `415` and for `FolderNotFoundException` returning `404`.
- **`FolderNotFoundException.java`** *(new)*: simple unchecked exception thrown by `PhotoManagerFacadeImpl` when the requested folder does not exist in the repository.
- **`asset.service.ts`**: new `uploadAsset(folderPath, file)` method using `HttpClient` with `reportProgress: true`; returns `Observable<HttpEvent<AssetDto>>`.
- **`drop-zone.component.ts`** *(new)*: standalone Angular component with `@Input() folderPath`, `@Output() uploadComplete = new EventEmitter<void>()`, dragover/drop host listeners, hidden `<input type="file" multiple>`, `MatProgressBar` per file, and `MatSnackBar` error feedback.
- **`drop-zone.component.html`** *(new)*: overlay template shown when `isDragging` is true, plus "Upload" button and per-file progress list.
- **`drop-zone.component.scss`** *(new)*: overlay styling (semi-transparent backdrop, dashed border, centred label); progress list layout.
- **`drop-zone.component.cy.ts`** *(new)*: Cypress component tests for drag-over state, file-type rejection, per-file progress, and uploadComplete emission.
- **`GalleryComponent`**: imports `DropZoneComponent`; adds `isDragging` flag and host `dragover`/`dragleave`/`drop` listeners that forward events to the drop zone; calls `loadAssets()` on `uploadComplete`.
- **No existing API contracts broken** — all existing endpoints remain unchanged.
