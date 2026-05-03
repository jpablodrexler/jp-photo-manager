## 1. Backend — DTO

- [ ] 1.1 Create `DownloadAssetsRequest.java` in `api/dto/`; use Lombok `@Data`; add field `@NotEmpty @Size(max = 500) List<Long> assetIds`

## 2. Backend — Application layer

- [ ] 2.1 Add `void downloadAssets(List<Long> assetIds, java.io.OutputStream out) throws IOException;` to the `PhotoManagerFacade` interface
- [ ] 2.2 Implement `PhotoManagerFacadeImpl.downloadAssets`: call `assetRepository.findAllById(assetIds)` to get `List<Asset> assets`
- [ ] 2.3 Build a deduplication map: iterate `assets`, add each `asset.getFileName()` to a `Set<String> seenNames`; for any asset whose `fileName` is already in the set, plan its ZIP entry name as `asset.getAssetId() + "_" + asset.getFileName()`; the first occurrence keeps its original `fileName`
- [ ] 2.4 Wrap the provided `OutputStream out` in a `ZipOutputStream zipOut = new ZipOutputStream(out)`; for each asset in order: call `storageService.readFileBytes(asset.getFullPath())` inside a try-catch `IOException`; on success create a `ZipEntry(entryName)`, call `zipOut.putNextEntry(entry)`, write the bytes, call `zipOut.closeEntry()`; on `IOException` log a warning (`log.warn("Skipping unreadable asset {}: {}", asset.getAssetId(), e.getMessage())`) and `continue`
- [ ] 2.5 After the loop call `zipOut.finish()` (do **not** call `zipOut.close()` — closing it would close the response's output stream prematurely; use `finish()` to flush the ZIP central directory without closing the underlying stream)

## 3. Backend — API layer

- [ ] 3.1 Add `@Value("${photomanager.max-download-assets:500}") private int maxDownloadAssets;` field to `AssetController`
- [ ] 3.2 Add `POST /api/assets/download` handler: `downloadAssets(@Valid @RequestBody DownloadAssetsRequest request, HttpServletResponse response) throws IOException`; check `request.getAssetIds().size() > maxDownloadAssets` and return `ResponseEntity.badRequest().build()` (by writing to the response and returning early) if true; otherwise call `response.setContentType("application/zip")`, `response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"photos.zip\"")`, then `facade.downloadAssets(request.getAssetIds(), response.getOutputStream())`; the handler return type is `void` (response is written directly)
- [ ] 3.3 Add `photomanager.max-download-assets: 500` to `src/main/resources/application.yml` under the `photomanager:` block

## 4. Backend — Tests

- [ ] 4.1 Create `AssetControllerDownloadTest` (`@WebMvcTest(AssetController.class)`): mock `PhotoManagerFacade` and `ThumbnailStorageService`; assert `POST /api/assets/download` with a valid body of three asset IDs calls `facade.downloadAssets(...)` and returns `200` with `Content-Type: application/zip` and `Content-Disposition: attachment; filename="photos.zip"`
- [ ] 4.2 Add test: request body with an empty `assetIds` list (violates `@NotEmpty`) returns `400 Bad Request`
- [ ] 4.3 Add test: request with `assetIds` list size exceeding `max-download-assets` returns `400 Bad Request`
- [ ] 4.4 Add unit test for `PhotoManagerFacadeImpl.downloadAssets`: mock `AssetRepository` returning two assets and `StorageService.readFileBytes` returning byte arrays; call `downloadAssets` with a `ByteArrayOutputStream`; parse the resulting ZIP and assert both entries are present with the correct names
- [ ] 4.5 Add unit test for the deduplication logic: two assets with the same `fileName`; assert the second entry is named `{assetId}_{fileName}`
- [ ] 4.6 Add unit test for skip-on-error: one asset's `readFileBytes` throws `IOException`; assert the other asset's entry is still present in the ZIP and no exception propagates from `downloadAssets`
- [ ] 4.7 Run `mvn test` and confirm all tests pass

## 5. Frontend — AssetService

- [ ] 5.1 Add `downloadAssets(assetIds: number[]): Observable<Blob>` to `asset.service.ts`; call `this.http.post('/api/assets/download', { assetIds }, { responseType: 'blob' as 'json' })` and return the observable; cast the return type as `Observable<Blob>`

## 6. Frontend — GalleryComponent

- [ ] 6.1 Add `downloadSelected()` method to `GalleryComponent`:
  - Store the current selection: `const ids = Array.from(this.selectedAssets)`
  - Open a snack bar: `const snackRef = this.snackBar.open('Preparing download…', undefined, { duration: 0 })`
  - Call `this.assetService.downloadAssets(ids).subscribe({ next: blob => { ... }, error: () => { ... } })`
  - On `next`: dismiss the snack bar with `snackRef.dismiss()`; create `const url = URL.createObjectURL(blob)`; create an `HTMLAnchorElement`, set `a.href = url` and `a.download = 'photos.zip'`; append to `document.body`, call `a.click()`, remove from `document.body`, call `URL.revokeObjectURL(url)`
  - On `error`: dismiss the snack bar; show a new error snack bar `'Failed to download assets'`
- [ ] 6.2 Add a "Download" menu item to the `#actionsMenu` in `gallery.component.html` inside the `@if (selectedCount > 0)` block:
  ```html
  <button mat-menu-item (click)="downloadSelected()">
    <mat-icon>download</mat-icon> Download
  </button>
  ```

## 7. Frontend — Tests

- [ ] 7.1 In `gallery.component.cy.ts` (or a new Cypress test), mount `GalleryComponent` with stubbed `AssetService` where `downloadAssets` returns `of(new Blob())`; select one asset; click "Download" from the actions menu; assert `assetService.downloadAssets` was called with the correct asset ID array
- [ ] 7.2 Add test: when `downloadAssets` returns an error, assert the snack bar shows `'Failed to download assets'`
- [ ] 7.3 Run `npm test` and confirm all tests pass
