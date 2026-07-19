## 1. Database migration

- [x] 1.1 Create `src/main/resources/db/migration/V14__add_asset_is_video.sql` with the following DDL:
  ```sql
  ALTER TABLE assets ADD COLUMN is_video BOOLEAN NOT NULL DEFAULT FALSE;
  ```

## 2. Backend — Domain layer

- [x] 2.1 Add `boolean isVideo` field to `Asset.java` (domain model) with `@Builder.Default` value `false`
- [x] 2.2 Add `boolean isVideoFile(String fileName);` to the `StoragePort` interface

## 3. Backend — Infrastructure — StorageServiceAdapter

- [x] 3.1 Add private `boolean isVideoFile(String fileName)` implementation in `StorageServiceAdapter`: return `true` if the lowercased extension is `.mp4`, `.mov`, or `.mkv`
- [x] 3.2 Implement `StoragePort.isVideoFile(String fileName)` in `StorageServiceAdapter` by delegating to the private helper
- [x] 3.3 Extend `listFiles()` to also accept video files: change the filter to `p -> isImageFile(name) || isVideoFile(name)`
- [x] 3.4 Add `private byte[] generateVideoThumbnail(String filePath, int maxWidth, int maxHeight) throws IOException` to `StorageServiceAdapter`:
  - Create a temp file via `Files.createTempFile("vthumb_", ".jpg")`
  - Build the command: `["ffmpeg", "-i", filePath, "-ss", "00:00:01", "-vframes", "1", "-vf", "scale=" + maxWidth + ":" + maxHeight + ":force_original_aspect_ratio=decrease", "-y", tempFile.toString()]`
  - Run via `ProcessBuilder`, redirect `stderr` to `DISCARD`, wait for the process to exit
  - If exit code is non-zero, throw `IOException` with the exit code in the message
  - Read `Files.readAllBytes(tempFile)` and return them
  - In `finally`, call `Files.deleteIfExists(tempFile)`
- [x] 3.5 Update `generateThumbnail(String filePath, int maxWidth, int maxHeight)` in `StorageServiceAdapter`: if `isVideoFile(filePath)`, call `generateVideoThumbnail()`; otherwise use the existing `loadImage()` path; wrap `generateVideoThumbnail()` with `try/catch` that logs a `warn` and rethrows
- [x] 3.6 Update `getImageRotation(String filePath)` in `StorageServiceAdapter`: if `isVideoFile(filePath)`, return `ImageRotation.ROTATE_0` immediately without reading the file
- [x] 3.7 Update `getExifMetadata(String filePath)` in `StorageServiceAdapter`: if `isVideoFile(filePath)`, return `new ExifMetadata(null, null, null, null, null, null, null, null, null, null, null, null)` immediately

## 4. Backend — Infrastructure — CatalogFolderServiceImpl

- [x] 4.1 In `CatalogFolderServiceImpl.createAsset()`, after constructing the `Asset` object, call `asset.setIsVideo(storageService.isVideoFile(fileName))`

## 5. Backend — Infrastructure — Persistence layer

- [x] 5.1 Add `boolean isVideo` field (default `false`) to `AssetEntity.java` JPA entity, annotated with `@Column(name = "is_video", nullable = false)`
- [x] 5.2 Update `AssetEntityMapper.java` (MapStruct) to map `isVideo` from `AssetEntity` to `Asset` domain model and back

## 6. Backend — API layer

- [x] 6.1 Add `boolean isVideo` field to the `AssetDto` HTTP response DTO (or equivalent response record/class in `infrastructure/web/dto/`)
- [x] 6.2 Update `AssetWebMapper.java` (MapStruct) to map `isVideo` from `Asset` domain model to `AssetDto`

## 7. Backend — Docker

- [x] 7.1 In `JPPhotoManagerWeb/backend/Dockerfile`, add `RUN apk add --no-cache ffmpeg` to the runtime stage (`eclipse-temurin:21-jre-alpine`), placed after the `addgroup`/`adduser` line and before `WORKDIR /app`

## 8. Backend — Tests

- [x] 8.1 Create `StorageServiceAdapterVideoTest` (unit, `@ExtendWith(MockitoExtension.class)`):
  - Assert `isVideoFile("clip.mp4")`, `isVideoFile("video.MOV")`, and `isVideoFile("rec.MKV")` return `true`
  - Assert `isVideoFile("photo.jpg")` returns `false`
  - Assert `getImageRotation()` returns `ROTATE_0` for a `.mp4` path without reading the file (spy or verify no `Imaging` call)
  - Assert `getExifMetadata()` returns an all-null `ExifMetadata` record for a `.mov` path
- [x] 8.2 Create `StorageServiceAdapterVideoThumbnailTest` (integration-style, requires FFmpeg on the test runner):
  - Assert `generateThumbnail()` for a small `.mp4` test fixture returns a non-empty byte array
  - Assert the returned bytes begin with the JPEG magic bytes (`0xFF 0xD8 0xFF`)
  - Place a minimal test `.mp4` file (a few KB) under `src/test/resources/` or reuse one from the test resources if available
- [x] 8.3 Create `CatalogFolderServiceVideoTest` (unit, `@ExtendWith(MockitoExtension.class)`):
  - Mock `storagePort.isVideoFile("clip.mp4")` to return `true`
  - Call `createAsset()` for the `.mp4` file
  - Assert the saved `Asset` has `isVideo = true`
- [x] 8.4 Create `AssetControllerVideoTest` (`@WebMvcTest`):
  - Mock `GetAssetsUseCase` to return an `Asset` with `isVideo = true`
  - Assert `GET /api/assets?folderPath=...` response JSON contains `"isVideo": true`
  - Assert a second asset with `isVideo = false` returns `"isVideo": false`
- [x] 8.5 Run `mvn test -Dtest=StorageServiceAdapterVideoTest,CatalogFolderServiceVideoTest,AssetControllerVideoTest` and confirm all new tests pass

## 9. Frontend — Model and service

- [x] 9.1 Add `isVideo: boolean` field to the `Asset` interface in `frontend/src/app/core/models/asset.model.ts`

## 10. Frontend — GalleryComponent

- [x] 10.1 In `gallery.component.html`, inside the `*cdkVirtualFor` thumbnail row, wrap the `<img>` in a `<div class="thumb-wrapper">` and add `@if (asset.isVideo) { <mat-icon class="video-overlay-icon">play_circle</mat-icon> }` as a sibling of the `<img>` inside the wrapper
- [x] 10.2 In `gallery.component.html`, in the viewer section (`@if (viewMode === 'viewer' && currentViewerAsset)`), replace the bare `<img [src]="currentViewerAsset.imageUrl" ...>` with:
  ```html
  @if (currentViewerAsset.isVideo) {
    <video [src]="currentViewerAsset.imageUrl" controls class="viewer-video"></video>
  } @else {
    <img [src]="currentViewerAsset.imageUrl" [alt]="currentViewerAsset.fileName" [style.transform]="'scale(' + viewerZoom + ')'"/>
  }
  ```
- [x] 10.3 Add `.thumb-wrapper` to `gallery.component.scss` with `position: relative; display: inline-block`
- [x] 10.4 Add `.video-overlay-icon` to `gallery.component.scss`:
  ```scss
  .video-overlay-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: rgba(255, 255, 255, 0.85);
    font-size: 28px;
    pointer-events: none;
  }
  ```
- [x] 10.5 Add `.viewer-video` to `gallery.component.scss` with `max-width: 100%; max-height: 100%; object-fit: contain`
- [x] 10.6 Verify `MatIconModule` is already in `GalleryComponent`'s `imports` array (it is — no change needed)

## 11. Frontend — Tests

- [x] 11.1 Add a test to `gallery.component.cy.ts` asserting that when a video asset (`isVideo: true`) is mounted, a `.video-overlay-icon` element exists inside the `.asset-list-row`
- [x] 11.2 Add a test to `gallery.component.cy.ts` asserting that when a video asset is opened in the viewer (call `component.openViewer(index)` where the asset has `isVideo: true`), the DOM contains a `video` element and does not contain the `img.viewer-image` element (or equivalent selector)
- [x] 11.3 Add a test to `gallery.component.cy.ts` asserting that when an image asset (`isVideo: false`) is opened in the viewer, an `img` element is present and no `video` element is present
- [x] 11.4 Run `npm test` and confirm all new tests pass

## 12. Testing and Commit

- [x] 12.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 12.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 12.3 Commit all changes (only after both test suites pass)
