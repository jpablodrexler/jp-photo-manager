## Context

The web backend (`JPPhotoManagerWeb/backend`) currently follows a layered "Clean Architecture" pattern:

```
api/           → REST controllers + DTOs
application/   → PhotoManagerFacade (single god interface + implementation)
domain/        → JPA entities, Spring Data repository interfaces, service interfaces
infrastructure/→ Service implementations
```

The overall intent is sound — infrastructure implements domain interfaces — but three concrete leaks prevent it from being true Hexagonal Architecture:

1. **Domain entities carry JPA annotations.** `domain/entity/Asset.java` imports `jakarta.persistence.*` and carries `@Entity`, `@Table`, `@ManyToOne`, etc. The domain should have no knowledge of the ORM.
2. **Repository interfaces extend Spring Data.** `domain/repository/AssetRepository.java` extends `JpaRepository<Asset, Long>` and returns `org.springframework.data.domain.Page`. Port interfaces must be plain Java.
3. **The application layer touches both the API layer and Spring.** `PhotoManagerFacadeImpl` imports `org.springframework.web.multipart.MultipartFile`, Spring `Page`/`PageRequest`/`Sort`, and `com.jpablodrexler.photomanager.api.dto.*` — a dependency inversion violation and a framework leak.

## Goals / Non-Goals

**Goals:**
- Domain layer contains zero `jakarta.*`, `org.springframework.*`, or `com.jpablodrexler.photomanager.api.*` imports.
- Use-case implementations in `application/usecase/` carry zero Spring annotations.
- All framework wiring (JPA, Spring MVC, Spring Security, transactions, async) lives exclusively in the `infrastructure/` tree.
- One use-case interface with exactly one method per operation, grouped by domain concept in subpackages under `domain/port/in/`; one implementation class per interface in mirrored subpackages under `application/usecase/`.
- Existing REST API surface, database schema, and behaviour are preserved exactly.

**Non-Goals:**
- Changing the PostgreSQL schema or Flyway migrations.
- Modifying the frontend.
- Changing any public REST endpoint path, method, or payload shape.
- Converting to a multi-module Maven build (all code stays in one Maven module).

## Decisions

### 1. Single Maven module — package-based hexagon boundary

**Decision:** Enforce the hexagonal boundary via package naming and import discipline, not Maven module isolation.

**Rationale:** Splitting into Maven modules (e.g. `domain-module`, `application-module`, `infrastructure-module`) would enforce the boundary at compile time but would significantly increase the scope and risk of this change. The existing codebase does not use modules, and adding them would require restructuring `pom.xml`, moving resources, and updating dozens of import paths simultaneously. Package-based discipline, enforced by code review and a linting rule (e.g. ArchUnit), achieves the same goal incrementally.

**Alternatives considered:**
- *Multi-module Maven build*: stronger compile-time enforcement, but high migration cost and out of scope for this change.

### 2. One use-case interface, one method, one implementation class — grouped by subpackage

**Decision:** Replace the single `PhotoManagerFacade` (40+ methods) with 38 single-method use-case interfaces in `domain/port/in/`, each implemented by exactly one class in `application/usecase/`. Both are organised into the same subpackage hierarchy by domain concept (`asset/`, `catalog/`, `album/`, `sync/`, `convert/`, `folder/`, `recycle/`, `search/`, `home/`, `user/`).

**Rationale:** Applying the Interface Segregation Principle strictly means each controller method depends on exactly one interface with exactly one method — no unrelated operations are dragged in. One implementation class per interface keeps responsibilities focused and makes individual use cases independently replaceable and testable without Mockito noise from sibling methods.

**Alternatives considered:**
- *Multi-method grouped interfaces (e.g. `ManageAlbumsUseCase` with 7 methods)*: reduces file count but reintroduces a smaller god interface; controllers still depend on methods they never call.
- *One implementation class per subpackage group*: allows one class to implement multiple interfaces; reduces class count but couples unrelated operations within the same file, making future extraction harder.

**Use-case inventory (38 total):**

| Subpackage | Interfaces (one method each) | Controller |
|---|---|---|
| `asset/` | `GetAssetsUseCase`, `GetAssetImageUseCase`, `GetAssetExifUseCase`, `DownloadAssetsUseCase`, `RateAssetUseCase`, `MoveAssetsUseCase`, `UploadAssetUseCase`, `DeleteAssetsUseCase` | `AssetController` |
| `catalog/` | `CatalogAssetsUseCase`, `GetDuplicatedAssetsUseCase` | `AssetController`, `CatalogScheduler` |
| `album/` | `GetAlbumsUseCase`, `CreateAlbumUseCase`, `GetAlbumUseCase`, `UpdateAlbumUseCase`, `DeleteAlbumUseCase`, `AddAssetsToAlbumUseCase`, `RemoveAssetsFromAlbumUseCase` | `AlbumController` |
| `sync/` | `GetSyncConfigUseCase`, `SaveSyncConfigUseCase`, `SyncAssetsUseCase` | `SyncController` |
| `convert/` | `GetConvertConfigUseCase`, `SaveConvertConfigUseCase`, `ConvertAssetsUseCase` | `ConvertController` |
| `folder/` | `GetSubFoldersUseCase`, `GetDrivesUseCase`, `GetInitialFolderUseCase`, `GetRecentTargetPathsUseCase` | `FolderController` |
| `recycle/` | `GetDeletedAssetsUseCase`, `RestoreAssetsUseCase`, `PurgeAssetsUseCase` | `RecycleBinController` |
| `search/` | `GetSearchPresetsUseCase`, `CreateSearchPresetUseCase`, `DeleteSearchPresetUseCase` | `SearchPresetController` |
| `home/` | `GetHomeStatsUseCase` | `HomeController` |
| `user/` | `ListUsersUseCase`, `CreateUserUseCase`, `UpdatePasswordUseCase`, `DeleteUserUseCase` | `UserAdminController` |

### 3. Separate domain models from JPA entities

**Decision:** Create pure POJO classes in `domain/model/` and move JPA-annotated classes to `infrastructure/persistence/entity/`. Add mappers in `infrastructure/persistence/mapper/`.

**Rationale:** This is the most invasive part of the change but is required for the domain to be framework-free. MapStruct `@Mapper(componentModel = "spring")` interfaces convert between the two representations at the adapter boundary; hand-writing is not permitted.

### 4. Repository ports use plain Java types only

**Decision:** Port interfaces in `domain/port/out/` use `List<T>`, `Optional<T>`, and a custom `PaginatedResult<T>` record instead of `Page<T>` and `Pageable`.

**Rationale:** `Page` and `Pageable` are Spring Data types. The domain must not know about them. `PaginatedResult<T>` is a simple record: `record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}`. The adapter translates between the two.

### 5. One implementation class per use-case interface — `@Service @Transactional` only

**Decision:** Each use-case interface has exactly one implementation class in the mirrored `application/usecase/<subpackage>/` path. Implementation classes carry `@Service` and `@Transactional` (or `@Transactional(readOnly = true)`) and nothing else from Spring. All other Spring types (`@Value`, `MultipartFile`, `Page`, etc.) remain out.

**Rationale:** One class per interface enforces Single Responsibility and makes each use case independently navigable, testable, and replaceable. `@Service` and `@Transactional` are the minimum required to participate in Spring's DI and transaction management without importing framework types into business logic. Mockito ignores these annotations, so unit tests remain framework-free.

**Alternatives considered:**
- *One implementation class per subpackage, implementing multiple interfaces*: keeps file count lower but reintroduces coupling between operations that happen to live in the same package.
- *`@Configuration` + `@Bean` factory methods*: achieves fully annotation-free use cases but requires one factory method per class — significant boilerplate for 38 classes.

### 6. `api/` is renamed and relocated to `infrastructure/web/`

**Decision:** All classes currently in `api/` (controllers, DTOs, exceptions) move to `infrastructure/web/controller/`, `infrastructure/web/dto/`, and `infrastructure/web/exception/`. HTTP-to-domain mappers go in `infrastructure/web/mapper/` as MapStruct `@Mapper(componentModel = "spring")` interfaces.

**Rationale:** Controllers are adapters — they translate HTTP concepts to use-case calls and domain types to HTTP responses. Placing them under `infrastructure/web/` makes the adapter role explicit in the package name.

## Target Package Structure

```
com.jpablodrexler.photomanager/
│
├── PhotoManagerApplication.java
│
├── domain/                                       ← pure Java; zero framework imports
│   ├── model/                                    ← plain POJO domain objects
│   │   ├── Asset.java
│   │   ├── AssetExif.java
│   │   ├── Folder.java
│   │   ├── Album.java
│   │   ├── User.java
│   │   ├── SearchPreset.java
│   │   ├── SyncDirectoriesDefinition.java
│   │   ├── ConvertDirectoriesDefinition.java
│   │   └── CatalogRunState.java
│   ├── port/
│   │   ├── in/                                   ← one interface per use case, one method each
│   │   │   ├── asset/
│   │   │   │   ├── GetAssetsUseCase.java
│   │   │   │   ├── GetAssetImageUseCase.java
│   │   │   │   ├── GetAssetExifUseCase.java
│   │   │   │   ├── DownloadAssetsUseCase.java
│   │   │   │   ├── RateAssetUseCase.java
│   │   │   │   ├── MoveAssetsUseCase.java
│   │   │   │   ├── UploadAssetUseCase.java
│   │   │   │   └── DeleteAssetsUseCase.java
│   │   │   ├── catalog/
│   │   │   │   ├── CatalogAssetsUseCase.java
│   │   │   │   └── GetDuplicatedAssetsUseCase.java
│   │   │   ├── album/
│   │   │   │   ├── GetAlbumsUseCase.java
│   │   │   │   ├── CreateAlbumUseCase.java
│   │   │   │   ├── GetAlbumUseCase.java
│   │   │   │   ├── UpdateAlbumUseCase.java
│   │   │   │   ├── DeleteAlbumUseCase.java
│   │   │   │   ├── AddAssetsToAlbumUseCase.java
│   │   │   │   └── RemoveAssetsFromAlbumUseCase.java
│   │   │   ├── sync/
│   │   │   │   ├── GetSyncConfigUseCase.java
│   │   │   │   ├── SaveSyncConfigUseCase.java
│   │   │   │   └── SyncAssetsUseCase.java
│   │   │   ├── convert/
│   │   │   │   ├── GetConvertConfigUseCase.java
│   │   │   │   ├── SaveConvertConfigUseCase.java
│   │   │   │   └── ConvertAssetsUseCase.java
│   │   │   ├── folder/
│   │   │   │   ├── GetSubFoldersUseCase.java
│   │   │   │   ├── GetDrivesUseCase.java
│   │   │   │   ├── GetInitialFolderUseCase.java
│   │   │   │   └── GetRecentTargetPathsUseCase.java
│   │   │   ├── recycle/
│   │   │   │   ├── GetDeletedAssetsUseCase.java
│   │   │   │   ├── RestoreAssetsUseCase.java
│   │   │   │   └── PurgeAssetsUseCase.java
│   │   │   ├── search/
│   │   │   │   ├── GetSearchPresetsUseCase.java
│   │   │   │   ├── CreateSearchPresetUseCase.java
│   │   │   │   └── DeleteSearchPresetUseCase.java
│   │   │   ├── home/
│   │   │   │   └── GetHomeStatsUseCase.java
│   │   │   └── user/
│   │   │       ├── ListUsersUseCase.java
│   │   │       ├── CreateUserUseCase.java
│   │   │       ├── UpdatePasswordUseCase.java
│   │   │       └── DeleteUserUseCase.java
│   │   └── out/                                  ← secondary / driven port interfaces
│   │       ├── AssetRepository.java
│   │       ├── AssetExifRepository.java
│   │       ├── FolderRepository.java
│   │       ├── AlbumRepository.java
│   │       ├── UserRepository.java
│   │       ├── RefreshTokenRepository.java
│   │       ├── SearchPresetRepository.java
│   │       ├── SyncConfigRepository.java
│   │       ├── ConvertConfigRepository.java
│   │       ├── CatalogStateRepository.java
│   │       ├── RecentTargetPathRepository.java
│   │       ├── StoragePort.java
│   │       ├── ThumbnailPort.java
│   │       ├── HashCalculatorPort.java
│   │       └── JwtTokenPort.java
│   ├── service/                                  ← stateless pure-domain logic
│   │   └── FindDuplicatedAssetsService.java
│   └── enums/                                    ← unchanged
│       ├── ImageRotation.java
│       ├── ReasonEnum.java
│       ├── SortCriteria.java
│       └── WallpaperStyle.java
│
├── application/
│   ├── dto/                                      ← framework-free application DTOs
│   │   ├── AssetFilter.java
│   │   ├── PaginatedResult.java
│   │   ├── CatalogChangeNotification.java
│   │   ├── SyncAssetsResult.java
│   │   ├── ConvertAssetsResult.java
│   │   ├── HomeStats.java
│   │   ├── AlbumData.java
│   │   ├── AssetImage.java
│   │   ├── FilterPreset.java
│   │   └── UserSummary.java
│   └── usecase/                                  ← one class per interface, one method each
│       ├── asset/
│       │   ├── GetAssetsUseCaseImpl.java          ← @Service @Transactional(readOnly=true)
│       │   ├── GetAssetImageUseCaseImpl.java
│       │   ├── GetAssetExifUseCaseImpl.java
│       │   ├── DownloadAssetsUseCaseImpl.java
│       │   ├── RateAssetUseCaseImpl.java          ← @Service @Transactional
│       │   ├── MoveAssetsUseCaseImpl.java
│       │   ├── UploadAssetUseCaseImpl.java
│       │   └── DeleteAssetsUseCaseImpl.java
│       ├── catalog/
│       │   ├── CatalogAssetsUseCaseImpl.java
│       │   └── GetDuplicatedAssetsUseCaseImpl.java
│       ├── album/
│       │   ├── GetAlbumsUseCaseImpl.java
│       │   ├── CreateAlbumUseCaseImpl.java
│       │   ├── GetAlbumUseCaseImpl.java
│       │   ├── UpdateAlbumUseCaseImpl.java
│       │   ├── DeleteAlbumUseCaseImpl.java
│       │   ├── AddAssetsToAlbumUseCaseImpl.java
│       │   └── RemoveAssetsFromAlbumUseCaseImpl.java
│       ├── sync/
│       │   ├── GetSyncConfigUseCaseImpl.java
│       │   ├── SaveSyncConfigUseCaseImpl.java
│       │   └── SyncAssetsUseCaseImpl.java
│       ├── convert/
│       │   ├── GetConvertConfigUseCaseImpl.java
│       │   ├── SaveConvertConfigUseCaseImpl.java
│       │   └── ConvertAssetsUseCaseImpl.java
│       ├── folder/
│       │   ├── GetSubFoldersUseCaseImpl.java
│       │   ├── GetDrivesUseCaseImpl.java
│       │   ├── GetInitialFolderUseCaseImpl.java
│       │   └── GetRecentTargetPathsUseCaseImpl.java
│       ├── recycle/
│       │   ├── GetDeletedAssetsUseCaseImpl.java
│       │   ├── RestoreAssetsUseCaseImpl.java
│       │   └── PurgeAssetsUseCaseImpl.java
│       ├── search/
│       │   ├── GetSearchPresetsUseCaseImpl.java
│       │   ├── CreateSearchPresetUseCaseImpl.java
│       │   └── DeleteSearchPresetUseCaseImpl.java
│       ├── home/
│       │   └── GetHomeStatsUseCaseImpl.java
│       └── user/
│           ├── ListUsersUseCaseImpl.java
│           ├── CreateUserUseCaseImpl.java
│           ├── UpdatePasswordUseCaseImpl.java
│           └── DeleteUserUseCaseImpl.java
│
└── infrastructure/                               ← all framework-specific code
    ├── persistence/
    │   ├── entity/                               ← @Entity JPA classes (moved from domain/entity/)
    │   │   ├── AssetEntity.java
    │   │   ├── AssetExifEntity.java
    │   │   ├── FolderEntity.java
    │   │   ├── AlbumEntity.java
    │   │   ├── UserEntity.java
    │   │   ├── RefreshTokenEntity.java
    │   │   ├── SearchPresetEntity.java
    │   │   ├── SyncAssetsDirectoriesDefinitionEntity.java
    │   │   ├── ConvertAssetsDirectoriesDefinitionEntity.java
    │   │   ├── CatalogRunStateEntity.java
    │   │   └── RecentTargetPathEntity.java
    │   ├── jpa/                                  ← Spring Data JPA interfaces (moved from domain/repository/)
    │   │   ├── JpaAssetRepository.java           ←  extends JpaRepository<AssetEntity, Long>
    │   │   ├── JpaAssetExifRepository.java
    │   │   ├── JpaFolderRepository.java
    │   │   ├── JpaAlbumRepository.java
    │   │   ├── JpaUserRepository.java
    │   │   ├── JpaRefreshTokenRepository.java
    │   │   ├── JpaSearchPresetRepository.java
    │   │   ├── JpaSyncConfigRepository.java
    │   │   ├── JpaConvertConfigRepository.java
    │   │   ├── JpaCatalogStateRepository.java
    │   │   └── JpaRecentTargetPathRepository.java
    │   ├── adapter/                              ← implements domain/port/out repository interfaces
    │   │   ├── AssetRepositoryImpl.java
    │   │   ├── AssetExifRepositoryImpl.java
    │   │   ├── FolderRepositoryImpl.java
    │   │   ├── AlbumRepositoryImpl.java
    │   │   ├── UserRepositoryImpl.java
    │   │   ├── RefreshTokenRepositoryImpl.java
    │   │   ├── SearchPresetRepositoryImpl.java
    │   │   ├── SyncConfigRepositoryImpl.java
    │   │   ├── ConvertConfigRepositoryImpl.java
    │   │   ├── CatalogStateRepositoryImpl.java
    │   │   └── RecentTargetPathRepositoryImpl.java
    │   └── mapper/                               ← entity ↔ domain model conversions
    │       ├── AssetEntityMapper.java
    │       ├── AssetExifEntityMapper.java
    │       ├── FolderEntityMapper.java
    │       ├── AlbumEntityMapper.java
    │       ├── UserEntityMapper.java
    │       └── SearchPresetEntityMapper.java
    ├── web/                                      ← HTTP primary adapters (moved from api/)
    │   ├── controller/
    │   │   ├── AssetController.java
    │   │   ├── AlbumController.java
    │   │   ├── AuthController.java
    │   │   ├── ConvertController.java
    │   │   ├── FolderController.java
    │   │   ├── HomeController.java
    │   │   ├── RecycleBinController.java
    │   │   ├── SearchPresetController.java
    │   │   ├── SyncController.java
    │   │   └── UserAdminController.java
    │   ├── dto/                                  ← HTTP request/response DTOs (moved from api/dto/)
    │   │   ├── AssetDto.java
    │   │   ├── AlbumDto.java
    │   │   ├── AlbumSummaryDto.java
    │   │   ├── FolderDto.java
    │   │   ├── ExifMetadataDto.java
    │   │   ├── MoveAssetsRequest.java
    │   │   ├── RateAssetRequest.java
    │   │   ├── DownloadAssetsRequest.java
    │   │   ├── UploadRequest.java
    │   │   ├── CreateAlbumRequest.java
    │   │   ├── UpdateAlbumRequest.java
    │   │   ├── AlbumAssetIdsRequest.java
    │   │   ├── CreatePresetRequest.java
    │   │   ├── SearchPresetDto.java
    │   │   ├── RecycleBinPurgeRequest.java
    │   │   └── RecycleBinRestoreRequest.java
    │   ├── mapper/                               ← domain model ↔ HTTP DTO conversions
    │   │   ├── AssetDtoMapper.java
    │   │   ├── AlbumDtoMapper.java
    │   │   └── FolderDtoMapper.java
    │   └── exception/                            ← HTTP exceptions (moved from api/exception/)
    │       ├── GlobalExceptionHandler.java
    │       ├── AlbumNotFoundException.java
    │       ├── FolderNotFoundException.java
    │       ├── InvalidRefreshTokenException.java
    │       └── SearchPresetNotFoundException.java
    ├── service/                                  ← secondary service adapters
    │   ├── StorageServiceAdapter.java            ← implements StoragePort
    │   ├── ThumbnailStorageServiceAdapter.java   ← implements ThumbnailPort
    │   ├── AssetHashCalculatorAdapter.java       ← implements HashCalculatorPort
    │   ├── JwtTokenAdapter.java                  ← implements JwtTokenPort
    │   ├── JwtAuthenticationFilter.java
    │   ├── JwtUtil.java
    │   ├── CatalogScheduler.java
    │   ├── RefreshTokenServiceImpl.java
    │   └── UserServiceImpl.java
    └── config/
        ├── AppConfig.java
        ├── DataInitializer.java
        ├── SecurityConfig.java
        ├── TestSecurityConfig.java
        └── UserConfig.java
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| High volume of file renames and import changes across ~100 files | Migrate layer-by-layer (domain models first, then ports, then adapters) with the test suite green after each phase |
| Entity ↔ model mapping adds boilerplate | Use MapStruct `@Mapper(componentModel = "spring")` for all entity ↔ domain model conversions; use `@Named` qualifiers when a mapper exposes multiple methods returning the same type |
| `@Transactional` boundaries shift when logic moves from infrastructure services to application use cases | Audit transaction propagation carefully during phase 4; keep `REQUIRES_NEW` heartbeat transaction in the adapter |
| Existing unit tests mock `JpaRepository` methods that will no longer appear in port interfaces | Rewrite those tests to mock the new port interfaces instead — this is the desired state |
| Spring Security filter chain references `UserRepository` directly (via `UserDetailsService`) | `UserDetailsService` stays in `infrastructure/config/UserConfig.java`; it calls `UserRepositoryImpl`, not the domain port |

## Migration Plan

Migration proceeds in seven phases, each leaving the test suite green:

1. **Phase 1 — Domain models:** Create `domain/model/` POJOs (copy fields, remove JPA annotations). Keep old entities in place.
2. **Phase 2 — Driven ports:** Create `domain/port/out/` interfaces with plain Java types. Create `PaginatedResult<T>` record. Repository interfaces use `Repository` suffix; service ports use `Port` suffix.
3. **Phase 3 — Persistence adapters:** Create `infrastructure/persistence/entity/`, `jpa/`, `adapter/`, `mapper/`. Implement all mappers as MapStruct `@Mapper(componentModel = "spring")` interfaces. Wire repository implementations (`RepositoryImpl` suffix) as `@Service` beans. Delete old `domain/entity/` and `domain/repository/`.
4. **Phase 4 — Driving ports + use cases:** Create `domain/port/in/` interfaces. Create `application/usecase/` implementations that use port/out interfaces. Delete `PhotoManagerFacade` and `PhotoManagerFacadeImpl`.
5. **Phase 5 — Web adapters:** Move `api/` to `infrastructure/web/`. Update controllers to inject use-case interfaces instead of facade. Add HTTP ↔ domain MapStruct mappers.
6. **Phase 6 — Service adapters:** Rename `infrastructure/service/` adapters to match port interfaces (`ServiceAdapter` suffix). Verify all tests pass.
7. **Phase 7 — Verification and documentation:** Full build and test verification; update `CLAUDE.md` and `README.md` to document the hexagonal structure, naming conventions (`Repository`/`RepositoryImpl`, `Port`/`ServiceAdapter`), and the MapStruct-only mapping rule.
