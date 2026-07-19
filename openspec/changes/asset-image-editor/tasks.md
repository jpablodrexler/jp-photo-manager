## 1. Domain — Use case interface and DTO

- [ ] 1.1 Create `domain/port/in/asset/EditAssetUseCase.java` with `AssetResponse execute(long assetId, EditAssetRequest request)`
- [ ] 1.2 Create `domain/model/EditAssetRequest.java` record: `float brightness`, `float contrast`, `float hue`, `boolean replaceOriginal`

## 2. Application — Use case implementation

- [ ] 2.1 Create `application/usecase/asset/EditAssetUseCaseImpl.java` annotated with `@Service`
- [ ] 2.2 Inject `AssetRepositoryPort`, `StoragePort`
- [ ] 2.3 If all adjustments are zero, return the original `AssetResponse` without modification
- [ ] 2.4 Implement brightness + contrast via `RescaleOp`: `scaleFactor = 1f + request.contrast() / 100f`, `offset = request.brightness() * 2.55f`; apply with `rescaleOp.filter(srcImage, dstImage)`
- [ ] 2.5 Implement hue rotation: iterate pixels, convert with `Color.RGBtoHSB()`, rotate H by `request.hue() / 360f` (modulo 1.0), convert back with `Color.HSBtoRGB()`
- [ ] 2.6 If `replaceOriginal = false`: save to `<originalName>_edited.<ext>` in the same folder via `StoragePort.saveFile()`; create a new asset record and thumbnail; return new `AssetResponse`
- [ ] 2.7 If `replaceOriginal = true`: overwrite the original file; update the existing asset record; return the updated `AssetResponse`

## 3. HTTP adapter

- [ ] 3.1 Add `POST /api/assets/{id}/edit` to `AssetController` accepting `EditAssetRequest` body; return `AssetResponse`

## 4. Backend unit tests

- [ ] 4.1 Test that all-zero adjustments return the original asset without creating a new file
- [ ] 4.2 Test that brightness +50 produces a lighter output image (verify pixel values)
- [ ] 4.3 Test that `replaceOriginal = true` updates the existing asset instead of creating a new one
- [ ] 4.4 Test that the hue rotation produces a different image for hue = 90

## 5. Frontend — AssetEditorComponent

- [ ] 5.1 Create `features/gallery/asset-editor/asset-editor.component.ts` as a standalone component
- [ ] 5.2 Declare `@Input() asset!: Asset`; properties: `brightness = 0`, `contrast = 0`, `hue = 0`, `replaceOriginal = false`
- [ ] 5.3 Template: three `<mat-slider>` controls; `<img [style.filter]>` driven by the slider values; "Save as new asset" `MatButton` and "Replace original" `MatCheckbox`
- [ ] 5.4 "Save" click calls `assetService.editAsset(asset.assetId, { brightness, contrast, hue, replaceOriginal })` and emits `(saved)` output with the new `AssetResponse`
- [ ] 5.5 Add `editAsset(id: number, request: EditAssetRequest): Observable<Asset>` to `AssetService`
- [ ] 5.6 Add an "Edit" toolbar button in the viewer that shows `AssetEditorComponent` as an overlay

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
