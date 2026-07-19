## 1. Database migration

- [ ] 1.1 Create `src/main/resources/db/migration/V16__add_asset_description.sql` adding `description VARCHAR(2000) NULL DEFAULT NULL` to `asset_exif`

## 2. Domain — Use case interface

- [ ] 2.1 Create `domain/port/in/asset/UpdateAssetDescriptionUseCase.java` with `void execute(long assetId, String description)`

## 3. Application — Use case implementation

- [ ] 3.1 Create `application/usecase/asset/UpdateAssetDescriptionUseCaseImpl.java` annotated with `@Service`
- [ ] 3.2 Inject `AssetExifRepositoryPort`; load the exif record, set `description`, save

## 4. HTTP adapter

- [ ] 4.1 Add `PATCH /api/assets/{id}/description` to `AssetController`; request body is `UpdateAssetDescriptionRequest { String description }`; validate max length 2000 with `@Size`
- [ ] 4.2 Return `200 OK` on success; rely on `GlobalExceptionHandler` for 404 if asset not found

## 5. Update GetAssetExifUseCase response

- [ ] 5.1 Add `description` field to the exif response DTO so the frontend receives it when loading the panel

## 6. Backend unit tests

- [ ] 6.1 Test that `UpdateAssetDescriptionUseCaseImpl` saves the description correctly
- [ ] 6.2 Test that a null description clears the field
- [ ] 6.3 Test that `PATCH /api/assets/{id}/description` with > 2000 chars returns 400

## 7. Frontend — ExifPanelComponent

- [ ] 7.1 Add a `<textarea matInput>` for `description` to the exif panel template
- [ ] 7.2 Bind `(blur)` to `saveDescription()` which calls `assetService.updateDescription(assetId, value)`
- [ ] 7.3 Add `updateDescription(assetId: number, description: string): Observable<void>` to `AssetService`

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
