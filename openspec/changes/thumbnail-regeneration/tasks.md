## 1. Domain — Use case interface

- [ ] 1.1 Create `domain/port/in/asset/RegenerateThumbnailsUseCase.java` with `void execute(String folderPath, Consumer<ThumbnailRegenerationNotification> progressConsumer)`
- [ ] 1.2 Create `domain/model/ThumbnailRegenerationNotification.java` record with `long assetId`, `String fileName`, `boolean success`, `int processed`, `int total`

## 2. Application — Use case implementation

- [ ] 2.1 Create `application/usecase/asset/RegenerateThumbnailsUseCaseImpl.java` annotated with `@Service` and `@PreAuthorize("hasRole('ADMIN')")`
- [ ] 2.2 Inject `AssetRepositoryPort`, `ThumbnailPort`, and `StoragePort`
- [ ] 2.3 If `folderPath` is non-null, load assets via `assetRepository.findByFolderPath(folderPath)`; otherwise load all non-deleted assets
- [ ] 2.4 For each asset: call `thumbnailPort.deleteThumbnail(assetId)`, then `storagePort.generateThumbnail(asset)`; catch exceptions per asset and mark `success = false`
- [ ] 2.5 After each asset, call `progressConsumer.accept(notification)` with the current progress

## 3. HTTP adapter

- [ ] 3.1 Add `POST /api/assets/regenerate-thumbnails` to `AssetController` that returns `SseEmitter`
- [ ] 3.2 The method creates an `SseEmitter`, starts `@Async` task calling `regenerateThumbnailsUseCase.execute(folderPath, consumer)`, where `consumer` sends SSE events
- [ ] 3.3 On completion, send a `done` event and complete the emitter

## 4. Backend unit tests

- [ ] 4.1 Test that `RegenerateThumbnailsUseCaseImpl` calls `thumbnailPort.deleteThumbnail()` and `storagePort.generateThumbnail()` for each asset
- [ ] 4.2 Test that a per-asset exception does not abort the loop
- [ ] 4.3 Test that `folderPath` scoping filters to only matching assets

## 5. Frontend — AssetService

- [ ] 5.1 Add `regenerateThumbnails(folderPath?: string): EventSource` to `AssetService`

## 6. Frontend — Gallery toolbar action

- [ ] 6.1 Add "Regenerate thumbnails" button to `GalleryComponent` toolbar (visible only to `ADMIN` users)
- [ ] 6.2 On click, open an `SseEmitter`-backed progress dialog (reuse the pattern from catalog/sync)
- [ ] 6.3 Show a `MatSnackBar` on completion: "Regenerated N thumbnails (M failed)"

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
