# drag-and-drop-upload

Specifies how the application accepts image files uploaded from the browser, saves them to a catalog folder, and indexes them as new assets.

---

### Requirement: Uploaded image files are saved to the target folder and indexed immediately

The system SHALL provide a `POST /api/assets/upload` endpoint accepting `multipart/form-data` with a `file` part (binary image data) and a `folderPath` part (string). The backend SHALL write the file to the target folder on the server filesystem and index it using the same pipeline as the catalog process (`CatalogFolderService.createAsset`). The endpoint SHALL return `201 Created` with the new `AssetDto` on success.

#### Scenario: Valid image uploaded to a cataloged folder
- **GIVEN** the user has selected a cataloged folder and drags a JPEG file onto the gallery
- **WHEN** the frontend posts the file to `POST /api/assets/upload`
- **THEN** the response is `201 Created` with an `AssetDto` containing the new asset's `assetId`, `fileName`, `fileSize`, `thumbnailUrl`, and `imageUrl`; the file is present on the server filesystem at `folderPath/fileName`; and a subsequent `GET /api/assets?folderPath=` includes the new asset

#### Scenario: Multiple files uploaded sequentially
- **GIVEN** the user drops three JPEG files onto the gallery
- **WHEN** the frontend uploads them one at a time
- **THEN** three separate `POST /api/assets/upload` requests are issued; each returns `201 Created`; after all three complete the gallery grid is refreshed showing the three new thumbnails

---

### Requirement: The upload endpoint rejects non-image files

The endpoint SHALL validate both the MIME content-type and the filename extension of the uploaded file. If either is not in the accepted image allowlist (`image/jpeg`, `image/png`, `image/gif`, `image/bmp`, `image/tiff`, `image/webp` / `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.tiff`, `.tif`, `.webp`), the endpoint SHALL return `415 Unsupported Media Type` and SHALL NOT write any data to the filesystem.

#### Scenario: Non-image file dropped
- **GIVEN** the user drops a `.pdf` file onto the gallery
- **WHEN** the frontend attempts to upload it
- **THEN** the file is filtered out client-side before a request is sent, and no `POST /api/assets/upload` request is issued

#### Scenario: Spoofed content-type
- **GIVEN** a request is crafted with content-type `image/jpeg` but a `.exe` filename extension
- **WHEN** the backend receives the request
- **THEN** the response is `415 Unsupported Media Type` and no file is written to disk

---

### Requirement: The upload endpoint returns 404 for unknown folder paths

If the `folderPath` in the upload request does not correspond to a folder already present in the catalog, the endpoint SHALL return `404 Not Found` and SHALL NOT write any data to the filesystem.

#### Scenario: Upload to an uncataloged folder
- **GIVEN** a folder path that has not been cataloged and has no row in the `folders` table
- **WHEN** an authenticated user calls `POST /api/assets/upload` with that `folderPath`
- **THEN** the response is `404 Not Found`

---

### Requirement: The gallery shows per-file upload progress

The `DropZoneComponent` SHALL display an individual progress bar for each file being uploaded. The progress SHALL advance as upload bytes are transmitted. On completion, a success indicator SHALL replace the progress bar.

#### Scenario: Progress bar advances during upload
- **GIVEN** a large image is being uploaded
- **WHEN** the upload is 50% complete
- **THEN** the progress bar for that file shows 50% fill

#### Scenario: Success indicator shown after completion
- **GIVEN** a file upload has just completed with `201 Created`
- **WHEN** the `DropZoneComponent` processes the response
- **THEN** the file's row shows a success icon (e.g., `check_circle`) and the progress bar is hidden

#### Scenario: Error indicator shown on failure
- **GIVEN** a file upload returns `415 Unsupported Media Type`
- **WHEN** the `DropZoneComponent` processes the error response
- **THEN** the file's row shows an error icon (e.g., `error`) and no further upload is attempted for that file

---

### Requirement: The drag-over state is visually indicated

When the user drags files over the gallery thumbnail grid, the `DropZoneComponent` SHALL show a full-overlay drop target with a dashed border and a "Drop images here" label. The overlay SHALL be hidden when the drag leaves or a drop occurs.

#### Scenario: Drag enters the gallery
- **GIVEN** the user drags one or more files over the `.thumbnail-grid-container`
- **WHEN** the `dragover` event fires
- **THEN** the drop overlay with dashed border and "Drop images here" label is visible

#### Scenario: Drag leaves the gallery
- **GIVEN** the drag overlay is visible
- **WHEN** the `dragleave` event fires
- **THEN** the drop overlay is hidden

---

### Requirement: The gallery grid refreshes after a completed upload batch

After all files in a drag-drop or file-picker batch have finished uploading (successfully or with errors), the `GalleryComponent` SHALL call `loadAssets()` to reload the current folder's asset list so newly uploaded images appear in the grid.

#### Scenario: Grid refreshes after batch completes
- **GIVEN** the user has dropped two files and both uploads have finished
- **WHEN** `DropZoneComponent` emits `uploadComplete`
- **THEN** `GalleryComponent` calls `loadAssets()` with `pageIndex = 0`; the two new thumbnails appear in the grid

---

### Requirement: Unauthenticated uploads are rejected

The upload endpoint SHALL require a valid JWT cookie. Requests without a valid session SHALL receive `401 Unauthorized`.

#### Scenario: Unauthenticated upload attempt
- **GIVEN** no valid JWT cookie is present
- **WHEN** a client calls `POST /api/assets/upload`
- **THEN** the response is `401 Unauthorized` and no file is written to disk
