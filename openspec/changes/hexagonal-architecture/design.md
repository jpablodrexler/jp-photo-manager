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
- One focused use-case interface per operation group (replace the single `PhotoManagerFacade`).
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

### 2. Decompose `PhotoManagerFacade` into per-domain use-case interfaces

**Decision:** Replace the single `PhotoManagerFacade` (40+ methods) with focused use-case interfaces in `domain/port/in/`, grouped by domain concept.

**Rationale:** A single facade with 40 methods is a God Object. Controllers currently depend on the entire facade even when they only call 2–3 methods. Focused interfaces enable controller-level dependency isolation, make use cases independently testable, and align with the Interface Segregation Principle.

**Use-case interface groups (driving ports):**

| Interface | Methods | Used by |
|---|---|---|
| `GetAssetsUseCase` | `getAssets()`, `getAssetImage()`, `getAssetExif()`, `downloadAssets()` | `AssetController` |
| `MutateAssetsUseCase` | `rateAsset()`, `moveAssets()`, `uploadAsset()`, `deleteAssets()` | `AssetController` |
| `CatalogAssetsUseCase` | `catalogAssetsAsync()` | `AssetController`, `CatalogScheduler` |
| `GetDuplicatedAssetsUseCase` | `getDuplicatedAssets()` | `AssetController` |
| `ManageAlbumsUseCase` | CRUD + asset membership | `AlbumController` |
| `SyncAssetsUseCase` | `syncAssetsAsync()`, config get/set | `SyncController` |
| `ConvertAssetsUseCase` | `convertAssetsAsync()`, config get/set | `ConvertController` |
| `GetFoldersUseCase` | `getSubFolders()`, `getDrives()`, `getInitialFolder()`, `getRecentTargetPaths()` | `FolderController` |
| `RecycleBinUseCase` | `getDeletedAssets()`, `restoreAssets()`, `purgeAssets()` | `RecycleBinController` |
| `ManageSearchPresetsUseCase` | CRUD presets | `SearchPresetController` |
| `GetHomeStatsUseCase` | `getHomeStats()` | `HomeController` |
| `UserAdminUseCase` | `listUsers()`, `createUser()`, `updatePassword()`, `deleteUser()` | `UserAdminController` |

### 3. Separate domain models from JPA entities

**Decision:** Create pure POJO classes in `domain/model/` and move JPA-annotated classes to `infrastructure/persistence/entity/`. Add mappers in `infrastructure/persistence/mapper/`.

**Rationale:** This is the most invasive part of the change but is required for the domain to be framework-free. MapStruct or hand-written mappers convert between the two representations at the adapter boundary.

### 4. Repository ports use plain Java types only

**Decision:** Port interfaces in `domain/port/out/` use `List<T>`, `Optional<T>`, and a custom `PaginatedResult<T>` record instead of `Page<T>` and `Pageable`.

**Rationale:** `Page` and `Pageable` are Spring Data types. The domain must not know about them. `PaginatedResult<T>` is a simple record: `record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}`. The adapter translates between the two.

### 5. `application/usecase/` implementations are Spring beans — via thin wiring only

**Decision:** Use-case implementation classes (`application/usecase/`) are annotated with `@Service` and `@Transactional` — but only those two Spring annotations. All other Spring types (`@Value`, `MultipartFile`, `Page`, etc.) remain out.

**Rationale:** Fully removing Spring DI from use cases would require a separate DI framework or manual factory wiring, which is out of scope. Allowing `@Service` and `@Transactional` is a pragmatic trade-off: the business logic is still readable and testable in isolation (Mockito does not care about `@Service`), while Spring still manages lifecycle and transactions.

**Alternatives considered:**
- *Use `@Configuration` + `@Bean` factory methods*: achieves full annotation-free use cases but requires writing factory methods for every use case class — significant boilerplate.

### 6. `api/` is renamed and relocated to `infrastructure/web/`

**Decision:** All classes currently in `api/` (controllers, DTOs, exceptions) move to `infrastructure/web/controller/`, `infrastructure/web/dto/`, and `infrastructure/web/exception/`. HTTP-to-domain mappers go in `infrastructure/web/mapper/`.

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
│   │   ├── in/                                   ← use-case interfaces (primary / driving ports)
│   │   │   ├── GetAssetsUseCase.java
│   │   │   ├── MutateAssetsUseCase.java
│   │   │   ├── CatalogAssetsUseCase.java
│   │   │   ├── GetDuplicatedAssetsUseCase.java
│   │   │   ├── ManageAlbumsUseCase.java
│   │   │   ├── SyncAssetsUseCase.java
│   │   │   ├── ConvertAssetsUseCase.java
│   │   │   ├── GetFoldersUseCase.java
│   │   │   ├── RecycleBinUseCase.java
│   │   │   ├── ManageSearchPresetsUseCase.java
│   │   │   ├── GetHomeStatsUseCase.java
│   │   │   └── UserAdminUseCase.java
│   │   └── out/                                  ← secondary / driven port interfaces
│   │       ├── AssetRepositoryPort.java
│   │       ├── AssetExifRepositoryPort.java
│   │       ├── FolderRepositoryPort.java
│   │       ├── AlbumRepositoryPort.java
│   │       ├── UserRepositoryPort.java
│   │       ├── RefreshTokenRepositoryPort.java
│   │       ├── SearchPresetRepositoryPort.java
│   │       ├── SyncConfigRepositoryPort.java
│   │       ├── ConvertConfigRepositoryPort.java
│   │       ├── CatalogStateRepositoryPort.java
│   │       ├── RecentTargetPathRepositoryPort.java
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
│   └── usecase/                                  ← use-case implementations
│       ├── GetAssetsUseCaseImpl.java             ← @Service @Transactional only
│       ├── MutateAssetsUseCaseImpl.java
│       ├── CatalogAssetsUseCaseImpl.java
│       ├── GetDuplicatedAssetsUseCaseImpl.java
│       ├── ManageAlbumsUseCaseImpl.java
│       ├── SyncAssetsUseCaseImpl.java
│       ├── ConvertAssetsUseCaseImpl.java
│       ├── GetFoldersUseCaseImpl.java
│       ├── RecycleBinUseCaseImpl.java
│       ├── ManageSearchPresetsUseCaseImpl.java
│       ├── GetHomeStatsUseCaseImpl.java
│       └── UserAdminUseCaseImpl.java
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
    │   ├── adapter/                              ← implements domain/port/out persistence ports
    │   │   ├── AssetRepositoryAdapter.java
    │   │   ├── AssetExifRepositoryAdapter.java
    │   │   ├── FolderRepositoryAdapter.java
    │   │   ├── AlbumRepositoryAdapter.java
    │   │   ├── UserRepositoryAdapter.java
    │   │   ├── RefreshTokenRepositoryAdapter.java
    │   │   ├── SearchPresetRepositoryAdapter.java
    │   │   ├── SyncConfigRepositoryAdapter.java
    │   │   ├── ConvertConfigRepositoryAdapter.java
    │   │   ├── CatalogStateRepositoryAdapter.java
    │   │   └── RecentTargetPathRepositoryAdapter.java
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
    │   ├── AlbumServiceImpl.java                 ← (to be absorbed into ManageAlbumsUseCaseImpl)
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
| Entity ↔ model mapping adds boilerplate | Use MapStruct for structural mappings; hand-write only where business logic is needed in the conversion |
| `@Transactional` boundaries shift when logic moves from infrastructure services to application use cases | Audit transaction propagation carefully during phase 4; keep `REQUIRES_NEW` heartbeat transaction in the adapter |
| Existing unit tests mock `JpaRepository` methods that will no longer appear in port interfaces | Rewrite those tests to mock the new port interfaces instead — this is the desired state |
| Spring Security filter chain references `UserRepository` directly (via `UserDetailsService`) | `UserDetailsService` stays in `infrastructure/config/UserConfig.java`; it calls `UserRepositoryAdapter`, not the domain port |

## Migration Plan

Migration proceeds in six phases, each leaving the test suite green:

1. **Phase 1 — Domain models:** Create `domain/model/` POJOs (copy fields, remove JPA annotations). Keep old entities in place.
2. **Phase 2 — Driven ports:** Create `domain/port/out/` interfaces with plain Java types. Create `PaginatedResult<T>` record.
3. **Phase 3 — Persistence adapters:** Create `infrastructure/persistence/entity/`, `jpa/`, `adapter/`, `mapper/`. Wire adapters as `@Service` beans. Delete old `domain/entity/` and `domain/repository/`.
4. **Phase 4 — Driving ports + use cases:** Create `domain/port/in/` interfaces. Create `application/usecase/` implementations that use port/out interfaces. Delete `PhotoManagerFacade` and `PhotoManagerFacadeImpl`.
5. **Phase 5 — Web adapters:** Move `api/` to `infrastructure/web/`. Update controllers to inject use-case interfaces instead of facade. Add HTTP ↔ domain mappers.
6. **Phase 6 — Service adapters:** Rename `infrastructure/service/` adapters to match port interfaces. Verify all tests pass.
