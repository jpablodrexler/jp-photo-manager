## Context

`CatalogAssetsServiceImpl.createAsset(directoryPath, fileName)` delegates directly to `CatalogFolderService.createAsset`, which performs the full single-file indexing pipeline: saves the file to disk (it assumes the file already exists at `directoryPath/fileName`), computes the SHA-256 hash, generates a 200×150 thumbnail, creates a `Folder` row if needed, creates an `Asset` row, and returns the saved entity. This is the exact entry point needed for upload indexing — no new domain logic is required.

`StorageService` has `copyFile(String sourcePath, String destinationPath)` that copies a file and creates parent directories. `FolderRepository.existsByPath(String path)` can validate that the target folder is a known catalog folder before writing to it.

`AssetController` already uses a `toDto(Asset)` private helper to map entities to `AssetDto`. The new upload endpoint reuses this helper and the existing `AssetDto`.

On the frontend, `AssetService` already issues `DELETE` and `POST` requests and `GalleryComponent` already calls `loadAssets()` to refresh the grid after mutations. The `HttpClient` available in Angular 19 supports `reportProgress: true` for upload progress events via `observe: 'events'`.

Spring Boot's `MultipartFile` support is already enabled — no additional configuration is required for multipart uploads.

## Goals / Non-Goals

**Goals:**

- Accept one or more image files from the browser and save them to the currently selected catalog folder on the server filesystem.
- Index each uploaded file immediately using the existing `CatalogFolderService.createAsset` pipeline and return the new `AssetDto`.
- Validate that the target folder exists in the catalog before writing to disk.
- Validate file type server-side (JPEG, PNG, GIF, BMP, TIFF, WEBP only).
- Show per-file upload progress and success/error feedback in the gallery.
- Refresh the gallery grid after all uploads in a batch complete.

**Non-Goals:**

- Creating new catalog folders via upload (the target folder must already be cataloged).
- Uploading non-image files (video, PDF, etc.).
- Resumable or chunked uploads.
- Server-side virus scanning.
- Uploading to a folder that is not under the configured catalog root.

## Decisions

### 1. Reuse `CatalogFolderService.createAsset` — no new indexing logic

**Decision:** After writing the uploaded bytes to the target folder via `StorageService.copyFile`, the facade calls `catalogFolderService.createAsset(folderPath, fileName)` to index the file. This is the same path used by the scheduled catalog when it encounters a new file.

**Rationale:** `createAsset` already handles hash computation, thumbnail generation, `Folder` row creation, and `Asset` row persistence. Duplicating any of that logic in the upload path would create a maintenance burden and risk divergence. The upload path is conceptually a single-file catalog run.

### 2. Write multipart bytes via temp file → `StorageService.copyFile`

**Decision:** `PhotoManagerFacadeImpl.uploadAsset` calls `multipartFile.transferTo(tempFile)` to write the uploaded bytes to `java.io.tmpdir/<uuid>_<originalFilename>`, then calls `storageService.copyFile(tempFile.getAbsolutePath(), targetPath)` to place it in the catalog folder. The temp file is deleted in a `finally` block.

**Rationale:** `StorageService.copyFile` creates parent directories, handles `StandardCopyOption.REPLACE_EXISTING`, and is already tested. Using `transferTo` to a temp file first avoids holding the multipart memory buffer open during the copy and keeps `StorageService` as the single point of filesystem writes.

**Alternative considered:** Calling `multipartFile.transferTo(targetFile)` directly. Rejected because it bypasses `StorageService`, breaking the abstraction boundary and making it impossible to mock filesystem writes in unit tests.

### 3. Validate folder existence before writing

**Decision:** `PhotoManagerFacadeImpl.uploadAsset` calls `folderRepository.existsByPath(folderPath)`. If the folder is not a cataloged path, it throws `FolderNotFoundException` which the controller maps to `404 Not Found`.

**Rationale:** Accepting uploads to arbitrary filesystem paths would be a path-traversal security risk. Restricting writes to already-cataloged folders ensures the upload path never leaves the managed catalog tree.

### 4. Server-side content-type validation by MIME type and extension

**Decision:** `AssetController.uploadAsset` validates both `multipartFile.getContentType()` against an allowlist (`image/jpeg`, `image/png`, `image/gif`, `image/bmp`, `image/tiff`, `image/webp`) and the original filename extension. If either check fails, the endpoint returns `415 Unsupported Media Type` immediately before writing to disk.

**Rationale:** Client-supplied MIME types can be spoofed. Checking both content-type header and extension provides defence in depth without requiring full file signature inspection (which Apache Commons Imaging would handle implicitly during thumbnail generation if needed).

### 5. `DropZoneComponent` as a standalone Angular component in `features/gallery/`

**Decision:** The drag-and-drop overlay and file input are encapsulated in a new `DropZoneComponent` (not in `GalleryComponent` directly), placed in `features/gallery/drop-zone/`. It takes `@Input() folderPath: string` and emits `@Output() uploadComplete` when all files in a batch finish. `GalleryComponent` hosts `DropZoneComponent` and calls `loadAssets()` on `uploadComplete`.

**Rationale:** Keeping the upload logic in its own component isolates the drag/drop state machine and makes the Cypress component test for upload independent of the gallery grid test. The co-location in `features/gallery/` (not `shared/`) reflects that uploading is a gallery-specific interaction, not a reusable widget.

### 6. Sequential uploads — one file at a time per batch

**Decision:** When multiple files are dropped or selected, the `DropZoneComponent` uploads them sequentially (each request completes before the next starts) and shows an individual `MatProgressBar` row per file.

**Rationale:** Parallel uploads would require managing multiple in-flight `HttpClient` subscriptions and a shared completion counter. Sequential uploads keep the progress state simple, avoid saturating the server connection, and are adequate for the typical batch size of a drag-and-drop operation.

## Data Flow

```
User drags files onto gallery / clicks Upload button
  → DropZoneComponent receives File[]
    → for each file (sequential):
        assetService.uploadAsset(folderPath, file)     [frontend]
          → POST /api/assets/upload                   [multipart/form-data]
            → AssetController.uploadAsset()           [backend]
              → validate content-type + extension     → 415 if invalid
              → facade.uploadAsset(folderPath, file)
                → folderRepository.existsByPath()     → 404 if unknown
                → file.transferTo(tempFile)           [disk: tmpdir]
                → storageService.copyFile(temp, dest) [disk: catalog folder]
                → catalogFolderService.createAsset()
                  → computeHash + generateThumbnail + persist Asset
                → delete tempFile
                → return Asset
              → toDto(asset)
            → 201 Created {AssetDto}
          → Observable<HttpEvent<AssetDto>> (progress events)
        → update per-file progress bar
        → on complete: mark file as done
    → after all files: emit uploadComplete
  → GalleryComponent.loadAssets()                     [refresh grid]
```

## File Change List

**New files:**

- `backend/.../api/dto/UploadRequest.java` — thin record with `@NotBlank folderPath`; passed alongside `@RequestPart MultipartFile file`
- `backend/.../api/exception/FolderNotFoundException.java` — unchecked exception thrown when `folderPath` is not in the catalog
- `backend/.../test/.../AssetControllerUploadTest.java` — `@WebMvcTest` for the upload endpoint
- `backend/.../test/.../UploadAssetIntegrationTest.java` — `@SpringBootTest` end-to-end upload test
- `frontend/src/app/features/gallery/drop-zone/drop-zone.component.ts`
- `frontend/src/app/features/gallery/drop-zone/drop-zone.component.html`
- `frontend/src/app/features/gallery/drop-zone/drop-zone.component.scss`
- `frontend/src/app/features/gallery/drop-zone/drop-zone.component.cy.ts`

**Modified files:**

- `backend/.../application/PhotoManagerFacade.java` — add `Asset uploadAsset(String folderPath, MultipartFile file) throws IOException`
- `backend/.../application/PhotoManagerFacadeImpl.java` — implement `uploadAsset`
- `backend/.../api/AssetController.java` — add `POST /api/assets/upload` handler
- `frontend/src/app/core/services/asset.service.ts` — add `uploadAsset(folderPath, file)`
- `frontend/src/app/features/gallery/gallery.component.ts` — import `DropZoneComponent`; add `isDragging` flag; wire `dragover`/`dragleave`/`drop` host events; handle `uploadComplete`
- `frontend/src/app/features/gallery/gallery.component.html` — add `<app-drop-zone>` alongside the thumbnail grid
- `frontend/src/app/features/gallery/gallery.component.scss` — add drag-active state style on `.thumbnail-grid-container`
