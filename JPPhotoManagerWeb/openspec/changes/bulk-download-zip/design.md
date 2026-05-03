## Context

`StorageService.readFileBytes(String filePath)` already exists and returns `byte[]`, so no new storage abstraction is needed. `Asset.getFullPath()` returns `folder.getPath() + "/" + fileName`, giving the server-side absolute path to each file.

`AssetRepository.findAllById(Iterable<Long>)` is inherited from `JpaRepository` and resolves a list of asset IDs in a single query. Assets whose IDs are not found (e.g., deleted between selection and download) are silently omitted — the ZIP contains only the assets that exist.

`AssetController` already sets response content via `ResponseEntity<byte[]>` for thumbnails and images. The ZIP endpoint differs: it streams bytes directly into `HttpServletResponse.getOutputStream()` to avoid buffering the entire archive in memory, which could be multiple gigabytes for large selections.

The gallery selection toolbar uses `selectedAssets: Set<number>` and renders action buttons inside `@if (selectedCount > 0)` in the `mat-toolbar`. The "Download" entry fits naturally into the existing `#actionsMenu` `MatMenu`.

## Goals / Non-Goals

**Goals:**

- Stream a ZIP of selected assets directly to the browser without writing a temporary file on the server.
- Deduplicate ZIP entry names when multiple selected assets share the same filename.
- Cap the number of assets per request with a configurable server-side limit.
- Show a "Preparing download…" snack bar while the request is in flight.
- Trigger a browser save-file dialog for `photos.zip` on success.

**Non-Goals:**

- Preserving folder structure inside the ZIP (all entries are flat at the root level).
- Resumable or chunked downloads.
- Server-side progress reporting for the ZIP assembly.
- Downloading assets across multiple pages in one click (only the current selection is downloaded).

## Decisions

### 1. Stream directly to `OutputStream` — facade accepts `OutputStream`, not `HttpServletResponse`

**Decision:** The facade method signature is `void downloadAssets(List<Long> assetIds, OutputStream out) throws IOException`. `AssetController` sets the response headers (`Content-Type`, `Content-Disposition`) and then passes `response.getOutputStream()` to the facade. The facade wraps it in a `ZipOutputStream`, writes entries, and closes the stream.

**Rationale:** Passing `HttpServletResponse` into the application layer would couple the facade to the Servlet API. Passing `OutputStream` keeps the facade testable without a mock HTTP context — unit tests can pass a `ByteArrayOutputStream` directly. The controller remains the only layer that touches HTTP-specific types.

### 2. Flat ZIP with `{assetId}_{fileName}` deduplication

**Decision:** Each ZIP entry is named by the asset's `fileName`. Before writing, the facade builds a `Set<String>` of seen names; if a collision is detected, the duplicate entry is named `{assetId}_{fileName}` instead. The first occurrence keeps its original name.

**Rationale:** Flat archives are simpler to open and match user expectations (download a selection, not a folder tree). Prefixing with the asset ID on collision is deterministic and avoids silent data loss. The full folder path is not used to avoid exposing server filesystem structure in the archive.

### 3. Missing or unreadable files are skipped — no partial-failure error

**Decision:** Inside the `ZipOutputStream` write loop, each `storageService.readFileBytes(path)` call is wrapped in a try-catch. `IOException`s are logged as warnings and the loop continues with the next asset. The ZIP is still returned even if some files could not be read.

**Rationale:** The selected assets may include files that have been moved, renamed, or deleted on disk since the last catalog run. Failing the entire download because one file is missing is a poor user experience. The browser receives a valid ZIP containing all readable files; the user can re-catalog to sync the DB with disk state.

### 4. Server-side size cap via `@Value("${photomanager.max-download-assets:500}")`

**Decision:** `AssetController.downloadAssets()` reads the configured cap and returns `400 Bad Request` if `request.getAssetIds().size() > maxDownloadAssets` before calling the facade. The `DownloadAssetsRequest` DTO also has a Bean Validation `@Size(max = 500)` annotation as a secondary guard.

**Rationale:** Streaming 10 000 images in a single request would consume unbounded server memory and connection time. A cap prevents accidental or malicious abuse. The `@Value` default of 500 matches the `@Size` annotation so both limits are consistent and can be raised together via `application.yml`.

### 5. Frontend uses `responseType: 'blob'` and an ephemeral `<a>` element

**Decision:** `AssetService.downloadAssets(assetIds)` calls `this.http.post('/api/assets/download', { assetIds }, { responseType: 'blob' })`. `GalleryComponent.downloadSelected()` subscribes, calls `URL.createObjectURL(blob)`, creates a temporary `<a>` element with `href` set to the object URL and `download="photos.zip"`, appends it to the document body, programmatically clicks it, then removes the element and calls `URL.revokeObjectURL(href)`.

**Rationale:** `responseType: 'blob'` is the standard Angular HTTP approach for binary responses; it avoids base64 encoding. The ephemeral anchor element is the only cross-browser way to trigger a save dialog with a custom filename from a Blob URL without a server redirect.

## Data Flow

```
User selects N assets → clicks "Download" in actions menu
  → GalleryComponent.downloadSelected()
    → snackBar.open("Preparing download…")
    → assetService.downloadAssets(Array.from(selectedAssets))
      → POST /api/assets/download { assetIds: [...] }
        → AssetController.downloadAssets(@Valid body, HttpServletResponse)
          → validate size ≤ max-download-assets → 400 if exceeded
          → response.setContentType("application/zip")
          → response.setHeader("Content-Disposition", "attachment; filename=\"photos.zip\"")
          → facade.downloadAssets(assetIds, response.getOutputStream())
            → assetRepository.findAllById(assetIds) → List<Asset>
            → build deduplication map
            → for each asset:
                → storageService.readFileBytes(asset.getFullPath()) → byte[]
                → zipOut.putNextEntry(new ZipEntry(entryName))
                → zipOut.write(bytes)
                → zipOut.closeEntry()
                (on IOException: log warning, continue)
            → zipOut.close()
      → response body is the ZIP byte stream
    → Observable<Blob> resolves with blob
    → snackBar.dismiss()
    → URL.createObjectURL(blob) → objectUrl
    → <a href=objectUrl download="photos.zip">.click()
    → URL.revokeObjectURL(objectUrl)
```

## File Change List

**New files:**

- `backend/.../api/dto/DownloadAssetsRequest.java` — Lombok `@Data` class with `@NotEmpty @Size(max = 500) List<Long> assetIds`
- `backend/.../test/.../AssetControllerDownloadTest.java` — `@WebMvcTest` covering the download endpoint

**Modified files:**

- `backend/.../application/PhotoManagerFacade.java` — add `void downloadAssets(List<Long> assetIds, OutputStream out) throws IOException`
- `backend/.../application/PhotoManagerFacadeImpl.java` — implement `downloadAssets`; inject `StorageService storageService` (already present); build ZIP stream
- `backend/.../api/AssetController.java` — add `POST /api/assets/download` handler; inject `@Value("${photomanager.max-download-assets:500}") int maxDownloadAssets`
- `backend/.../resources/application.yml` — add `photomanager.max-download-assets: 500`
- `frontend/src/app/core/services/asset.service.ts` — add `downloadAssets(assetIds: number[]): Observable<Blob>`
- `frontend/src/app/features/gallery/gallery.component.ts` — add `downloadSelected()` method
- `frontend/src/app/features/gallery/gallery.component.html` — add "Download" menu item inside `#actionsMenu`
