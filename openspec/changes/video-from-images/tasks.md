## 1. Domain — Use case interface and request model

- [ ] 1.1 Create `domain/port/in/asset/CreateVideoFromImagesUseCase.java` with `void execute(CreateVideoRequest request, Consumer<VideoCreationNotification> consumer)`
- [ ] 1.2 Create `domain/model/CreateVideoRequest.java` record: `List<Long> assetIds`, `int slideDuration`, `Long musicAssetId`, `String outputFolder`
- [ ] 1.3 Create `domain/model/VideoCreationNotification.java` record: `String phase`, `int progress`, `String message`

## 2. Application — Use case implementation

- [ ] 2.1 Create `application/usecase/asset/CreateVideoFromImagesUseCaseImpl.java` annotated with `@Service`
- [ ] 2.2 Resolve asset file paths from `assetIds`; notify PREPARING phase
- [ ] 2.3 Create a temp directory; copy each image as `frame0001.jpg`, `frame0002.jpg`, ...
- [ ] 2.4 Build FFmpeg command: `ffmpeg -framerate 1/{slideDuration} -i <tmp>/frame%04d.jpg [-i <musicPath>] -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p <outputFolder>/<timestamp>.mp4`
- [ ] 2.5 Run `ProcessBuilder`; read stderr in a separate thread; parse `frame=N` lines to emit ENCODING progress events
- [ ] 2.6 On exit code 0: catalog the output file, notify DONE; on non-zero: notify error
- [ ] 2.7 In `finally`: delete temp directory

## 3. HTTP adapter

- [ ] 3.1 Add `POST /api/assets/create-video` to `AssetController` accepting `CreateVideoRequest` body; return `SseEmitter`; run use case `@Async`

## 4. Backend unit tests

- [ ] 4.1 Test that `CreateVideoFromImagesUseCaseImpl` builds the correct FFmpeg command for a list of 3 assets with duration 5s
- [ ] 4.2 Test that the music asset path is appended to the command when `musicAssetId` is provided
- [ ] 4.3 Test that a non-zero FFmpeg exit code causes an error notification

## 5. Frontend — VideoCreatorComponent

- [ ] 5.1 Create `features/gallery/video-creator/video-creator.component.ts` as a standalone multi-step wizard
- [ ] 5.2 Step 1: display selected assets as a reorderable list (use `CdkDragDrop` or up/down buttons)
- [ ] 5.3 Step 2: `<input type="number">` for slide duration; `<mat-select>` for optional music asset (loaded from `GET /api/assets?type=AUDIO`)
- [ ] 5.4 Step 3: folder picker for output folder; "Create Video" button; SSE progress bar; DONE message with link to gallery folder
- [ ] 5.5 Add `createVideo(request: CreateVideoRequest): EventSource` to `AssetService`
- [ ] 5.6 Show "Create Video" toolbar button in gallery when at least 2 image assets are selected

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
