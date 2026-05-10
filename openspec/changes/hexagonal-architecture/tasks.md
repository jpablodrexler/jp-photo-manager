## Phase 1 — Domain Models

- [ ] 1.1 Create `domain/model/` package with plain POJO classes for each entity: `Asset`, `AssetExif`, `Folder`, `Album`, `User`, `RefreshToken`, `SearchPreset`, `SyncDirectoriesDefinition`, `ConvertDirectoriesDefinition`, `CatalogRunState`, `RecentTargetPath` — copy fields from the existing `domain/entity/` classes; remove all `jakarta.persistence.*` imports and JPA annotations; use Lombok `@Data` / `@Builder` / `@NoArgsConstructor`
- [ ] 1.2 Create `application/dto/PaginatedResult.java` as a Java record: `record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}`
- [ ] 1.3 Run `mvn test` — all tests must still pass (domain entity classes still exist in parallel)

## Phase 2 — Driven Port Interfaces (port/out)

- [ ] 2.1 Create `domain/port/out/AssetRepository.java` — plain Java interface with methods: `Optional<Asset> findById(Long id)`, `Optional<Asset> findByFolderAndFileName(Folder folder, String fileName)`, `PaginatedResult<Asset> findFiltered(AssetFilter filter)`, `List<Asset> findByFolder(Folder folder)`, `List<Asset> findAll()`, `Asset save(Asset asset)`, `void delete(Long id)` — use domain model types only, no Spring or JPA imports
- [ ] 2.2 Create `domain/port/out/AssetExifRepository.java`
- [ ] 2.3 Create `domain/port/out/FolderRepository.java`
- [ ] 2.4 Create `domain/port/out/AlbumRepository.java`
- [ ] 2.5 Create `domain/port/out/UserRepository.java`
- [ ] 2.6 Create `domain/port/out/RefreshTokenRepository.java`
- [ ] 2.7 Create `domain/port/out/SearchPresetRepository.java`
- [ ] 2.8 Create `domain/port/out/SyncConfigRepository.java`
- [ ] 2.9 Create `domain/port/out/ConvertConfigRepository.java`
- [ ] 2.10 Create `domain/port/out/CatalogStateRepository.java`
- [ ] 2.11 Create `domain/port/out/RecentTargetPathRepository.java`
- [ ] 2.12 Create `domain/port/out/StoragePort.java` — mirror the existing `domain/service/StorageService.java` interface signature but use domain model types; remove `java.awt.image.BufferedImage` from the port if it belongs in the adapter
- [ ] 2.13 Create `domain/port/out/ThumbnailPort.java`
- [ ] 2.14 Create `domain/port/out/HashCalculatorPort.java`
- [ ] 2.15 Create `domain/port/out/JwtTokenPort.java`
- [ ] 2.16 Create `application/dto/AssetFilter.java` record capturing: `folderId`, `search`, `dateFrom`, `dateTo`, `minRating`, `sortCriteria`, `page`, `pageSize`, `includeDeleted`
- [ ] 2.17 Run `mvn test` — all tests still pass

## Phase 3 — Persistence Adapters

- [ ] 3.1 Create `infrastructure/persistence/entity/` package; rename/copy each `domain/entity/*.java` to `infrastructure/persistence/entity/*Entity.java` — preserve all JPA annotations; update `@Table`, `@Entity`, `@ManyToOne` etc. to reference entity classes within the same package
- [ ] 3.2 Create `infrastructure/persistence/jpa/` package; move each `domain/repository/*.java` interface to `infrastructure/persistence/jpa/Jpa*.java`; update generic type parameters to use `*Entity` classes
- [ ] 3.3 Create `infrastructure/persistence/mapper/` with one mapper per entity type (e.g. `AssetEntityMapper`) converting between `AssetEntity` ↔ `Asset` domain model; use MapStruct `@Mapper` or hand-write if conversion logic is non-trivial
- [ ] 3.4 Create `infrastructure/persistence/adapter/AssetRepositoryAdapter.java` — annotate `@Service`; inject `JpaAssetRepository` + `AssetEntityMapper`; implement all methods of `AssetRepository` by delegating to the JPA repository and mapping results
- [ ] 3.5 Create remaining adapter classes: `AssetExifRepositoryAdapter`, `FolderRepositoryAdapter`, `AlbumRepositoryAdapter`, `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, `SearchPresetRepositoryAdapter`, `SyncConfigRepositoryAdapter`, `ConvertConfigRepositoryAdapter`, `CatalogStateRepositoryAdapter`, `RecentTargetPathRepositoryAdapter`
- [ ] 3.6 Delete `domain/entity/` package
- [ ] 3.7 Delete `domain/repository/` package
- [ ] 3.8 Update `infrastructure/service/` implementations (still in use by `PhotoManagerFacadeImpl`) to import from `infrastructure/persistence/jpa/` and `infrastructure/persistence/entity/` as an interim measure
- [ ] 3.9 Run `mvn test` — all tests pass

## Phase 4 — Driving Port Interfaces and Use Case Implementations

Each use-case interface has exactly one method. Each implementation class implements exactly one interface and has exactly one business method. Annotate read implementations with `@Service @Transactional(readOnly = true)` and write implementations with `@Service @Transactional`. Inject only `domain/port/out/` interfaces; no Spring MVC, Spring Data, or HTTP types are permitted.

### 4a — asset/ subpackage (8 interfaces + 8 implementations)

- [ ] 4a.1 Create `domain/port/in/asset/GetAssetsUseCase.java` — `PaginatedResult<Asset> execute(AssetFilter filter)`
- [ ] 4a.2 Create `application/usecase/asset/GetAssetsUseCaseImpl.java` — migrate `getAssets()` logic from `PhotoManagerFacadeImpl`; replace `Page`/`PageRequest`/`Sort` with `PaginatedResult` + `AssetFilter`
- [ ] 4a.3 Create `domain/port/in/asset/GetAssetImageUseCase.java` — `AssetImage execute(Long assetId)`
- [ ] 4a.4 Create `application/usecase/asset/GetAssetImageUseCaseImpl.java`
- [ ] 4a.5 Create `domain/port/in/asset/GetAssetExifUseCase.java` — `AssetExif execute(Long assetId)`
- [ ] 4a.6 Create `application/usecase/asset/GetAssetExifUseCaseImpl.java`
- [ ] 4a.7 Create `domain/port/in/asset/DownloadAssetsUseCase.java` — `void execute(List<Long> assetIds, OutputStream out)`
- [ ] 4a.8 Create `application/usecase/asset/DownloadAssetsUseCaseImpl.java`
- [ ] 4a.9 Create `domain/port/in/asset/RateAssetUseCase.java` — `void execute(Long assetId, int rating)`
- [ ] 4a.10 Create `application/usecase/asset/RateAssetUseCaseImpl.java`
- [ ] 4a.11 Create `domain/port/in/asset/MoveAssetsUseCase.java` — `void execute(List<Long> assetIds, String destinationPath, boolean preserveOriginal)`
- [ ] 4a.12 Create `application/usecase/asset/MoveAssetsUseCaseImpl.java`; validate destination is within configured `rootCatalogFolders`; update recent target paths
- [ ] 4a.13 Create `domain/port/in/asset/UploadAssetUseCase.java` — `void execute(String folderPath, String fileName, byte[] content)`
- [ ] 4a.14 Create `application/usecase/asset/UploadAssetUseCaseImpl.java`; replace `MultipartFile` with `byte[]` (conversion done in the controller mapper)
- [ ] 4a.15 Create `domain/port/in/asset/DeleteAssetsUseCase.java` — `void execute(List<Long> assetIds, boolean permanently)`
- [ ] 4a.16 Create `application/usecase/asset/DeleteAssetsUseCaseImpl.java`

### 4b — catalog/ subpackage (2 interfaces + 2 implementations)

- [ ] 4b.1 Create `domain/port/in/catalog/CatalogAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener)`
- [ ] 4b.2 Create `application/usecase/catalog/CatalogAssetsUseCaseImpl.java`; migrate catalog-run logic including distributed lock acquisition and heartbeat
- [ ] 4b.3 Create `domain/port/in/catalog/GetDuplicatedAssetsUseCase.java` — `List<List<Asset>> execute()`
- [ ] 4b.4 Create `application/usecase/catalog/GetDuplicatedAssetsUseCaseImpl.java`

### 4c — album/ subpackage (7 interfaces + 7 implementations)

- [ ] 4c.1 Create `domain/port/in/album/GetAlbumsUseCase.java` — `PaginatedResult<AlbumData> execute(UUID userId, int page)`
- [ ] 4c.2 Create `application/usecase/album/GetAlbumsUseCaseImpl.java`
- [ ] 4c.3 Create `domain/port/in/album/CreateAlbumUseCase.java` — `AlbumData execute(UUID userId, String name, String description)`
- [ ] 4c.4 Create `application/usecase/album/CreateAlbumUseCaseImpl.java`
- [ ] 4c.5 Create `domain/port/in/album/GetAlbumUseCase.java` — `AlbumData execute(Long albumId, UUID userId, int page)`
- [ ] 4c.6 Create `application/usecase/album/GetAlbumUseCaseImpl.java`
- [ ] 4c.7 Create `domain/port/in/album/UpdateAlbumUseCase.java` — `AlbumData execute(Long albumId, UUID userId, String name, String description)`
- [ ] 4c.8 Create `application/usecase/album/UpdateAlbumUseCaseImpl.java`
- [ ] 4c.9 Create `domain/port/in/album/DeleteAlbumUseCase.java` — `void execute(Long albumId, UUID userId)`
- [ ] 4c.10 Create `application/usecase/album/DeleteAlbumUseCaseImpl.java`
- [ ] 4c.11 Create `domain/port/in/album/AddAssetsToAlbumUseCase.java` — `void execute(Long albumId, UUID userId, List<Long> assetIds)`
- [ ] 4c.12 Create `application/usecase/album/AddAssetsToAlbumUseCaseImpl.java`
- [ ] 4c.13 Create `domain/port/in/album/RemoveAssetsFromAlbumUseCase.java` — `void execute(Long albumId, UUID userId, List<Long> assetIds)`
- [ ] 4c.14 Create `application/usecase/album/RemoveAssetsFromAlbumUseCaseImpl.java`

### 4d — sync/ subpackage (3 interfaces + 3 implementations)

- [ ] 4d.1 Create `domain/port/in/sync/GetSyncConfigUseCase.java` — `List<SyncDirectoriesDefinition> execute()`
- [ ] 4d.2 Create `application/usecase/sync/GetSyncConfigUseCaseImpl.java`
- [ ] 4d.3 Create `domain/port/in/sync/SaveSyncConfigUseCase.java` — `void execute(List<SyncDirectoriesDefinition> definitions)`
- [ ] 4d.4 Create `application/usecase/sync/SaveSyncConfigUseCaseImpl.java`
- [ ] 4d.5 Create `domain/port/in/sync/SyncAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<SyncAssetsResult> listener)`
- [ ] 4d.6 Create `application/usecase/sync/SyncAssetsUseCaseImpl.java`

### 4e — convert/ subpackage (3 interfaces + 3 implementations)

- [ ] 4e.1 Create `domain/port/in/convert/GetConvertConfigUseCase.java` — `List<ConvertDirectoriesDefinition> execute()`
- [ ] 4e.2 Create `application/usecase/convert/GetConvertConfigUseCaseImpl.java`
- [ ] 4e.3 Create `domain/port/in/convert/SaveConvertConfigUseCase.java` — `void execute(List<ConvertDirectoriesDefinition> definitions)`
- [ ] 4e.4 Create `application/usecase/convert/SaveConvertConfigUseCaseImpl.java`
- [ ] 4e.5 Create `domain/port/in/convert/ConvertAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<ConvertAssetsResult> listener)`
- [ ] 4e.6 Create `application/usecase/convert/ConvertAssetsUseCaseImpl.java`

### 4f — folder/ subpackage (4 interfaces + 4 implementations)

- [ ] 4f.1 Create `domain/port/in/folder/GetSubFoldersUseCase.java` — `List<Folder> execute(String parentPath)`
- [ ] 4f.2 Create `application/usecase/folder/GetSubFoldersUseCaseImpl.java`
- [ ] 4f.3 Create `domain/port/in/folder/GetDrivesUseCase.java` — `List<String> execute()`
- [ ] 4f.4 Create `application/usecase/folder/GetDrivesUseCaseImpl.java`
- [ ] 4f.5 Create `domain/port/in/folder/GetInitialFolderUseCase.java` — `String execute()`
- [ ] 4f.6 Create `application/usecase/folder/GetInitialFolderUseCaseImpl.java`
- [ ] 4f.7 Create `domain/port/in/folder/GetRecentTargetPathsUseCase.java` — `List<String> execute()`
- [ ] 4f.8 Create `application/usecase/folder/GetRecentTargetPathsUseCaseImpl.java`

### 4g — recycle/ subpackage (3 interfaces + 3 implementations)

- [ ] 4g.1 Create `domain/port/in/recycle/GetDeletedAssetsUseCase.java` — `PaginatedResult<Asset> execute(int page)`
- [ ] 4g.2 Create `application/usecase/recycle/GetDeletedAssetsUseCaseImpl.java`
- [ ] 4g.3 Create `domain/port/in/recycle/RestoreAssetsUseCase.java` — `void execute(List<Long> assetIds)`
- [ ] 4g.4 Create `application/usecase/recycle/RestoreAssetsUseCaseImpl.java`
- [ ] 4g.5 Create `domain/port/in/recycle/PurgeAssetsUseCase.java` — `void execute(List<Long> assetIds)`
- [ ] 4g.6 Create `application/usecase/recycle/PurgeAssetsUseCaseImpl.java`

### 4h — search/ subpackage (3 interfaces + 3 implementations)

- [ ] 4h.1 Create `domain/port/in/search/GetSearchPresetsUseCase.java` — `List<SearchPreset> execute(UUID userId)`
- [ ] 4h.2 Create `application/usecase/search/GetSearchPresetsUseCaseImpl.java`
- [ ] 4h.3 Create `domain/port/in/search/CreateSearchPresetUseCase.java` — `SearchPreset execute(UUID userId, String name, FilterPreset criteria)`
- [ ] 4h.4 Create `application/usecase/search/CreateSearchPresetUseCaseImpl.java`
- [ ] 4h.5 Create `domain/port/in/search/DeleteSearchPresetUseCase.java` — `void execute(Long presetId, UUID userId)`
- [ ] 4h.6 Create `application/usecase/search/DeleteSearchPresetUseCaseImpl.java`

### 4i — home/ subpackage (1 interface + 1 implementation)

- [ ] 4i.1 Create `domain/port/in/home/GetHomeStatsUseCase.java` — `HomeStats execute()`
- [ ] 4i.2 Create `application/usecase/home/GetHomeStatsUseCaseImpl.java`

### 4j — user/ subpackage (4 interfaces + 4 implementations)

- [ ] 4j.1 Create `domain/port/in/user/ListUsersUseCase.java` — `List<UserSummary> execute()`
- [ ] 4j.2 Create `application/usecase/user/ListUsersUseCaseImpl.java`
- [ ] 4j.3 Create `domain/port/in/user/CreateUserUseCase.java` — `UserSummary execute(String username, String password, String role)`
- [ ] 4j.4 Create `application/usecase/user/CreateUserUseCaseImpl.java`
- [ ] 4j.5 Create `domain/port/in/user/UpdatePasswordUseCase.java` — `void execute(UUID userId, String newPassword)`
- [ ] 4j.6 Create `application/usecase/user/UpdatePasswordUseCaseImpl.java`
- [ ] 4j.7 Create `domain/port/in/user/DeleteUserUseCase.java` — `void execute(UUID userId)`
- [ ] 4j.8 Create `application/usecase/user/DeleteUserUseCaseImpl.java`

### 4k — cleanup

- [ ] 4k.1 Delete `application/PhotoManagerFacade.java` and `application/PhotoManagerFacadeImpl.java`
- [ ] 4k.2 Run `mvn test` — all tests pass (controllers still broken at this point; use-case unit tests must pass)

## Phase 5 — Web Adapters (HTTP Layer)

- [ ] 5.1 Create `infrastructure/web/controller/`, `infrastructure/web/dto/`, `infrastructure/web/mapper/`, `infrastructure/web/exception/` packages
- [ ] 5.2 Move all classes from `api/dto/` → `infrastructure/web/dto/`; update imports throughout
- [ ] 5.3 Move all classes from `api/exception/` → `infrastructure/web/exception/`; update imports
- [ ] 5.4 Create `infrastructure/web/mapper/AssetDtoMapper.java` — converts `Asset` domain model ↔ `AssetDto`; convert `MultipartFile` → `byte[]` + filename before calling use case
- [ ] 5.5 Create `infrastructure/web/mapper/AlbumDtoMapper.java` and `FolderDtoMapper.java`
- [ ] 5.6 Move `api/AssetController.java` → `infrastructure/web/controller/AssetController.java`; replace `@Autowired PhotoManagerFacade` with individual use-case injections (`GetAssetsUseCase`, `GetAssetImageUseCase`, `GetAssetExifUseCase`, `DownloadAssetsUseCase`, `RateAssetUseCase`, `MoveAssetsUseCase`, `UploadAssetUseCase`, `DeleteAssetsUseCase`, `CatalogAssetsUseCase`, `GetDuplicatedAssetsUseCase`); each controller handler calls `useCase.execute(…)` and delegates HTTP↔domain conversion to `AssetDtoMapper`
- [ ] 5.7 Move and update `AlbumController`, `AuthController`, `ConvertController`, `FolderController`, `HomeController`, `RecycleBinController`, `SearchPresetController`, `SyncController`, `UserAdminController` following the same pattern as 5.6
- [ ] 5.8 Move `api/GlobalExceptionHandler.java`, `api/LoginResponse.java`, `api/AuthRequest.java`, `api/CreateUserRequest.java`, `api/UpdatePasswordRequest.java`, `api/ErrorResponse.java` to `infrastructure/web/`
- [ ] 5.9 Delete `api/` package
- [ ] 5.10 Run `mvn test` — all tests pass

## Phase 6 — Service Adapters

- [ ] 6.1 Rename `infrastructure/service/StorageServiceImpl.java` → `StorageServiceAdapter.java`; annotate class as implementing `StoragePort`; remove the old `domain/service/StorageService` interface if it is now unused
- [ ] 6.2 Rename `ThumbnailStorageServiceImpl` → `ThumbnailStorageServiceAdapter`; implement `ThumbnailPort`
- [ ] 6.3 Rename `AssetHashCalculatorService` → `AssetHashCalculatorAdapter`; implement `HashCalculatorPort`
- [ ] 6.4 Create `JwtTokenAdapter` implementing `JwtTokenPort`; delegate to `JwtUtil`
- [ ] 6.5 Delete the now-unused interfaces from `domain/service/`: `StorageService`, `ThumbnailStorageService`, `CatalogAssetsService`, `SyncAssetsService`, `ConvertAssetsService`, `FindDuplicatedAssetsService`, `MoveAssetsService`, `AlbumService`, `RecycleBinService`, `SearchPresetService`, `JwtTokenService`, `CatalogFolderService`, `UserService`, `UserAdminService`, `RefreshTokenService`, `ExifMetadata`
- [ ] 6.6 Delete `domain/service/` package once empty
- [ ] 6.7 Run `mvn test` — all tests pass

## Phase 7 — Verification and Documentation

- [ ] 7.1 Run `mvn clean package` — build succeeds with no warnings
- [ ] 7.2 Run `mvn test` — all tests pass
- [ ] 7.3 Verify that no class in `domain/` imports from `jakarta.*`, `org.springframework.*`, or `infrastructure.*` (use `grep -r "import jakarta\|import org.springframework\|import.*infrastructure" src/main/java/com/jpablodrexler/photomanager/domain/` — must return empty)
- [ ] 7.4 Verify that no class in `application/usecase/` imports from `infrastructure.*` or `org.springframework.web.*` or `org.springframework.data.*`
- [ ] 7.5 Start the application locally (`mvn spring-boot:run`), authenticate as `admin/admin`, and verify: gallery loads, catalog runs, album creation works, sync config saves, duplicate detection works
- [ ] 7.6 Update `CLAUDE.md` — replace the **Backend** architecture section under _Web Architecture_ with the new hexagonal layout: update the package tree diagram to show `domain/model/`, `domain/port/in/`, `domain/port/out/`, `application/usecase/`, `infrastructure/persistence/` (entity, jpa, adapter, mapper), `infrastructure/web/` (controller, dto, mapper, exception), and `infrastructure/service/`; update the dependency-flow line to `infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service`; update the _Key domain services_ prose to describe use-case interfaces instead of the old service interfaces; remove references to `PhotoManagerFacade`
- [ ] 7.7 Update `README.md` — add or replace the backend architecture diagram (ASCII or Mermaid) to reflect the hexagonal structure; document the `domain/port/in/` and `domain/port/out/` conventions; add a note that controllers in `infrastructure/web/controller/` delegate directly to use-case interfaces and never touch repositories or service adapters directly
