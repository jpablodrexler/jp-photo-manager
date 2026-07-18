# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

**Build:**
```bash
dotnet restore JPPhotoManager/JPPhotoManager.sln
dotnet build --no-restore --configuration Release JPPhotoManager/JPPhotoManager.sln
```

**Run all tests:**
```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj
```

**Run a single test class:**
```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj --filter "ClassName=ApplicationTests"
```

**Run a single test method:**
```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj --filter "FullyQualifiedName=JPPhotoManager.Tests.Unit.Application.ApplicationTests.MethodName"
```

**Tests with coverage** (Windows only, generates `TestResults/index.htm`):
```bash
cd JPPhotoManager && ./test-with-coverage.bat
```

## Architecture

This is a WPF desktop application for Windows targeting .NET 8.0, built with clean architecture across five projects:

```
JPPhotoManager.UI           → WPF entry point, MVVM ViewModels, XAML views
JPPhotoManager.Application  → Application.cs facade that orchestrates all services
JPPhotoManager.Domain       → Entities, interfaces, stateless domain services
JPPhotoManager.Infrastructure → EF Core / SQLite repositories and service implementations
JPPhotoManager.Common       → Shared utilities
JPPhotoManager.Tests        → xUnit tests (Unit/ and Integration/ subdirectories)
```

**Dependency flow:** UI → Application → Domain ← Infrastructure (Infrastructure implements Domain interfaces.)

**Startup sequence** (`App.xaml.cs`):
1. Configures log4net from `log4net.config`
2. Builds the DI container (`Microsoft.Extensions.DependencyInjection`)—all services registered as Singletons
3. `App_OnStartup` checks for duplicate instances, runs EF Core migrations, then shows `MainWindow`

**Key domain services** (all in `JPPhotoManager.Domain/Services/`):
- `CatalogAssetsService` — scans folders and indexes images into the database
- `SyncAssetsService` / `ConvertAssetsService` — copy/move/convert images between directories
- `FindDuplicatedAssetsService` — hash-based duplicate detection
- `MoveAssetsService` — handles conflict resolution when copying/moving
- `AssetHashCalculatorService` (Infrastructure) — computes image hashes

**Persistence:** SQLite via EF Core 8. The connection string uses `{ApplicationData}/JPPhotoManager/{FileFormat}/JPPhotoManager.db`. Migrations live in `JPPhotoManager.Infrastructure/Migrations/`.

**Configuration:** `appsettings.json` in the UI project holds initial directory, batch sizes, cooldown periods, and GitHub repo info for release-update checking. User config is loaded via `UserConfigurationService`.

## Testing Conventions

Tests use **xUnit** + **Autofac.Extras.Moq** + **FluentAssertions**. The typical unit test pattern:

```csharp
[Fact]
public void MethodName_Condition_ExpectedResult()
{
    using var mock = AutoMock.GetLoose();
    mock.Mock<ISomeDependency>().Setup(m => m.Method(...)).Returns(...);
    var sut = mock.Container.Resolve<SomeClass>();
    var result = sut.DoSomething();
    result.Should().BeEquivalentTo(expected);
}
```

Integration tests in `Integration/` use real (in-memory or temp-file) SQLite databases and test the full stack from `Application` down through repositories.

Test data files (images, folders) live under `JPPhotoManager.Tests/TestFiles/`.

## Key Conventions

- **Target framework:** `net8.0-windows10.0.17763.0` for UI; `net8.0-windows7.0` for other projects.
- **Nullable references:** Enabled project-wide; respect nullability annotations.
- **Code style enforcement:** `<EnforceCodeStyleInBuild>True</EnforceCodeStyleInBuild>` — the build will fail on style violations.
- **Logging:** log4net throughout; use the existing logger pattern, not `Console.WriteLine`.
- **DI registration:** New services should be registered as Singletons in `App.xaml.cs ConfigureServices()` following the existing pattern.
- **MVVM:** All UI logic belongs in ViewModels; code-behind (`.xaml.cs`) is only for wiring or WPF-specific concerns.

---

# Web Application (JPPhotoManagerWeb)

The web application lives under `JPPhotoManagerWeb/` and is split into two sub-projects:

- **`backend/`** — Java 21 + Spring Boot 3.4 REST API
- **`frontend/`** — Angular 19 SPA

## Web Commands

### Backend

**Build:**
```bash
cd JPPhotoManagerWeb/backend
mvn clean package -DskipTests
```

**Run:**
```bash
cd JPPhotoManagerWeb/backend
mvn spring-boot:run
```
The API starts on `http://localhost:8080`.

**Run unit tests** (no Docker required — what the pre-commit hook runs):
```bash
cd JPPhotoManagerWeb/backend
mvn test
```

**Run unit + integration tests** (requires Docker for Testcontainers):
```bash
cd JPPhotoManagerWeb/backend
mvn verify -Pintegration-tests
```

**Run a single test class:**
```bash
cd JPPhotoManagerWeb/backend
mvn test -Dtest=CatalogAssetsServiceImplTest
```

**Run a single test method:**
```bash
cd JPPhotoManagerWeb/backend
mvn test -Dtest=CatalogAssetsServiceImplTest#methodName
```

### Frontend

**Install dependencies:**
```bash
cd JPPhotoManagerWeb/frontend
npm install
```

**Run dev server** (proxies `/api` to `localhost:8080`):
```bash
cd JPPhotoManagerWeb/frontend
npm start
```
App available at `http://localhost:4200`.

**Build for production:**
```bash
cd JPPhotoManagerWeb/frontend
npm run build:prod
```

**Run tests:**
```bash
cd JPPhotoManagerWeb/frontend
npm test
```

**Open Cypress UI:**
```bash
cd JPPhotoManagerWeb/frontend
npm run cypress:open
```

**Lint:**
```bash
cd JPPhotoManagerWeb/frontend
npm run lint
```

## Web Architecture

Clean architecture mirroring the desktop app's layer separation across two sub-projects:

### Backend

```
api/                  → REST controllers + request/response DTOs
application/          → Use-case orchestration + application DTOs (progress messages, results)
domain/
  entity/             → JPA entities (Asset, Folder, …)
  enums/              → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum
  repository/         → Spring Data JPA interfaces
  service/            → Domain service interfaces
infrastructure/
  service/            → Service implementations, StorageServiceImpl, ThumbnailStorageService,
                        KafkaProgressRegistry (runId → SseEmitter + CompletableFuture map)
  kafka/              → KafkaProgressListener (@KafkaListener for catalog/sync/convert topics)
  batch/              → Spring Batch job config and item writers/listeners for catalog pipeline
  config/             → AppConfig (CORS, async executor), KafkaTopicConfig (topic declarations)
```

**Dependency flow:** `api` → `application` → `domain` ← `infrastructure`

**Startup:** `PhotoManagerApplication.java` bootstraps Spring Boot; all beans are auto-detected via `@Service` / `@RestController`. The async thread pool and CORS policy are configured in `AppConfig`.

**Key use cases** (interfaces in `domain/port/in/`, implementations in `application/usecase/`):
- `CatalogAssetsUseCase.execute(long runId) → CompletableFuture<Void>` — registers a completion future in `KafkaProgressRegistry`, then launches a Spring Batch job. The Batch item writer publishes `CatalogProgressMessage` events to the `job.catalog.progress` Kafka topic; `KafkaProgressListener` forwards each event to the waiting `SseEmitter` and completes the future on `done=true`.
- `SyncAssetsUseCase.execute(long runId)` — `@Async void`; publishes `SyncProgressMessage` events to `job.sync.progress` and a final `done=true` when finished.
- `ConvertAssetsUseCase.execute(long runId)` — `@Async void`; same pattern, publishes to `job.convert.progress`.

**Key infrastructure services:**
- `KafkaProgressRegistry` — thread-safe `ConcurrentHashMap` holding `SseEmitter` and `CompletableFuture<Void>` keyed by `runId`; used to bridge Kafka consumer callbacks back to waiting HTTP connections.
- `KafkaProgressListener` — `@KafkaListener` on `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`; routes messages to the registered emitter and calls `registry.complete(runId)` on `done=true`.
- `StorageService` — file I/O, thumbnail generation, EXIF rotation (Apache Commons Imaging), SHA-256

**Persistence:** PostgreSQL via Spring Data JPA + Hibernate. Schema managed by Flyway; migrations in `src/main/resources/db/migration/`. Connect using environment variables `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (defaults: `localhost`, `5432`, `photomanager`, `postgres`, `postgres`). `asset_exif` lives in PostgreSQL, like every other table — see `raw-exif-jsonb` below for its `raw_exif JSONB` column.

**`AssetExif` persistence (PostgreSQL, `raw_exif JSONB`):** the `asset_exif` table (`V33__recreate_asset_exif.sql`) is keyed directly on `asset_id BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE` — `AssetExifRepositoryImpl` is a Spring Data JPA adapter (`JpaAssetExifRepository`, `AssetExifEntity`) behind the unchanged `AssetExifRepository` domain port. The `raw_exif` column stores the full EXIF tag map via Hibernate 6's native `@JdbcTypeCode(SqlTypes.JSON)`. Deleting an asset relies on the `ON DELETE CASCADE` foreign key — no explicit application-level delete call is needed; soft-deleting an asset does not touch the `assets` row, so EXIF data survives a soft-delete/restore cycle. (This table briefly moved to MongoDB under `mongodb-exif-store`, then was reverted by `revert-exif-postgres-jsonb` once `gps-map-view` — the feature that had motivated a `2dsphere` geospatial index — was cancelled; see `openspec/changes/archive/2026-07-05-mongodb-exif-store/` and `openspec/changes/revert-exif-postgres-jsonb/` for the history. That MongoDB data was not migrated back.)

**Refresh-token persistence (dual-written to PostgreSQL and Redis — Phase 1 of `redis-refresh-tokens`):** the `refresh_tokens` PostgreSQL table remains the sole read source of truth. `RefreshTokenRepositoryImpl` additionally mirrors every write into Redis via `RedisRefreshTokenStore`: a hash at `refresh_token:{token}` (`userId`, `tokenId`, `issuedAt`, TTL = remaining lifetime), a `refresh_tokens:user:{userId}` set, and a `refresh_token:id:{tokenId}` index (`tokenId` generated by a Redis `INCR` sequence, independent of the PostgreSQL `BIGSERIAL` id). A revoked/rotated-away token is deleted from Redis immediately rather than mirrored with a `revoked` flag. Redis mirror failures are caught, logged at `WARN`, and never fail the login/refresh request. A follow-up change (Deploy 2, not yet implemented) will cut reads over to Redis-only and drop the `refresh_tokens` table.

**Audit log persistence (MongoDB, `mongodb-audit-log`):** user-action history is stored append-only in a MongoDB `asset_audit_log` collection, accessed exclusively through the `AuditLogRepository` domain port (`AuditLogRepositoryImpl` adapter, backed by the `MongoAuditLogRepository` Spring Data interface). Each document is an `AuditEvent` (`userId`, `action`, `entityType`, `entityId`, `timestamp`, free-form `metadata`). Two write paths populate it: (1) `AuditLogKafkaListener`, running under its own explicit consumer group `audit-log-writer` (not per-instance like `sse-broadcaster-*`, so each event is processed exactly once across all app instances), subscribed to `asset.cataloged`, `asset.deleted`, and the `done=true` case of `job.catalog.progress`/`job.sync.progress`/`job.convert.progress`; and (2) direct `auditLogRepository.log(...)` calls from the tag add/remove, star-rating, asset-view, and asset-download use cases, for actions with no existing Kafka topic. Every write path wraps the call in a try/catch that logs at `WARN` and never fails the primary request. `MongoIndexInitializer` ensures a compound `{ userId: 1, timestamp: -1 }` index and a TTL index on `timestamp` (365-day expiry) at startup. Read access is exposed via `GET /api/audit-log?userId=&entityId=&from=&to=&page=&size=`, scoped to the authenticated user unless the caller has the `ADMIN` role (non-admins requesting another `userId` get `403`).

**Server-side caching (Redis, `server-side-spring-cache` / `redis-search-tag-cache`):** `AppConfig.cacheManager()` is a `RedisCacheManager` (Lettuce, reusing the `RedisConnectionFactory` already provisioned for the refresh-token store and thumbnail cache) with five named caches, each with its own TTL and a `Jackson2JsonRedisSerializer` built for its exact declared type (no polymorphic `@class` type hints — see `AppConfig.typedConfig`): `home-stats` (10 min), `sub-folders` (5 min), `asset-exif` (30 min), `assets` (5 min), `tags` (5 min). Key prefixes are computed as `<cacheName>:` (single colon), and a `LoggingCacheErrorHandler` catches and logs (`WARN`) any Redis error during a cache get/put/evict so a Redis outage degrades to always querying the source of truth rather than failing the request — this was originally a per-JVM `CaffeineCacheManager` before `redis-search-tag-cache` moved it to Redis so writes on one backend instance correctly invalidate copies served by others. `GetAssetsUseCaseImpl.execute(AssetFilter)` is cached under `assets`, keyed by `{folderId}:{sha256 of the remaining filter fields}` (`AssetSearchCacheKeyGenerator`) so a folder's entries can be pattern-matched and evicted (`assets:{folderId}:*`) without touching other folders'. `ListTagsUseCaseImpl.execute(String query)` is cached under `tags` at the fixed key `all`, but only when `query` is null/blank — prefix-search calls bypass the cache entirely. Invalidation of `assets` happens two ways: (1) `AssetSearchCacheInvalidationListener`, a `@KafkaListener` on `asset.cataloged`/`asset.deleted` under its own persistent consumer group `asset-search-cache-invalidator`, and (2) synchronously from `AddTagToAssetUseCaseImpl`/`RemoveTagFromAssetUseCaseImpl`/`BulkAddTagUseCaseImpl`/`BulkRemoveTagUseCaseImpl` — a tag mutation has no Kafka event of its own but can equally change a folder's tag-filtered search results, so these use cases resolve the affected folder(s) and evict them directly. Both paths delegate the actual cursor-based `SCAN`/`DEL` (never `KEYS`) to the shared `AssetSearchCachePort` (`AssetSearchCacheServiceAdapter`). The `tags` cache is evicted declaratively (`@CacheEvict(cacheNames = "tags", key = "'all'")`) by the same four tag-mutating use cases.

**Asynchronous upload processing (`kafka-async-upload`):** `POST /api/assets/upload` no longer computes the SHA-256 hash, extracts EXIF, or generates the thumbnail on the request thread. `UploadAssetUseCaseImpl` writes the file to disk, persists a minimal placeholder `Asset` row (`processing_status = PENDING`, `hash = null`, `thumbnail_creation_date_time = null`), publishes an `AssetUploadedEvent { assetId, filePath, folderPath, fileName }` to `asset.uploaded` (keyed by `assetId`), and the controller returns `202 Accepted` with `{ assetId, status }` — `CatalogFolderServiceAdapter.createAsset` (used by the folder-scan batch pipeline) is untouched. Three independent, persistent-consumer-group listeners in `infrastructure/kafka/` — `AssetHashProcessor` (`asset-hash-processor`), `AssetExifProcessor` (`asset-exif-processor`), `AssetThumbnailProcessor` (`asset-thumbnail-processor`), all subscribed to `asset.uploaded` — each compute their stage independently, set their own `*_completed_at` timestamp, and commit in their own transaction, mirroring `AuditLogKafkaListener`'s explicit-persistent-group pattern (not the per-instance `sse-broadcaster-*` pattern) so each event is processed exactly once per stage regardless of instance count. After committing, each processor calls `AssetRepository.completeIfAllStagesFinished(assetId)` — a single guarded `UPDATE ... WHERE processing_status <> 'COMPLETED' AND hash_completed_at IS NOT NULL AND exif_completed_at IS NOT NULL AND thumbnail_completed_at IS NOT NULL` — so exactly one of the three processors (whichever finishes last) flips `processing_status` to `COMPLETED` and publishes the final `done=true` message; a race between two processors observing "all three complete" simultaneously is resolved by PostgreSQL's row-level locking on the guarded UPDATE, not application-level coordination. Each listener's container factory (`config/UploadProcessorKafkaConfig`) configures a bounded retry (`DefaultErrorHandler` + `FixedBackOff`, 3 retries/2s); on retry exhaustion the recoverer sets `processing_status = FAILED` and publishes a failed progress message. `POST /api/assets/{id}/reprocess` (admin-only) re-publishes `AssetUploadedEvent` for an asset to retry all three stages; each processor unconditionally overwrites its own field(s), so re-running is idempotent with no "skip if already done" branching needed. Progress is surfaced via `GET /api/assets/upload/{assetId}/observe` (SSE), which registers an emitter in the existing `KafkaProgressRegistry` (keyed by `assetId` instead of `runId` — same map, no structural change) and is fed by a new `KafkaProgressListener.onUploadProgress` method consuming `job.upload.progress`. Because `hash` and `thumbnail_creation_date_time` can now be `null` while an upload is in flight, hash-dependent queries (duplicate detection) filter out `hash IS NULL` rows so in-flight uploads are never treated as duplicates of each other.

**Local development prerequisites:** PostgreSQL 15+, Kafka, and MongoDB must be running locally. Quickstart:
```bash
docker run -d --name photomanager-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=photomanager -p 5432:5432 postgres:15
docker run -d --name photomanager-kafka -p 9092:9092 -p 9094:9094 \
  -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,EXTERNAL://:9094,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,EXTERNAL://localhost:9094 \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:3.9.0
docker run -d --name photomanager-mongo -p 27017:27017 mongo:8
```

**Real-time progress:** long-running operations (catalog, sync, convert) publish progress messages to Kafka topics. `KafkaProgressListener` consumes those messages and forwards them to the appropriate `SseEmitter`, streaming events to the frontend without any direct coupling between the use case and the HTTP layer.

**Kafka topics:**

| Topic | Partitions | Retention | Description |
|---|---|---|---|
| `job.catalog.progress` | 3 | 1 h | Per-asset catalog progress + done signal |
| `job.sync.progress` | 1 | 1 h | Per-pair sync status + done signal with results |
| `job.convert.progress` | 1 | 1 h | Per-pair convert status + done signal with results |
| `asset.cataloged` | 3 | 7 d | Emitted for each newly indexed asset |
| `asset.deleted` | 3 | 7 d | Emitted for each asset removed from catalog |
| `asset.uploaded` | 3 | 7 d | Emitted after a single-file upload's placeholder asset row is persisted; consumed by `asset-hash-processor`, `asset-exif-processor`, `asset-thumbnail-processor` |
| `job.upload.progress` | 1 | 1 h | Per-stage upload processing progress + done/failed signal, keyed by `assetId` |

**Configuration:** `src/main/resources/application.yml`. Key properties:

| Property | Default | Description |
|---|---|---|
| `photomanager.initial-directory` | `~/Pictures` | Starting folder shown in the UI |
| `photomanager.root-catalog-folders` | `~/Pictures` | Semicolon-separated roots to catalog |
| `photomanager.catalog-batch-size` | `1000` | Files processed per catalog pass |
| `photomanager.thumbnails-directory` | `~/.photomanager/thumbnails` | Thumbnail storage path |
| `photomanager.thumbnail-cache.enabled` | `true` | Enables the Redis L2 thumbnail cache (`redis-thumbnail-cache`); `false` reverts to disk-only |
| `photomanager.thumbnail-cache.ttl-seconds` | `86400` | TTL for cached thumbnail bytes at `asset:thumbnail:{assetId}` (requires the Redis deployment's `allkeys-lru` eviction policy) |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `photomanager` | Database name |
| `POSTGRES_USERNAME` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka bootstrap server address |
| `MONGO_URI` | `mongodb://localhost:27017/photomanager` | MongoDB connection URI (`asset_audit_log` collection) |

### Frontend

Angular 19 SPA using **standalone components** and **lazy-loaded routes**. No NgModules.

```
src/app/
  app.component.ts/html/scss   → Shell with top navigation bar
  app.routes.ts                → Lazy routes: /gallery, /sync, /convert, /duplicates
  app.config.ts                → ApplicationConfig (HttpClient, Router, Animations)
  core/
    models/                    → TypeScript interfaces (Asset, Folder, PaginatedData, …)
    services/                  → Angular services wrapping the backend API
  features/
    gallery/                   → Thumbnail grid + full-size viewer
    folder-nav/                → Folder tree (Angular CDK FlatTreeControl)
    sync/                      → Sync configuration and execution
    convert/                   → Convert configuration and execution
    duplicates/                → Duplicate detection and cleanup
  shared/
    components/thumbnail/      → Reusable thumbnail card component
    pipes/file-size.pipe.ts    → Human-readable file size formatting
```

**Routing:**

| Path | Component | Description |
|---|---|---|
| `/gallery` | `GalleryComponent` | Main photo browser (default) |
| `/sync` | `SyncComponent` | Configure and run directory sync |
| `/convert` | `ConvertComponent` | Configure and run PNG→JPEG conversion |
| `/duplicates` | `DuplicatesComponent` | Find and remove duplicate images |

## Web Testing Conventions

### Backend

Tests use **JUnit 5** + **Mockito** + **AssertJ**. Typical unit test pattern:

```java
@ExtendWith(MockitoExtension.class)
class CatalogAssetsServiceImplTest {

    @Mock StorageService storageService;
    @Mock AssetRepository assetRepository;
    @InjectMocks CatalogAssetsServiceImpl sut;

    @Test
    void methodName_condition_expectedResult() {
        when(storageService.listFiles(any())).thenReturn(List.of("/photos/a.jpg"));
        // ...
        assertThat(result).isNotNull();
    }
}
```

Integration tests annotate with `@SpringBootTest` and extend `PostgresIntegrationTest` (which starts a Testcontainers PostgreSQL container via `@ServiceConnection`) using the `test` Spring profile (`application-test.yml`).

### Frontend

Tests use **Cypress Component Testing**. Typical component test pattern:

```typescript
it('methodName_condition_expectedResult', () => {
  const assetService = { getAssets: cy.stub().returns(of({ assets: [], total: 0 })) } as Partial<AssetService>;
  cy.mount(GalleryComponent, {
    providers: [{ provide: AssetService, useValue: assetService }],
  });
  cy.get('.thumbnail-grid').should('exist');
});
```

Tests use `cy.mount()`, Chai assertions, `Partial<Service>` stubs with `cy.stub()`, and `MockEventSource` for SSE flows. Test files are colocated with their source files as `*.cy.ts`.

## Web Key Conventions

### Backend

- **Java version:** 21 (use records, sealed classes, pattern matching where appropriate).
- **Lombok:** use `@Data`, `@RequiredArgsConstructor`, `@Slf4j` — avoid writing boilerplate manually.
- **Logging:** use the SLF4J logger injected by `@Slf4j`, not `System.out`.
- **Transactions:** read-only queries use `@Transactional(readOnly = true)`; writes use `@Transactional`.
- **Async:** long-running methods are annotated `@Async` and return `CompletableFuture<T>`; the thread pool is configured in `AppConfig`.
- **New services:** declare the interface in `domain/service/`, implement in `infrastructure/service/`, register with `@Service`. No manual bean registration needed.
- **New endpoints:** add a `@RestController` in `api/`; keep controllers thin (delegate immediately to `PhotoManagerFacade`).

### Frontend

- **Standalone components only** — never add `NgModule`.
- **Strict TypeScript** — `strict: true` in `tsconfig.json`; no `any` unless unavoidable.
- **Angular control flow** — use `@if`, `@for`, `@switch` (Angular 17+ syntax); avoid `*ngIf` / `*ngFor` directives.
- **Angular Material** — use Material components for all UI elements; import only the specific Material modules each component needs.
- **New features** — add a folder under `features/`, create a standalone component, and register a lazy route in `app.routes.ts`.
- **New API calls** — add a method to the relevant service in `core/services/`; keep HTTP logic out of components.
- **Styles** — global styles in `src/styles.scss`; component-specific styles in the component's `.scss` file.
