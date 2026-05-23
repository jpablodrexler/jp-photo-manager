## 1. Domain — Use case interface and format enum

- [ ] 1.1 Create `domain/enums/SocialMediaFormat.java` enum with 12 values: `INSTAGRAM_POST(1080,1080)`, `INSTAGRAM_PORTRAIT(1080,1350)`, `INSTAGRAM_LANDSCAPE(1080,566)`, `INSTAGRAM_STORY(1080,1920)`, `INSTAGRAM_PROFILE(110,110)`, `FACEBOOK_POST(1200,630)`, `FACEBOOK_PROFILE(170,170)`, `LINKEDIN_POST(1200,627)`, `LINKEDIN_PROFILE(400,400)`, `TWITTER_POST(1600,900)`, `TWITTER_PROFILE(400,400)`, `TWITTER_HEADER(1500,500)`; each with `targetWidth` and `targetHeight` fields; mark profile formats with `boolean isCircle`
- [ ] 1.2 Create `domain/port/in/asset/CropAssetUseCase.java` with `AssetResponse execute(long assetId, CropAssetRequest request)`
- [ ] 1.3 Create `domain/model/CropAssetRequest.java` record: `String formatKey`, `int x`, `int y`, `int width`, `int height`

## 2. Application — Use case implementation

- [ ] 2.1 Create `application/usecase/asset/CropAssetUseCaseImpl.java` annotated with `@Service`
- [ ] 2.2 Inject `AssetRepositoryPort`, `StoragePort`
- [ ] 2.3 Load original image via `StoragePort.readImage(asset.filePath)`; call `BufferedImage.getSubimage(x, y, width, height)`
- [ ] 2.4 Scale subimage to format target dimensions via `Graphics2D.drawImage(subimage, 0, 0, targetW, targetH, null)` on a new `BufferedImage(targetW, targetH, TYPE_INT_RGB)`
- [ ] 2.5 Save output as `<originalName>_<formatKey>.jpg` via `StoragePort.saveFile()`; create new asset record and thumbnail; return `AssetResponse`

## 3. HTTP adapter

- [ ] 3.1 Add `POST /api/assets/{id}/crop` to `AssetController` accepting `CropAssetRequest` body; return `AssetResponse`

## 4. Backend unit tests

- [ ] 4.1 Test that `CropAssetUseCaseImpl` produces an image of the exact format target dimensions
- [ ] 4.2 Test that the saved file name includes the format key suffix
- [ ] 4.3 Test that out-of-bounds coordinates throw `ValidationException`

## 5. Frontend — SocialMediaCropComponent

- [ ] 5.1 Create `features/gallery/social-media-crop/social-media-crop.component.ts` as a standalone component
- [ ] 5.2 Define TypeScript `SOCIAL_MEDIA_FORMATS` constant matching the Java enum (12 presets with `key`, `targetWidth`, `targetHeight`, `isCircle`)
- [ ] 5.3 Template: `<canvas>` element; `<mat-select>` for format; "Save & Download" and "Cancel" buttons
- [ ] 5.4 `ngAfterViewInit`: draw image on canvas; initialize crop box at maximum centered fit
- [ ] 5.5 `(mousedown)` / `(mousemove)` / `(mouseup)` on canvas: detect hit on crop box interior (move) vs corner handle (resize while maintaining ratio)
- [ ] 5.6 For `isCircle` formats: draw `ctx.arc()` outline inside crop box on each redraw
- [ ] 5.7 On format change: recalculate crop box position to maximum centered fit
- [ ] 5.8 On "Save & Download": compute original pixel coordinates; call `assetService.cropAsset(assetId, request)`; on response, `window.open('/api/assets/' + newAsset.assetId + '/image', '_blank')`
- [ ] 5.9 Add "Crop" scissors toolbar button to viewer; renders `SocialMediaCropComponent` as overlay
- [ ] 5.10 Add `cropAsset(assetId: number, request: CropAssetRequest): Observable<Asset>` to `AssetService`

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
