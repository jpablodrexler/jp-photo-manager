## 1. Domain — ResolutionPolicy enum

- [ ] 1.1 Create `domain/enums/ResolutionPolicy.java` with values `KEEP_OLDEST`, `KEEP_NEWEST`, `KEEP_HIGHEST_RESOLUTION`, `KEEP_PREFERRED_FOLDER`

## 2. Domain — Use case interface and result model

- [ ] 2.1 Create `domain/port/in/asset/AutoResolveDuplicatesUseCase.java` with method `AutoResolveResult execute(ResolutionPolicy policy, String preferredFolderPath, boolean dryRun)`
- [ ] 2.2 Create `application/dto/AutoResolveResult.java` as a record with `int deletedCount`

## 3. Application — Use case implementation

- [ ] 3.1 Create `application/usecase/asset/AutoResolveDuplicatesUseCaseImpl.java` injecting `FindDuplicatedAssetsUseCase` and `SoftDeleteAssetUseCase`
- [ ] 3.2 Implement `KEEP_OLDEST`: sort group by `creationDateTime` ascending, retain first
- [ ] 3.3 Implement `KEEP_NEWEST`: sort by `creationDateTime` descending, retain first
- [ ] 3.4 Implement `KEEP_HIGHEST_RESOLUTION`: sort by `pixelWidth * pixelHeight` descending, retain first
- [ ] 3.5 Implement `KEEP_PREFERRED_FOLDER`: find first asset where `folderPath.startsWith(preferredFolderPath)`, fall back to `KEEP_OLDEST` if none match
- [ ] 3.6 When `dryRun = true`, count non-winners without calling `SoftDeleteAssetUseCase`
- [ ] 3.7 When `dryRun = false`, call `SoftDeleteAssetUseCase.execute(assetId)` for each non-winner

## 4. HTTP adapter

- [ ] 4.1 Create `infrastructure/web/dto/AutoResolveDuplicatesRequest.java` record with `ResolutionPolicy policy`, `String preferredFolderPath`, `boolean dryRun`
- [ ] 4.2 Create `infrastructure/web/dto/AutoResolveDuplicatesResponse.java` record with `int deletedCount`
- [ ] 4.3 Add `POST /api/assets/duplicates/auto-resolve` to `AssetController` delegating to `PhotoManagerFacade`
- [ ] 4.4 Add `autoResolveDuplicates(AutoResolveDuplicatesRequest)` to `PhotoManagerFacade`

## 5. Backend unit tests

- [ ] 5.1 Test `KEEP_OLDEST` retains the earliest asset and soft-deletes the rest
- [ ] 5.2 Test `KEEP_NEWEST` retains the most recent asset
- [ ] 5.3 Test `KEEP_HIGHEST_RESOLUTION` retains the largest-area asset
- [ ] 5.4 Test `KEEP_PREFERRED_FOLDER` retains the asset in the preferred folder
- [ ] 5.5 Test `KEEP_PREFERRED_FOLDER` falls back to `KEEP_OLDEST` when no asset is in the preferred folder
- [ ] 5.6 Test dry run returns count without calling `SoftDeleteAssetUseCase`

## 6. Frontend — AssetService

- [ ] 6.1 Add `autoResolveDuplicates(policy: string, preferredFolderPath?: string, dryRun?: boolean): Observable<{ deletedCount: number }>` to `AssetService`

## 7. Frontend — AutoResolveDialogComponent

- [ ] 7.1 Create `features/duplicates/auto-resolve-dialog/auto-resolve-dialog.component.ts` as a standalone Angular Material dialog
- [ ] 7.2 Add a `MatSelect` for policy: "Keep oldest", "Keep newest", "Keep highest resolution", "Keep preferred folder"
- [ ] 7.3 Show a `MatFormField` text input for preferred folder path (only when `KEEP_PREFERRED_FOLDER` is selected)
- [ ] 7.4 On policy selection, call `assetService.autoResolveDuplicates(policy, path, true)` to get the dry-run count
- [ ] 7.5 Display confirmation message: "This will delete {count} assets. Proceed?"
- [ ] 7.6 Confirm button calls `assetService.autoResolveDuplicates(policy, path, false)`, closes dialog, and emits a result

## 8. Frontend — DuplicatesComponent integration

- [ ] 8.1 Add "Clean up automatically" button to `DuplicatesComponent` toolbar
- [ ] 8.2 Open `AutoResolveDialogComponent` via `MatDialog`
- [ ] 8.3 After dialog closes with a result, reload the duplicates list and show a `MatSnackBar` with "Deleted {count} duplicate assets"

## 9. Frontend tests

- [ ] 9.1 Cypress component test: policy selector shows all four options
- [ ] 9.2 Cypress component test: preferred folder input appears only for `KEEP_PREFERRED_FOLDER`
- [ ] 9.3 Cypress component test: dry-run count is displayed in the confirmation message
- [ ] 9.4 Cypress component test: Cancel closes the dialog without calling the non-dry-run endpoint

## 10. Testing and Commit

- [ ] 10.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 10.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 10.3 Commit all changes (only after both test suites pass)
