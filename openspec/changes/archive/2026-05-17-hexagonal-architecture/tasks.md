## Phase 1 — Domain Models

- [x] 1.1 Create `domain/model/` package with plain POJO classes for each entity: `Asset`, `AssetExif`, `Folder`, `Album`, `User`, `RefreshToken`, `SearchPreset`, `SyncDirectoriesDefinition`, `ConvertDirectoriesDefinition`, `CatalogRunState`, `RecentTargetPath` — copy fields from the existing `domain/entity/` classes; remove all `jakarta.persistence.*` imports and JPA annotations; use Lombok `@Data` / `@Builder` / `@NoArgsConstructor`
- [x] 1.2 Create `application/dto/PaginatedResult.java` as a Java record: `record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}`
- [x] 1.3 Run `mvn test` — all tests must still pass (domain entity classes still exist in parallel)

## Phase 2 — Driven Port Interfaces (port/out)

- [x] 2.1 Create `domain/port/out/AssetRepository.java` — plain Java interface with methods: `Optional<Asset> findById(Long id)`, `Optional<Asset> findByFolderAndFileName(Folder folder, String fileName)`, `PaginatedResult<Asset> findFiltered(AssetFilter filter)`, `List<Asset> findByFolder(Folder folder)`, `List<Asset> findAll()`, `Asset save(Asset asset)`, `void delete(Long id)` — use domain model types only, no Spring or JPA imports
- [x] 2.2 Create `domain/port/out/AssetExifRepository.java`
- [x] 2.3 Create `domain/port/out/FolderRepository.java`
- [x] 2.4 Create `domain/port/out/AlbumRepository.java`
- [x] 2.5 Create `domain/port/out/UserRepository.java`
- [x] 2.6 Create `domain/port/out/RefreshTokenRepository.java`
- [x] 2.7 Create `domain/port/out/SearchPresetRepository.java`
- [x] 2.8 Create `domain/port/out/SyncConfigRepository.java`
- [x] 2.9 Create `domain/port/out/ConvertConfigRepository.java`
- [x] 2.10 Create `domain/port/out/CatalogStateRepository.java`
- [x] 2.11 Create `domain/port/out/RecentTargetPathRepository.java`
- [x] 2.12 Create `domain/port/out/StoragePort.java` — mirror the existing `domain/service/StorageService.java` interface signature but use domain model types; remove `java.awt.image.BufferedImage` from the port if it belongs in the adapter
- [x] 2.13 Create `domain/port/out/ThumbnailPort.java`
- [x] 2.14 Create `domain/port/out/HashCalculatorPort.java`
- [x] 2.15 Create `domain/port/out/JwtTokenPort.java`
- [x] 2.16 Create `application/dto/AssetFilter.java` record capturing: `folderId`, `search`, `dateFrom`, `dateTo`, `minRating`, `sortCriteria`, `page`, `pageSize`, `includeDeleted`
- [x] 2.17 Run `mvn test` — all tests still pass

## Phase 3 — Persistence Adapters

- [x] 3.1 Create `infrastructure/persistence/entity/` package; rename/copy each `domain/entity/*.java` to `infrastructure/persistence/entity/*Entity.java` — preserve all JPA annotations; update `@Table`, `@Entity`, `@ManyToOne` etc. to reference entity classes within the same package
- [x] 3.2 Create `infrastructure/persistence/jpa/` package; move each `domain/repository/*.java` interface to `infrastructure/persistence/jpa/Jpa*.java`; update generic type parameters to use `*Entity` classes
- [x] 3.3 Create `infrastructure/persistence/mapper/` with one mapper per entity type (e.g. `AssetEntityMapper`) converting between `AssetEntity` ↔ `Asset` domain model; use MapStruct `@Mapper(componentModel = "spring")` interfaces — hand-writing is not permitted; use `@Named` qualifiers when a mapper exposes multiple methods returning the same type (e.g. `toEntityRef` vs `toEntity`)
- [x] 3.4 Create `infrastructure/persistence/adapter/AssetRepositoryImpl.java` — annotate `@Service`; inject `JpaAssetRepository` + `AssetEntityMapper`; implement all methods of `AssetRepository` by delegating to the JPA repository and mapping results
- [x] 3.5 Create remaining implementation classes: `AssetExifRepositoryImpl`, `FolderRepositoryImpl`, `AlbumRepositoryImpl`, `UserRepositoryImpl`, `RefreshTokenRepositoryImpl`, `SearchPresetRepositoryImpl`, `SyncConfigRepositoryImpl`, `ConvertConfigRepositoryImpl`, `CatalogStateRepositoryImpl`, `RecentTargetPathRepositoryImpl`
- [x] 3.6 Delete `domain/entity/` package
- [x] 3.7 Delete `domain/repository/` package
- [x] 3.8 Update `infrastructure/service/` implementations (still in use by `PhotoManagerFacadeImpl`) to import from `infrastructure/persistence/jpa/` and `infrastructure/persistence/entity/` as an interim measure
- [x] 3.9 Run `mvn test` — all tests pass

## Phase 4 — Driving Port Interfaces and Use Case Implementations

Each use-case interface has exactly one method. Each implementation class implements exactly one interface and has exactly one business method. Annotate read implementations with `@Service @Transactional(readOnly = true)` and write implementations with `@Service @Transactional`. Inject only `domain/port/out/` interfaces; no Spring MVC, Spring Data, or HTTP types are permitted.

### 4a — asset/ subpackage (8 interfaces + 8 implementations)

- [x] 4a.1 Create `domain/port/in/asset/GetAssetsUseCase.java` — `PaginatedResult<Asset> execute(AssetFilter filter)`
- [x] 4a.2 Create `application/usecase/asset/GetAssetsUseCaseImpl.java` — migrate `getAssets()` logic from `PhotoManagerFacadeImpl`; replace `Page`/`PageRequest`/`Sort` with `PaginatedResult` + `AssetFilter`
- [x] 4a.3 Create `domain/port/in/asset/GetAssetImageUseCase.java` — `AssetImage execute(Long assetId)`
- [x] 4a.4 Create `application/usecase/asset/GetAssetImageUseCaseImpl.java`
- [x] 4a.5 Create `domain/port/in/asset/GetAssetExifUseCase.java` — `AssetExif execute(Long assetId)`
- [x] 4a.6 Create `application/usecase/asset/GetAssetExifUseCaseImpl.java`
- [x] 4a.7 Create `domain/port/in/asset/DownloadAssetsUseCase.java` — `void execute(List<Long> assetIds, OutputStream out)`
- [x] 4a.8 Create `application/usecase/asset/DownloadAssetsUseCaseImpl.java`
- [x] 4a.9 Create `domain/port/in/asset/RateAssetUseCase.java` — `void execute(Long assetId, int rating)`
- [x] 4a.10 Create `application/usecase/asset/RateAssetUseCaseImpl.java`
- [x] 4a.11 Create `domain/port/in/asset/MoveAssetsUseCase.java` — `void execute(List<Long> assetIds, String destinationPath, boolean preserveOriginal)`
- [x] 4a.12 Create `application/usecase/asset/MoveAssetsUseCaseImpl.java`; validate destination is within configured `rootCatalogFolders`; update recent target paths
- [x] 4a.13 Create `domain/port/in/asset/UploadAssetUseCase.java` — `void execute(String folderPath, String fileName, byte[] content)`
- [x] 4a.14 Create `application/usecase/asset/UploadAssetUseCaseImpl.java`; replace `MultipartFile` with `byte[]` (conversion done in the controller mapper)
- [x] 4a.15 Create `domain/port/in/asset/DeleteAssetsUseCase.java` — `void execute(List<Long> assetIds, boolean permanently)`
- [x] 4a.16 Create `application/usecase/asset/DeleteAssetsUseCaseImpl.java`

### 4b — catalog/ subpackage (2 interfaces + 2 implementations)

- [x] 4b.1 Create `domain/port/in/catalog/CatalogAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener)`
- [x] 4b.2 Create `application/usecase/catalog/CatalogAssetsUseCaseImpl.java`; migrate catalog-run logic including distributed lock acquisition and heartbeat
- [x] 4b.3 Create `domain/port/in/catalog/GetDuplicatedAssetsUseCase.java` — `List<List<Asset>> execute()`
- [x] 4b.4 Create `application/usecase/catalog/GetDuplicatedAssetsUseCaseImpl.java`

### 4c — album/ subpackage (7 interfaces + 7 implementations)

- [x] 4c.1 Create `domain/port/in/album/GetAlbumsUseCase.java` — `PaginatedResult<AlbumData> execute(UUID userId, int page)`
- [x] 4c.2 Create `application/usecase/album/GetAlbumsUseCaseImpl.java`
- [x] 4c.3 Create `domain/port/in/album/CreateAlbumUseCase.java` — `AlbumData execute(UUID userId, String name, String description)`
- [x] 4c.4 Create `application/usecase/album/CreateAlbumUseCaseImpl.java`
- [x] 4c.5 Create `domain/port/in/album/GetAlbumUseCase.java` — `AlbumData execute(Long albumId, UUID userId, int page)`
- [x] 4c.6 Create `application/usecase/album/GetAlbumUseCaseImpl.java`
- [x] 4c.7 Create `domain/port/in/album/UpdateAlbumUseCase.java` — `AlbumData execute(Long albumId, UUID userId, String name, String description)`
- [x] 4c.8 Create `application/usecase/album/UpdateAlbumUseCaseImpl.java`
- [x] 4c.9 Create `domain/port/in/album/DeleteAlbumUseCase.java` — `void execute(Long albumId, UUID userId)`
- [x] 4c.10 Create `application/usecase/album/DeleteAlbumUseCaseImpl.java`
- [x] 4c.11 Create `domain/port/in/album/AddAssetsToAlbumUseCase.java` — `void execute(Long albumId, UUID userId, List<Long> assetIds)`
- [x] 4c.12 Create `application/usecase/album/AddAssetsToAlbumUseCaseImpl.java`
- [x] 4c.13 Create `domain/port/in/album/RemoveAssetsFromAlbumUseCase.java` — `void execute(Long albumId, UUID userId, List<Long> assetIds)`
- [x] 4c.14 Create `application/usecase/album/RemoveAssetsFromAlbumUseCaseImpl.java`

### 4d — sync/ subpackage (3 interfaces + 3 implementations)

- [x] 4d.1 Create `domain/port/in/sync/GetSyncConfigUseCase.java` — `List<SyncDirectoriesDefinition> execute()`
- [x] 4d.2 Create `application/usecase/sync/GetSyncConfigUseCaseImpl.java`
- [x] 4d.3 Create `domain/port/in/sync/SaveSyncConfigUseCase.java` — `void execute(List<SyncDirectoriesDefinition> definitions)`
- [x] 4d.4 Create `application/usecase/sync/SaveSyncConfigUseCaseImpl.java`
- [x] 4d.5 Create `domain/port/in/sync/SyncAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<SyncAssetsResult> listener)`
- [x] 4d.6 Create `application/usecase/sync/SyncAssetsUseCaseImpl.java`

### 4e — convert/ subpackage (3 interfaces + 3 implementations)

- [x] 4e.1 Create `domain/port/in/convert/GetConvertConfigUseCase.java` — `List<ConvertDirectoriesDefinition> execute()`
- [x] 4e.2 Create `application/usecase/convert/GetConvertConfigUseCaseImpl.java`
- [x] 4e.3 Create `domain/port/in/convert/SaveConvertConfigUseCase.java` — `void execute(List<ConvertDirectoriesDefinition> definitions)`
- [x] 4e.4 Create `application/usecase/convert/SaveConvertConfigUseCaseImpl.java`
- [x] 4e.5 Create `domain/port/in/convert/ConvertAssetsUseCase.java` — `CompletableFuture<Void> execute(Consumer<ConvertAssetsResult> listener)`
- [x] 4e.6 Create `application/usecase/convert/ConvertAssetsUseCaseImpl.java`

### 4f — folder/ subpackage (4 interfaces + 4 implementations)

- [x] 4f.1 Create `domain/port/in/folder/GetSubFoldersUseCase.java` — `List<Folder> execute(String parentPath)`
- [x] 4f.2 Create `application/usecase/folder/GetSubFoldersUseCaseImpl.java`
- [x] 4f.3 Create `domain/port/in/folder/GetDrivesUseCase.java` — `List<String> execute()`
- [x] 4f.4 Create `application/usecase/folder/GetDrivesUseCaseImpl.java`
- [x] 4f.5 Create `domain/port/in/folder/GetInitialFolderUseCase.java` — `String execute()`
- [x] 4f.6 Create `application/usecase/folder/GetInitialFolderUseCaseImpl.java`
- [x] 4f.7 Create `domain/port/in/folder/GetRecentTargetPathsUseCase.java` — `List<String> execute()`
- [x] 4f.8 Create `application/usecase/folder/GetRecentTargetPathsUseCaseImpl.java`

### 4g — recycle/ subpackage (3 interfaces + 3 implementations)

- [x] 4g.1 Create `domain/port/in/recycle/GetDeletedAssetsUseCase.java` — `PaginatedResult<Asset> execute(int page)`
- [x] 4g.2 Create `application/usecase/recycle/GetDeletedAssetsUseCaseImpl.java`
- [x] 4g.3 Create `domain/port/in/recycle/RestoreAssetsUseCase.java` — `void execute(List<Long> assetIds)`
- [x] 4g.4 Create `application/usecase/recycle/RestoreAssetsUseCaseImpl.java`
- [x] 4g.5 Create `domain/port/in/recycle/PurgeAssetsUseCase.java` — `void execute(List<Long> assetIds)`
- [x] 4g.6 Create `application/usecase/recycle/PurgeAssetsUseCaseImpl.java`

### 4h — search/ subpackage (3 interfaces + 3 implementations)

- [x] 4h.1 Create `domain/port/in/search/GetSearchPresetsUseCase.java` — `List<SearchPreset> execute(UUID userId)`
- [x] 4h.2 Create `application/usecase/search/GetSearchPresetsUseCaseImpl.java`
- [x] 4h.3 Create `domain/port/in/search/CreateSearchPresetUseCase.java` — `SearchPreset execute(UUID userId, String name, FilterPreset criteria)`
- [x] 4h.4 Create `application/usecase/search/CreateSearchPresetUseCaseImpl.java`
- [x] 4h.5 Create `domain/port/in/search/DeleteSearchPresetUseCase.java` — `void execute(Long presetId, UUID userId)`
- [x] 4h.6 Create `application/usecase/search/DeleteSearchPresetUseCaseImpl.java`

### 4i — home/ subpackage (1 interface + 1 implementation)

- [x] 4i.1 Create `domain/port/in/home/GetHomeStatsUseCase.java` — `HomeStats execute()`
- [x] 4i.2 Create `application/usecase/home/GetHomeStatsUseCaseImpl.java`

### 4j — user/ subpackage (4 interfaces + 4 implementations)

- [x] 4j.1 Create `domain/port/in/user/ListUsersUseCase.java` — `List<UserSummary> execute()`
- [x] 4j.2 Create `application/usecase/user/ListUsersUseCaseImpl.java`
- [x] 4j.3 Create `domain/port/in/user/CreateUserUseCase.java` — `UserSummary execute(String username, String password, String role)`
- [x] 4j.4 Create `application/usecase/user/CreateUserUseCaseImpl.java`
- [x] 4j.5 Create `domain/port/in/user/UpdatePasswordUseCase.java` — `void execute(UUID userId, String newPassword)`
- [x] 4j.6 Create `application/usecase/user/UpdatePasswordUseCaseImpl.java`
- [x] 4j.7 Create `domain/port/in/user/DeleteUserUseCase.java` — `void execute(UUID userId)`
- [x] 4j.8 Create `application/usecase/user/DeleteUserUseCaseImpl.java`

### 4k — cleanup

- [x] 4k.1 Delete `application/PhotoManagerFacade.java` and `application/PhotoManagerFacadeImpl.java`
- [x] 4k.2 Run `mvn test` — all tests pass (controllers still broken at this point; use-case unit tests must pass)

## Phase 5 — Web Adapters (HTTP Layer)

- [x] 5.1 Create `infrastructure/web/controller/`, `infrastructure/web/dto/`, `infrastructure/web/mapper/`, `infrastructure/web/exception/` packages
- [x] 5.2 Move all classes from `api/dto/` → `infrastructure/web/dto/`; update imports throughout
- [x] 5.3 Move all classes from `api/exception/` → `infrastructure/web/exception/`; update imports
- [x] 5.4 Create `infrastructure/web/mapper/AssetDtoMapper.java` as a MapStruct `@Mapper(componentModel = "spring")` interface — converts `Asset` domain model ↔ `AssetDto`; convert `MultipartFile` → `byte[]` + filename in a `default` method before calling use case
- [x] 5.5 Create `infrastructure/web/mapper/AlbumDtoMapper.java` and `FolderDtoMapper.java` as MapStruct `@Mapper(componentModel = "spring")` interfaces
- [x] 5.6 Move `api/AssetController.java` → `infrastructure/web/controller/AssetController.java`; replace `@Autowired PhotoManagerFacade` with individual use-case injections (`GetAssetsUseCase`, `GetAssetImageUseCase`, `GetAssetExifUseCase`, `DownloadAssetsUseCase`, `RateAssetUseCase`, `MoveAssetsUseCase`, `UploadAssetUseCase`, `DeleteAssetsUseCase`, `CatalogAssetsUseCase`, `GetDuplicatedAssetsUseCase`); each controller handler calls `useCase.execute(…)` and delegates HTTP↔domain conversion to `AssetDtoMapper`
- [x] 5.7 Move and update `AlbumController`, `AuthController`, `ConvertController`, `FolderController`, `HomeController`, `RecycleBinController`, `SearchPresetController`, `SyncController`, `UserAdminController` following the same pattern as 5.6
- [x] 5.8 Move `api/GlobalExceptionHandler.java`, `api/LoginResponse.java`, `api/AuthRequest.java`, `api/CreateUserRequest.java`, `api/UpdatePasswordRequest.java`, `api/ErrorResponse.java` to `infrastructure/web/`
- [x] 5.9 Delete `api/` package
- [x] 5.10 Run `mvn test` — all tests pass

## Phase 6 — Service Adapters

- [x] 6.1 Rename `infrastructure/service/StorageServiceImpl.java` → `StorageServiceAdapter.java`; annotate class as implementing `StoragePort`; remove the old `domain/service/StorageService` interface if it is now unused
- [x] 6.2 Rename `ThumbnailStorageServiceImpl` → `ThumbnailStorageServiceAdapter`; implement `ThumbnailPort`
- [x] 6.3 Rename `AssetHashCalculatorService` → `AssetHashCalculatorAdapter`; implement `HashCalculatorPort`
- [x] 6.4 Create `JwtTokenAdapter` implementing `JwtTokenPort`; delegate to `JwtUtil`
- [x] 6.5 Delete the now-unused interfaces from `domain/service/`: `StorageService`, `ThumbnailStorageService`, `CatalogAssetsService`, `SyncAssetsService`, `ConvertAssetsService`, `FindDuplicatedAssetsService`, `MoveAssetsService`, `AlbumService`, `RecycleBinService`, `SearchPresetService`, `JwtTokenService`, `CatalogFolderService`, `UserService`, `UserAdminService`, `RefreshTokenService`, `ExifMetadata`
- [x] 6.6 Delete `domain/service/` package once empty
- [x] 6.7 Run `mvn test` — all tests pass

## Phase 7 — Verification and Documentation

- [x] 7.1 Run `mvn clean package` — build succeeds with no warnings
- [x] 7.2 Run `mvn test` — all tests pass
- [x] 7.3 Verify that no class in `domain/` imports from `jakarta.*`, `org.springframework.*`, or `infrastructure.*` (use `grep -r "import jakarta\|import org.springframework\|import.*infrastructure" src/main/java/com/jpablodrexler/photomanager/domain/` — must return empty)
- [x] 7.4 Verify that no class in `application/usecase/` imports from `infrastructure.*` or `org.springframework.web.*` or `org.springframework.data.*`
- [x] 7.5 Start the application locally (`mvn spring-boot:run`), authenticate as `admin/admin`, and verify: gallery loads, catalog runs, album creation works, sync config saves, duplicate detection works
- [x] 7.6 Update `CLAUDE.md` — replace the **Backend** architecture section under _Web Architecture_ with the new hexagonal layout:
  - Update the package tree diagram to show `domain/model/`, `domain/port/in/`, `domain/port/out/`, `application/usecase/`, `infrastructure/persistence/` (entity, jpa, adapter, mapper), `infrastructure/web/` (controller, dto, mapper, exception), and `infrastructure/service/`
  - Update the dependency-flow line to `infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service`
  - Update the _Key domain services_ prose to describe use-case interfaces instead of the old service interfaces; remove references to `PhotoManagerFacade`
  - Add a **Key Conventions** entry: repository interfaces in `domain/port/out/` use the `Repository` suffix; their infrastructure implementations in `infrastructure/persistence/adapter/` use the `RepositoryImpl` suffix; service ports in `domain/port/out/` use the `Port` suffix; their infrastructure implementations use the `ServiceAdapter` suffix (e.g. `StorageServiceAdapter implements StoragePort`)
  - Add a **Key Conventions** entry: all entity ↔ domain model and HTTP DTO ↔ domain model mappings are implemented as MapStruct `@Mapper(componentModel = "spring")` interfaces in `infrastructure/persistence/mapper/` and `infrastructure/web/mapper/` respectively; hand-writing mappers is not permitted; use `@Named` qualifiers when a mapper interface exposes multiple methods returning the same type (e.g. `toEntityRef` for FK-only references vs `toEntity` for full mapping)
- [x] 7.7 Update `README.md` — add or replace the backend architecture diagram (ASCII or Mermaid) to reflect the hexagonal structure:
  - Document the `domain/port/in/` (use-case interfaces) and `domain/port/out/` (repository and service port interfaces) conventions
  - Document the naming convention: `XxxRepository` / `XxxRepositoryImpl` for persistence ports; `XxxPort` / `XxxServiceAdapter` for service ports
  - Add a note that controllers in `infrastructure/web/controller/` delegate directly to use-case interfaces and never touch repositories or service adapters directly
  - Add a note that all entity↔domain and DTO↔domain conversions go through MapStruct-generated mappers; explain the `toEntityRef` pattern (id-only FK reference to avoid accidental updates to the referenced row)
