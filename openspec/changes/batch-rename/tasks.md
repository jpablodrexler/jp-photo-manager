## 1. Backend: Domain Port and Use Case

- [x] 1.1 Create `domain/port/in/asset/RenameAssetsUseCase.java` — single method `RenameAssetsResult execute(Long[] assetIds, String pattern, boolean applied)`; define `RenameAssetsResult` as a record in `application/dto/` with fields `List<RenamePreview> previews` and `boolean applied`, where `RenamePreview` is a record with `Long assetId`, `String oldName`, `String newName`
- [x] 1.2 Create `application/usecase/asset/RenameAssetsUseCaseImpl.java` annotated `@Service @Transactional`; inject `AssetRepository` and `StoragePort`; implement token resolution for `{date:FORMAT}`, `{index:PAD}`, `{original}`, and `{ext}`; validate the `{date:...}` format string against an allowlist of safe characters before passing it to `DateTimeFormatter.ofPattern()`
- [x] 1.3 Implement collision detection in `RenameAssetsUseCaseImpl`: before applying any rename, verify no two assets in the batch resolve to the same name and no existing asset in the folder already holds any target name; throw `IllegalArgumentException("ASSET_NAME_COLLISION")` on collision
- [x] 1.4 When `applied` is `true`, call `storagePort.moveFile(oldPath, newPath)` for each asset and update `asset.setFileName(newName)`; save with `assetRepository.save(asset)`; roll back the full transaction on any `IOException`
- [x] 1.5 Write unit tests for `RenameAssetsUseCaseImpl` covering: all four token types, `{date:...}` validation rejection, within-batch collision, existing-asset collision, preview-only (no file mutation), successful apply (file mutation + DB update), partial failure rolls back

## 2. Backend: HTTP Adapter

- [x] 2.1 Create `infrastructure/web/dto/RenameAssetsRequest.java` as a record with `@NotEmpty Long[] assetIds`, `@NotBlank String pattern`, and `boolean applied`
- [x] 2.2 Create `infrastructure/web/dto/RenameAssetsResponse.java` as a record with `List<RenamePreviewDto> previews` and `boolean applied`; create `RenamePreviewDto` record with `Long assetId`, `String oldName`, `String newName`
- [x] 2.3 Add `@PostMapping("/rename")` handler to `AssetController`; inject `RenameAssetsUseCase`; delegate to `useCase.execute(request.assetIds(), request.pattern(), request.applied())`; map `RenameAssetsResult` to `RenameAssetsResponse`; catch `IllegalArgumentException` with message `"ASSET_NAME_COLLISION"` and return `400 Bad Request` with an error body
- [x] 2.4 Write `@WebMvcTest` for `AssetController` covering `POST /api/assets/rename`: preview-only success, apply success, collision `400`, empty asset list `400`, invalid date format `400`

## 3. Frontend: Service Method

- [x] 3.1 Add `RenameAssetsRequest`, `RenamePreview`, and `RenameAssetsResponse` interfaces to `core/models/asset.model.ts`
- [x] 3.2 Add `renameAssets(assetIds: number[], pattern: string, applied: boolean): Observable<RenameAssetsResponse>` to `AssetService`, calling `POST /api/assets/rename`
- [x] 3.3 Write a Cypress component test for `AssetService.renameAssets()` verifying the request body and that the response is mapped correctly

## 4. Frontend: Batch Rename Dialog Component

- [x] 4.1 Create `features/gallery/batch-rename-dialog/batch-rename-dialog.component.ts` as a standalone `MatDialog` component; inject `MAT_DIALOG_DATA` typed as `{ assetIds: number[], assetCount: number }`; inject `AssetService` and `MatDialogRef`
- [x] 4.2 Add `pattern = ''` state, `previews: RenamePreview[] = []`, `previewError: string | null = null`, `isApplying = false`; implement `onPatternChange()` with a 400 ms debounce that calls `AssetService.renameAssets(..., false)` and updates `previews` / `previewError`
- [x] 4.3 Implement the template: pattern `MatInput` with a token hint line, `MatTable` with columns `oldName` and `newName`, error message row when `previewError` is set, Apply button (disabled when `!pattern || !!previewError || isApplying`), Cancel button
- [x] 4.4 Implement `applyRename()`: set `isApplying = true`, call `AssetService.renameAssets(..., true)`, close dialog with `{ success: true, count: assetIds.length }` on success, or close with `{ success: false, error }` on error
- [x] 4.5 Add SCSS: set `min-width: 520px` on the dialog, constrain the preview table with `max-height: 320px; overflow-y: auto`
- [x] 4.6 Write Cypress component tests for `BatchRenameDialogComponent` covering: empty pattern disables Apply, valid pattern enables Apply, collision error disables Apply and shows error row, Cancel closes without result, Apply success closes with result, Apply error closes with error

## 5. Frontend: Gallery Integration

- [x] 5.1 Add `renameSelectedAssets()` method to `GalleryComponent`: open `BatchRenameDialogComponent` with `{ assetIds: [...selectedAssets], assetCount: selectedAssets.size }`; on dialog close with `{ success: true }` call `loadAssets()`, clear `selectedAssets`, show snackbar "Renamed N asset(s)"; on `{ success: false }` show error snackbar "Rename failed: [error]"
- [x] 5.2 Add **Rename selected…** item to the `#actionsMenu` in the gallery toolbar template calling `renameSelectedAssets()`; show only when `viewMode === 'thumbnails'`
- [x] 5.3 Add `BatchRenameDialogComponent` to `GalleryComponent` imports array
- [x] 5.4 Write Cypress component tests for the gallery rename flow: successful rename shows snackbar and triggers reload; failed rename shows error snackbar and leaves gallery unchanged

## 6. Testing and Commit

- [x] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 6.3 Commit all changes (only after both test suites pass)
