## 1. Backend — Database migration

- [ ] 1.1 Create `V10__add_soft_delete.sql` in `backend/src/main/resources/db/migration/`:
  ```sql
  ALTER TABLE assets ADD COLUMN deleted_at TIMESTAMPTZ NULL;
  CREATE INDEX ix_assets_deleted_at ON assets(deleted_at);
  ```
- [ ] 1.2 Add `photomanager.recycle-bin-retention-days: 30` to `application.yml` under the `photomanager:` block

## 2. Backend — Entity

- [ ] 2.1 Add `@Column(name = "deleted_at") private LocalDateTime deletedAt;` field to `Asset.java`; import `java.time.LocalDateTime` (already present)

## 3. Backend — Repository

- [ ] 3.1 Add the following methods to `AssetRepository`:
  - `Page<Asset> findByFolderAndDeletedAtIsNull(Folder folder, Pageable pageable);` — used by normal gallery listing
  - `List<Asset> findByFolderAndDeletedAtIsNull(Folder folder);` — used by non-paginated callers
  - `Page<Asset> findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable pageable);` — recycle-bin listing
  - `List<Asset> findByDeletedAtBeforeAndDeletedAtIsNotNull(LocalDateTime cutoff);` — auto-purge

## 4. Backend — Domain service

- [ ] 4.1 Create `RecycleBinService.java` interface in `domain/service/` with methods:
  - `PaginatedData<Asset> getDeletedAssets(int pageIndex)`
  - `void restoreAssets(List<Long> assetIds)`
  - `void purgeAssets(List<Long> assetIds)`
  - `void purgeAll()`
  - `void purgeExpired(int retentionDays)`
- [ ] 4.2 Create `RecycleBinServiceImpl.java` in `infrastructure/service/`; annotate `@Service @RequiredArgsConstructor @Slf4j`; inject `AssetRepository assetRepository`, `StorageService storageService`, `ThumbnailStorageService thumbnailStorageService`; read `@Value("${photomanager.recycle-bin-retention-days:30}") int retentionDays`
- [ ] 4.3 Implement `getDeletedAssets(int pageIndex)` annotated `@Transactional(readOnly = true)`: call `assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(PageRequest.of(pageIndex, 100))`; return `PaginatedData<Asset>`
- [ ] 4.4 Implement `restoreAssets(List<Long> assetIds)` annotated `@Transactional`: for each ID in `assetRepository.findAllById(assetIds)`, set `asset.setDeletedAt(null)` and save
- [ ] 4.5 Implement `purgeAssets(List<Long> assetIds)` annotated `@Transactional`: for each found asset, call `storageService.deleteFile(asset.getFullPath())` inside a try-catch logging `IOException`; call `thumbnailStorageService.deleteThumbnail(asset.getThumbnailBlobName())`; call `assetRepository.delete(asset)`
- [ ] 4.6 Implement `purgeAll()` annotated `@Transactional`: call `assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable.unpaged()).getContent()` and pass the IDs to `purgeAssets`
- [ ] 4.7 Implement `purgeExpired(int retentionDays)` annotated `@Transactional`: compute `cutoff = LocalDateTime.now().minusDays(retentionDays)`; call `assetRepository.findByDeletedAtBeforeAndDeletedAtIsNotNull(cutoff)`; pass IDs to `purgeAssets`; log the count purged
- [ ] 4.8 Add `@Scheduled(cron = "0 0 2 * * *") public void autoPurgeExpired()` method calling `purgeExpired(retentionDays)`

## 5. Backend — Scheduler config

- [ ] 5.1 Add `@EnableScheduling` annotation to `AppConfig.java`

## 6. Backend — MoveAssetsService update

- [ ] 6.1 In `MoveAssetsServiceImpl.deleteAssets`, change the `deleteFile = false` branch: instead of `thumbnailStorageService.deleteThumbnail(...)` + `assetRepository.delete(asset)`, call:
  ```java
  asset.setDeletedAt(LocalDateTime.now());
  assetRepository.save(asset);
  ```
  The thumbnail is NOT deleted during soft delete; keep the thumbnail deletion only in the `deleteFile = true` branch

## 7. Backend — Facade and Controller

- [ ] 7.1 Add three method signatures to `PhotoManagerFacade`:
  - `PaginatedData<Asset> getRecycleBin(int pageIndex)`
  - `void restoreAssets(List<Long> assetIds)`
  - `void purgeRecycleBin(List<Long> assetIds)`
- [ ] 7.2 Inject `RecycleBinService recycleBinService` into `PhotoManagerFacadeImpl`; implement all three by delegating to `recycleBinService`
- [ ] 7.3 In `PhotoManagerFacadeImpl.getAssets`, replace `assetRepository.findByFolder(folder.get(), pageRequest)` with `assetRepository.findByFolderAndDeletedAtIsNull(folder.get(), pageRequest)`
- [ ] 7.4 Create `RecycleBinPurgeRequest.java` in `api/dto/` as a record with `@NotEmpty List<Long> assetIds` (nullable — if null, purge all)
- [ ] 7.5 Create `RecycleBinRestoreRequest.java` in `api/dto/` as a record with `@NotEmpty List<Long> assetIds`
- [ ] 7.6 Create `RecycleBinController.java` in `api/` annotated `@RestController @RequestMapping("/api/recycle-bin") @RequiredArgsConstructor`; inject `PhotoManagerFacade facade`
- [ ] 7.7 Add `GET /api/recycle-bin` handler: `listDeleted(@RequestParam(defaultValue="0") int page)` → `facade.getRecycleBin(page)` → map to `PaginatedData<AssetDto>` → `200 OK`
- [ ] 7.8 Add `POST /api/recycle-bin/restore` handler: `restore(@Valid @RequestBody RecycleBinRestoreRequest body)` → `facade.restoreAssets(body.assetIds())` → `204 No Content`
- [ ] 7.9 Add `DELETE /api/recycle-bin` handler: `purge(@RequestBody(required=false) RecycleBinPurgeRequest body)` → if body is null or `assetIds` is null, call `facade.purgeRecycleBin(null)` (purge all); else call `facade.purgeRecycleBin(body.assetIds())` → `204 No Content`

## 8. Backend — Duplicate detection update

- [ ] 8.1 In `FindDuplicatedAssetsServiceImpl` (or wherever `assetRepository.findAll()` / `findByHash` is called), update the query to exclude soft-deleted assets; add `List<Asset> findByHashAndDeletedAtIsNull(String hash)` to `AssetRepository` and update usages

## 9. Backend — Tests

- [ ] 9.1 Create `RecycleBinControllerTest` (`@WebMvcTest(RecycleBinController.class)`): mock `PhotoManagerFacade`; assert `GET /api/recycle-bin` returns `200` with empty list; assert `POST /api/recycle-bin/restore` with valid body returns `204`; assert `DELETE /api/recycle-bin` with body returns `204`; assert `DELETE /api/recycle-bin` with no body returns `204`
- [ ] 9.2 Create `RecycleBinServiceTest` (`@ExtendWith(MockitoExtension.class)`): mock `AssetRepository`, `StorageService`, `ThumbnailStorageService`; test `restoreAssets` sets `deletedAt = null` and saves; test `purgeAssets` calls `deleteFile`, `deleteThumbnail`, and `assetRepository.delete`; test `purgeExpired` only passes assets older than the cutoff
- [ ] 9.3 Add test to verify `MoveAssetsServiceImpl.deleteAssets` with `deleteFile=false` sets `deletedAt` and does NOT call `thumbnailStorageService.deleteThumbnail`
- [ ] 9.4 Add test to verify `MoveAssetsServiceImpl.deleteAssets` with `deleteFile=true` continues to call `storageService.deleteFile`, `thumbnailStorageService.deleteThumbnail`, and `assetRepository.delete` (unchanged behaviour)
- [ ] 9.5 Run `mvn test` and confirm all tests pass

## 10. Frontend — Service

- [ ] 10.1 Create `recycle-bin.service.ts` in `frontend/src/app/core/services/` with methods:
  - `getRecycleBin(page = 0): Observable<PaginatedData<Asset>>` — `GET /api/recycle-bin?page=`
  - `restoreAssets(assetIds: number[]): Observable<void>` — `POST /api/recycle-bin/restore { assetIds }`
  - `purgeAssets(assetIds: number[]): Observable<void>` — `DELETE /api/recycle-bin` with body `{ assetIds }`
  - `purgeAll(): Observable<void>` — `DELETE /api/recycle-bin` with no body

## 11. Frontend — RecycleBinComponent

- [ ] 11.1 Create `frontend/src/app/features/recycle-bin/` directory with `recycle-bin.component.ts`, `.html`, `.scss`, `.cy.ts`
- [ ] 11.2 Declare standalone `RecycleBinComponent`; inject `RecycleBinService` and `MatSnackBar`; on `ngOnInit` call `loadPage(0)`
- [ ] 11.3 Add state: `assets: Asset[] = []`, `selectedAssets = new Set<number>()`, `pageIndex = 0`, `totalPages = 0`, `totalItems = 0`
- [ ] 11.4 Implement `loadPage(page: number)`: call `recycleBinService.getRecycleBin(page)`; on success set `assets`, `pageIndex`, `totalPages`, `totalItems`
- [ ] 11.5 Implement `restoreSelected()`: call `recycleBinService.restoreAssets(Array.from(selectedAssets))`; on success show snack bar "Restored successfully"; clear selection; reload page 0
- [ ] 11.6 Implement `purgeSelected()`: call `recycleBinService.purgeAssets(Array.from(selectedAssets))`; on success show snack bar "Permanently deleted"; clear selection; reload page 0
- [ ] 11.7 Implement `purgeAll()`: call `recycleBinService.purgeAll()`; on success show snack bar "Recycle bin emptied"; reload page 0
- [ ] 11.8 Build template: `MatToolbar` with title "Recycle Bin" and an "Empty Recycle Bin" button (`delete_forever` icon); action buttons "Restore" and "Delete Permanently" visible when `selectedAssets.size > 0`; `@for (asset of assets)` thumbnail grid using `ThumbnailComponent` with selection checkbox; pagination controls; import `ThumbnailComponent`, `MatToolbarModule`, `MatButtonModule`, `MatIconModule`, `MatCheckboxModule`, `MatSnackBarModule`
- [ ] 11.9 Write SCSS: reuse the same `.thumbnail-grid` layout as the gallery

## 12. Frontend — Routing and navigation

- [ ] 12.1 Add lazy route to `app.routes.ts`:
  ```typescript
  { path: 'recycle-bin', loadComponent: () => import('./features/recycle-bin/recycle-bin.component').then(m => m.RecycleBinComponent), canActivate: [authGuard] }
  ```
- [ ] 12.2 Add `<a mat-button routerLink="/recycle-bin">Recycle Bin</a>` to `app.component.html` nav

## 13. Frontend — Tests

- [ ] 13.1 In `recycle-bin.component.cy.ts`: mount with stubbed `RecycleBinService` returning two soft-deleted assets; assert two thumbnail cards are rendered
- [ ] 13.2 Add test: select one asset; click "Restore"; assert `recycleBinService.restoreAssets` called with correct ID; assert snack bar shows "Restored successfully"
- [ ] 13.3 Add test: select one asset; click "Delete Permanently"; assert `recycleBinService.purgeAssets` called; assert snack bar shows "Permanently deleted"
- [ ] 13.4 Add test: click "Empty Recycle Bin"; assert `recycleBinService.purgeAll` called
- [ ] 13.5 Run `npm test` and confirm all tests pass
