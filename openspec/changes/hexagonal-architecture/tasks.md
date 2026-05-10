## Phase 1 — Domain Models

- [ ] 1.1 Create `domain/model/` package with plain POJO classes for each entity: `Asset`, `AssetExif`, `Folder`, `Album`, `User`, `RefreshToken`, `SearchPreset`, `SyncDirectoriesDefinition`, `ConvertDirectoriesDefinition`, `CatalogRunState`, `RecentTargetPath` — copy fields from the existing `domain/entity/` classes; remove all `jakarta.persistence.*` imports and JPA annotations; use Lombok `@Data` / `@Builder` / `@NoArgsConstructor`
- [ ] 1.2 Create `application/dto/PaginatedResult.java` as a Java record: `record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}`
- [ ] 1.3 Run `mvn test` — all tests must still pass (domain entity classes still exist in parallel)

## Phase 2 — Driven Port Interfaces (port/out)

- [ ] 2.1 Create `domain/port/out/AssetRepositoryPort.java` — plain Java interface with methods: `Optional<Asset> findById(Long id)`, `Optional<Asset> findByFolderAndFileName(Folder folder, String fileName)`, `PaginatedResult<Asset> findFiltered(AssetFilter filter)`, `List<Asset> findByFolder(Folder folder)`, `List<Asset> findAll()`, `Asset save(Asset asset)`, `void delete(Long id)` — use domain model types only, no Spring or JPA imports
- [ ] 2.2 Create `domain/port/out/AssetExifRepositoryPort.java`
- [ ] 2.3 Create `domain/port/out/FolderRepositoryPort.java`
- [ ] 2.4 Create `domain/port/out/AlbumRepositoryPort.java`
- [ ] 2.5 Create `domain/port/out/UserRepositoryPort.java`
- [ ] 2.6 Create `domain/port/out/RefreshTokenRepositoryPort.java`
- [ ] 2.7 Create `domain/port/out/SearchPresetRepositoryPort.java`
- [ ] 2.8 Create `domain/port/out/SyncConfigRepositoryPort.java`
- [ ] 2.9 Create `domain/port/out/ConvertConfigRepositoryPort.java`
- [ ] 2.10 Create `domain/port/out/CatalogStateRepositoryPort.java`
- [ ] 2.11 Create `domain/port/out/RecentTargetPathRepositoryPort.java`
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
- [ ] 3.4 Create `infrastructure/persistence/adapter/AssetRepositoryAdapter.java` — annotate `@Service`; inject `JpaAssetRepository` + `AssetEntityMapper`; implement all methods of `AssetRepositoryPort` by delegating to the JPA repository and mapping results
- [ ] 3.5 Create remaining adapter classes: `AssetExifRepositoryAdapter`, `FolderRepositoryAdapter`, `AlbumRepositoryAdapter`, `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, `SearchPresetRepositoryAdapter`, `SyncConfigRepositoryAdapter`, `ConvertConfigRepositoryAdapter`, `CatalogStateRepositoryAdapter`, `RecentTargetPathRepositoryAdapter`
- [ ] 3.6 Delete `domain/entity/` package
- [ ] 3.7 Delete `domain/repository/` package
- [ ] 3.8 Update `infrastructure/service/` implementations (still in use by `PhotoManagerFacadeImpl`) to import from `infrastructure/persistence/jpa/` and `infrastructure/persistence/entity/` as an interim measure
- [ ] 3.9 Run `mvn test` — all tests pass

## Phase 4 — Driving Port Interfaces and Use Case Implementations

- [ ] 4.1 Create `domain/port/in/GetAssetsUseCase.java` — methods: `PaginatedResult<Asset> getAssets(AssetFilter filter)`, `AssetImage getAssetImage(Long assetId)`, `AssetExif getAssetExif(Long assetId)`, `void downloadAssets(List<Long> assetIds, OutputStream out)`
- [ ] 4.2 Create `domain/port/in/MutateAssetsUseCase.java` — methods: `void rateAsset(Long assetId, int rating)`, `void moveAssets(List<Long> assetIds, String destinationPath, boolean preserveOriginal)`, `void uploadAsset(String folderPath, String fileName, byte[] content)`, `void deleteAssets(List<Long> assetIds, boolean permanently)`
- [ ] 4.3 Create `domain/port/in/CatalogAssetsUseCase.java`
- [ ] 4.4 Create `domain/port/in/GetDuplicatedAssetsUseCase.java`
- [ ] 4.5 Create `domain/port/in/ManageAlbumsUseCase.java`
- [ ] 4.6 Create `domain/port/in/SyncAssetsUseCase.java`
- [ ] 4.7 Create `domain/port/in/ConvertAssetsUseCase.java`
- [ ] 4.8 Create `domain/port/in/GetFoldersUseCase.java`
- [ ] 4.9 Create `domain/port/in/RecycleBinUseCase.java`
- [ ] 4.10 Create `domain/port/in/ManageSearchPresetsUseCase.java`
- [ ] 4.11 Create `domain/port/in/GetHomeStatsUseCase.java`
- [ ] 4.12 Create `domain/port/in/UserAdminUseCase.java`
- [ ] 4.13 Create `application/usecase/GetAssetsUseCaseImpl.java` — annotate `@Service @Transactional(readOnly = true)` only; inject only `domain/port/out/` interfaces; migrate logic from `PhotoManagerFacadeImpl` corresponding methods; replace `Page`/`PageRequest` with `PaginatedResult` + `AssetFilter`; replace `MultipartFile` with `byte[]` and `String fileName`
- [ ] 4.14 Create remaining use-case implementations following the same rules: `MutateAssetsUseCaseImpl`, `CatalogAssetsUseCaseImpl`, `GetDuplicatedAssetsUseCaseImpl`, `ManageAlbumsUseCaseImpl`, `SyncAssetsUseCaseImpl`, `ConvertAssetsUseCaseImpl`, `GetFoldersUseCaseImpl`, `RecycleBinUseCaseImpl`, `ManageSearchPresetsUseCaseImpl`, `GetHomeStatsUseCaseImpl`, `UserAdminUseCaseImpl`
- [ ] 4.15 Delete `application/PhotoManagerFacade.java` and `application/PhotoManagerFacadeImpl.java`
- [ ] 4.16 Run `mvn test` — all tests pass (controllers still broken; unit tests for use cases must pass)

## Phase 5 — Web Adapters (HTTP Layer)

- [ ] 5.1 Create `infrastructure/web/controller/`, `infrastructure/web/dto/`, `infrastructure/web/mapper/`, `infrastructure/web/exception/` packages
- [ ] 5.2 Move all classes from `api/dto/` → `infrastructure/web/dto/`; update imports throughout
- [ ] 5.3 Move all classes from `api/exception/` → `infrastructure/web/exception/`; update imports
- [ ] 5.4 Create `infrastructure/web/mapper/AssetDtoMapper.java` — converts `Asset` domain model ↔ `AssetDto`; convert `MultipartFile` → `byte[]` + filename before calling use case
- [ ] 5.5 Create `infrastructure/web/mapper/AlbumDtoMapper.java` and `FolderDtoMapper.java`
- [ ] 5.6 Move `api/AssetController.java` → `infrastructure/web/controller/AssetController.java`; replace `@Autowired PhotoManagerFacade` injection with individual use-case interface injections (`GetAssetsUseCase`, `MutateAssetsUseCase`, `CatalogAssetsUseCase`, `GetDuplicatedAssetsUseCase`); update all method calls to use the new use-case interfaces; use `AssetDtoMapper` for conversions
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
- [ ] 7.6 Update `JPPhotoManagerWeb/CLAUDE.md` — replace the architecture section and package tree with the new hexagonal structure
- [ ] 7.7 Update `JPPhotoManagerWeb/README.md` — replace the Backend Layer Architecture Mermaid diagram with the hexagonal one; update the dependency flow description
