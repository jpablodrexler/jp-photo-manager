# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this folder.

## Never read these files

`JPPhotoManagerWeb/k8s/catalog-volumes.yaml` and `JPPhotoManagerWeb/k8s/secret.yaml` contain real
secrets and machine-specific paths and must **never** be read, under any circumstances — not via a
direct request, not while reviewing a diff or PR, not while walking git branch history, and not as
part of a broad "review all the code" sweep. Both are gitignored (see `.gitignore`) and have
`.example` counterparts (`catalog-volumes.yaml.example`, `secret.yaml.example`) that are safe to
read instead when documenting or reasoning about their structure.

## Overview

`JPPhotoManagerWeb` is the web rewrite of the JP Photo Manager desktop application. It is split into two sub-projects:

- **`backend/`** — Java 21 + Spring Boot 3.4 REST API
- **`frontend/`** — Angular 19 SPA

---

## Backend

### Commands

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

### Architecture

Hexagonal (Ports and Adapters) architecture in a single Maven module (`com.jpablodrexler.photo-manager`). Boundaries are enforced by package naming and import discipline.

```
domain/
  model/              → Pure POJO domain objects (no framework imports)
  port/
    in/               → Use-case interfaces (one interface, one method each)
      asset/          → GetAssetsUseCase, GetAssetImageUseCase, …
      catalog/        → CatalogAssetsUseCase, GetDuplicatedAssetsUseCase
      album/          → GetAlbumsUseCase, CreateAlbumUseCase, …
      sync/           → GetSyncConfigUseCase, SaveSyncConfigUseCase, SyncAssetsUseCase
      convert/        → GetConvertConfigUseCase, SaveConvertConfigUseCase, ConvertAssetsUseCase
      folder/         → GetSubFoldersUseCase, GetDrivesUseCase, …
      recycle/        → GetDeletedAssetsUseCase, RestoreAssetsUseCase, PurgeAssetsUseCase
      search/         → GetSearchPresetsUseCase, CreateSearchPresetUseCase, DeleteSearchPresetUseCase
      home/           → GetHomeStatsUseCase
      user/           → ListUsersUseCase, CreateUserUseCase, UpdatePasswordUseCase, DeleteUserUseCase
    out/              → Repository and service port interfaces (driven ports)
  enums/              → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum

application/
  dto/                → Framework-free application DTOs (AssetFilter, PaginatedResult, …)
  usecase/            → One implementation class per use-case interface (@Service @Transactional)

infrastructure/
  persistence/
    entity/           → @Entity JPA classes
    jpa/              → Spring Data JPA interfaces (JpaXxxRepository)
    adapter/          → Implements domain/port/out/ repository interfaces (XxxRepositoryImpl)
    mapper/           → MapStruct entity ↔ domain model mappers
  web/
    controller/       → @RestController classes (HTTP primary adapters)
    dto/              → HTTP request/response DTOs
    mapper/           → MapStruct domain ↔ HTTP DTO mappers
    exception/        → GlobalExceptionHandler and HTTP exceptions
  service/            → Service adapters (StorageServiceAdapter, ThumbnailStorageServiceAdapter, …)
  config/             → AppConfig, SecurityConfig, UserConfig, DataInitializer
```

**Dependency flow:** `infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service`

**Key use cases** (interfaces in `domain/port/in/`, implementations in `application/usecase/`):
- `CatalogAssetsUseCase` — recursively scans folders, generates thumbnails (200×150 px), computes SHA-256 hashes, persists to DB; runs asynchronously and streams progress via `Consumer<CatalogChangeNotification>`
- `SyncAssetsUseCase` — copies new files between directory pairs, optionally deletes files missing from the source
- `ConvertAssetsUseCase` — converts PNG images to JPEG
- `GetDuplicatedAssetsUseCase` — groups assets by hash, filters out stale entries
- `MoveAssetsUseCase` — copies or moves files on disk and updates the DB record

**Key service ports** (interfaces in `domain/port/out/`, adapters in `infrastructure/service/`):
- `StoragePort` / `StorageServiceAdapter` — file I/O, thumbnail generation, EXIF rotation (Apache Commons Imaging), SHA-256
- `ThumbnailPort` / `ThumbnailStorageServiceAdapter` — thumbnail read/write/delete on disk, fronted by a Redis L2 cache (`redis-thumbnail-cache`)
- `JwtTokenPort` / `JwtTokenAdapter` — JWT generation and validation (delegates to `JwtUtil`)

**Persistence:** PostgreSQL via Spring Data JPA + Hibernate. Schema managed by **Flyway**; migrations live in `src/main/resources/db/migration/`. Connection is configured via environment variables (see table below). `asset_exif` lives in PostgreSQL like every other table (`AssetExifRepositoryImpl` / `JpaAssetExifRepository`, keyed on `asset_id BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE`, with a `raw_exif JSONB` column for the full EXIF tag map). This table briefly moved to MongoDB under `mongodb-exif-store` (#72) and was reverted back to PostgreSQL by `revert-exif-postgres-jsonb` (#84) once `gps-map-view` — the feature that had motivated a `2dsphere` geospatial index on `asset_exif` — was cancelled; see `openspec/changes/archive/2026-07-05-mongodb-exif-store/` and `openspec/changes/revert-exif-postgres-jsonb/` for the history. The MongoDB `asset_exif` data from that period was not migrated back — assets catalogued during that window get their EXIF data repopulated the next time their folder is re-catalogued. Refresh tokens (`refresh_tokens` table) are dual-written to PostgreSQL and Redis: `RefreshTokenRepositoryImpl` mirrors every `save()` into `RedisRefreshTokenStore` (`refresh_token:{token}` hash, `refresh_tokens:user:{userId}` set, `refresh_token:id:{tokenId}` index) alongside the existing JPA write. This is Phase 1 of the `redis-refresh-tokens` migration — PostgreSQL remains authoritative for reads (`findByToken`/`deleteByUserId`/`deleteById`); a follow-up change will cut reads over to Redis-only and drop the PostgreSQL table. User-action history is stored append-only in a MongoDB `asset_audit_log` collection (via `AuditLogRepositoryImpl` / `MongoAuditLogRepository`), populated by `AuditLogKafkaListener` (a dedicated `audit-log-writer` Kafka consumer group) for already-published `asset.cataloged`/`asset.deleted`/`job.*.progress` events, and by direct `AuditLogRepository.log(...)` calls from the tag, rating, view, and download use cases for actions with no existing topic; a compound `userId`/`timestamp` index and a 365-day TTL index are ensured at startup by `MongoIndexInitializer`.

**Server-side caching (Redis, `server-side-spring-cache` / `redis-search-tag-cache`):** `AppConfig.cacheManager()` is a `RedisCacheManager` (Lettuce, sharing the `RedisConnectionFactory` used by the refresh-token store and thumbnail cache) — moved off a per-JVM `CaffeineCacheManager` so a write on one backend instance correctly invalidates the copy served by another. Five named caches, each with its own TTL and a `Jackson2JsonRedisSerializer` built for its exact declared type (deliberately not one shared `GenericJackson2JsonRedisSerializer` with polymorphic `@class` hints — see `AppConfig.typedConfig`'s Javadoc for why that broke the `tags` cache during development): `home-stats` (10 min), `sub-folders` (5 min), `asset-exif` (30 min), `assets` (5 min), `tags` (5 min). Keys are prefixed `<cacheName>:` (single colon) so `assets` entries can be pattern-matched per folder. A `LoggingCacheErrorHandler` catches and logs (`WARN`) any Redis error during a cache get/put/evict, so a Redis outage degrades to always querying the source of truth rather than failing the request. `GetAssetsUseCaseImpl.execute(AssetFilter)` is cached under `assets`, keyed `{folderId}:{sha256 of the remaining filter fields}` via `AssetSearchCacheKeyGenerator`. `ListTagsUseCaseImpl.execute(String query)` is cached under `tags` at the fixed key `all` only when `query` is null/blank. The `assets` cache is invalidated per folder two ways: an `@KafkaListener` (`AssetSearchCacheInvalidationListener`, consumer group `asset-search-cache-invalidator`) on `asset.cataloged`/`asset.deleted`, and synchronously from `AddTagToAssetUseCaseImpl`/`RemoveTagFromAssetUseCaseImpl`/`BulkAddTagUseCaseImpl`/`BulkRemoveTagUseCaseImpl` (a tag mutation has no Kafka event of its own but can equally change a folder's tag-filtered results). Both paths share the same cursor-based `SCAN`/`DEL` (never `KEYS`) eviction logic via `AssetSearchCachePort`/`AssetSearchCacheServiceAdapter`; the same four tag-mutating use cases also declaratively evict `tags:all`.

**Local development prerequisite:** PostgreSQL 18+ and MongoDB must be running. Quickstart:
```bash
docker run -d --name photomanager-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=photomanager -p 5432:5432 postgres:18
docker run -d --name photomanager-mongo -p 27017:27017 mongo:8
```

**Thumbnails:** stored as `{assetId}.bin` files under `~/.photomanager/thumbnails/` managed by `ThumbnailStorageServiceAdapter`. A Redis L2 cache (`redis-thumbnail-cache`) sits in front of the disk store: `loadThumbnail` checks `asset:thumbnail:{assetId}` before reading disk, `saveThumbnail`/`loadThumbnail` (on a disk-read repopulate) write it with a 24-hour TTL, and `deleteThumbnail` evicts it. Requires the Redis deployment shared with `redis-distributed-rate-limiting` (#78) and `redis-refresh-tokens` (#79) to run with the `allkeys-lru` eviction policy so cache growth stays bounded. Every Redis call fails open (caught, logged at `WARN`, falls back to disk) and the whole tier can be disabled via `photomanager.thumbnail-cache.enabled=false`.

**Real-time progress:** long-running operations (catalog, sync, convert) use Spring's `SseEmitter` to stream status events to the frontend.

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
| `MONGO_URI` | `mongodb://localhost:27017/photomanager` | MongoDB connection URI (`asset_audit_log` collection) |

### REST API

| Method | Path | Auth required | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | No | Authenticate; sets `jwt` HttpOnly cookie |
| `POST` | `/api/auth/logout` | No | Clears `jwt` cookie |
| `GET` | `/api/admin/users` | Yes | List all users |
| `POST` | `/api/admin/users` | Yes | Create a user |
| `PATCH` | `/api/admin/users/{id}/password` | Yes | Change a user's password |
| `DELETE` | `/api/admin/users/{id}` | Yes | Delete a user |
| `GET` | `/api/assets` | Yes | Paginated assets for a folder (`folderPath`, `page`, `sort`) |
| `GET` | `/api/assets/{id}/thumbnail` | Yes | 200×150 JPEG thumbnail |
| `GET` | `/api/assets/{id}/image` | Yes | Full-size original image |
| `GET` | `/api/assets/catalog` | Yes | SSE stream: catalog progress events |
| `GET` | `/api/assets/duplicates` | Yes | Grouped duplicate assets |
| `POST` | `/api/assets/move` | Yes | Move/copy assets to a destination folder |
| `DELETE` | `/api/assets` | Yes | Remove assets from catalog (optionally delete files) |
| `GET` | `/api/audit-log` | Yes | Paginated audit trail (`userId`, `entityId`, `from`, `to`, `page`, `size`); non-admins are scoped to their own `userId` |
| `GET` | `/api/folders` | Yes | Catalogued folders (optionally filtered by `parentPath`) |
| `GET` | `/api/folders/drives` | Yes | Available filesystem roots |
| `GET` | `/api/folders/initial` | Yes | Configured initial folder |
| `GET` | `/api/folders/recent-paths` | Yes | Recently used destination paths |
| `GET` | `/api/sync/configuration` | Yes | Load sync directory pairs |
| `PUT` | `/api/sync/configuration` | Yes | Save sync directory pairs |
| `GET` | `/api/sync/run` | Yes | SSE stream: run sync and stream status |
| `GET` | `/api/convert/configuration` | Yes | Load convert directory pairs |
| `PUT` | `/api/convert/configuration` | Yes | Save convert directory pairs |
| `GET` | `/api/convert/run` | Yes | SSE stream: run convert and stream status |

### Authentication & Security

The app uses **JWT stored in an HttpOnly cookie** (`SameSite=Strict`, `Path=/`).

- **Why HttpOnly cookie:** The browser sends cookies automatically with all same-origin requests — including `<img src="...">` and the native `EventSource` API — which do not support custom `Authorization` headers. Storing the token in `localStorage` and injecting it as a header would break image loading and SSE.
- **Login flow:** `POST /api/auth/login` validates credentials and sets the `jwt` cookie. The response body contains `{username, expiresAt}` only — the token is never returned to JavaScript.
- **Logout flow:** `POST /api/auth/logout` returns a `Set-Cookie: jwt=; Max-Age=0` header to clear the cookie. The Angular `AuthService` also removes the session metadata from `localStorage`.
- **SSE + Spring Security:** Tomcat creates a new async dispatch thread for `SseEmitter` writes. Spring Security's filter chain re-runs on that thread where `SecurityContextHolder` is empty. Fix: `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` must appear as the first rule in `SecurityFilterChain`.
- **Default admin user:** `DataInitializer` seeds `admin`/`admin` on first startup if no users exist. Change this password immediately after first login.
- **Security configuration classes:**
  - `config/SecurityConfig.java` — `SecurityFilterChain` bean; `@Profile("!test")`
  - `config/UserConfig.java` — `UserDetailsService` + `BCryptPasswordEncoder` beans; kept separate from `SecurityConfig` to avoid circular dependency
  - `infrastructure/service/JwtAuthenticationFilter.java` — reads `jwt` cookie on every request
  - `infrastructure/service/JwtUtil.java` — HMAC-SHA256 token generation/validation

### Testing Conventions

Tests use **JUnit 5** + **Mockito** + **AssertJ**. Typical unit test pattern:

```java
@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock StoragePort storagePort;
    @Mock AssetRepository assetRepository;
    @InjectMocks CatalogAssetsUseCaseImpl sut;

    @Test
    void methodName_condition_expectedResult() {
        when(storagePort.listFiles(any())).thenReturn(List.of("/photos/a.jpg"));
        // ...
        assertThat(result).isNotNull();
    }
}
```

Integration tests annotate with `@SpringBootTest`, extend `PostgresIntegrationTest` (which starts a Testcontainers PostgreSQL container via `@ServiceConnection`), and use the `test` Spring profile (`application-test.yml`). Flyway is enabled in tests so the real schema is applied before each test run. Requires Docker to be running.

### Key Conventions

- **Java version:** 21 (use records, sealed classes, pattern matching where appropriate).
- **Lombok:** use `@Data`, `@RequiredArgsConstructor`, `@Slf4j` — avoid writing boilerplate manually.
- **Transactions:** read-only queries use `@Transactional(readOnly = true)`; writes use `@Transactional`.
- **Async:** long-running methods are annotated `@Async` and return `CompletableFuture<T>`; the thread pool is configured in `AppConfig`.
- **Logging:** use the SLF4J logger injected by `@Slf4j`, not `System.out`.
- **New use cases:** declare a single-method interface in `domain/port/in/<subpackage>/`, implement in `application/usecase/<subpackage>/` with `@Service @Transactional`; inject only `domain/port/out/` interfaces — no Spring MVC, Spring Data, or HTTP types permitted in use cases.
- **New endpoints:** add a `@RestController` in `infrastructure/web/controller/`; keep controllers thin — inject use-case interfaces and delegate immediately; use MapStruct mappers in `infrastructure/web/mapper/` for HTTP ↔ domain conversion.
- **Repository naming:** port interfaces in `domain/port/out/` use the `Repository` suffix (e.g. `AssetRepository`); their persistence adapters in `infrastructure/persistence/adapter/` use `RepositoryImpl` suffix (e.g. `AssetRepositoryImpl`).
- **Service port naming:** service port interfaces in `domain/port/out/` use the `Port` suffix (e.g. `StoragePort`); their service adapters in `infrastructure/service/` use `ServiceAdapter` suffix (e.g. `StorageServiceAdapter`).
- **Mappers:** all entity ↔ domain model and HTTP DTO ↔ domain model conversions are implemented as MapStruct `@Mapper(componentModel = "spring")` interfaces in `infrastructure/persistence/mapper/` and `infrastructure/web/mapper/` respectively; hand-writing mappers is not permitted; use `@Named` qualifiers when a mapper exposes multiple methods returning the same type (e.g. `toEntityRef` for FK-only references vs `toEntity` for full mapping).

---

## Frontend

### Commands

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

**Lint:**
```bash
cd JPPhotoManagerWeb/frontend
npm run lint
```

### Architecture

Angular 19 SPA using **standalone components** and **lazy-loaded routes**. No NgModules.

```
src/app/
  app.component.ts/html/scss   → Shell with top navigation bar (shows nav only when logged in)
  app.routes.ts                → Lazy routes; all except /login protected by authGuard
  app.config.ts                → ApplicationConfig (HttpClient with interceptor, Router, Animations)
  core/
    models/                    → TypeScript interfaces (Asset, Folder, PaginatedData, …)
    services/                  → Angular services wrapping the backend API
    guards/                    → auth.guard.ts — redirects unauthenticated users to /login
    interceptors/              → auth.interceptor.ts — handles 401 → redirect to /login
  features/
    auth/login/                → LoginComponent (/login)
    home/                      → HomeComponent (/home) — dashboard with stats
    gallery/                   → Thumbnail grid + full-size viewer
    folder-nav/                → Folder tree (Angular CDK FlatTreeControl)
    sync/                      → Sync configuration and execution
    convert/                   → Convert configuration and execution
    duplicates/                → Duplicate detection and cleanup
    admin/users/               → UserAdminComponent (/admin/users) — add/change password/delete users
  shared/
    components/thumbnail/      → Reusable thumbnail card component
    pipes/file-size.pipe.ts    → Human-readable file size formatting
```

**Routing:**

| Path | Component | Auth | Description |
|---|---|---|---|
| `/login` | `LoginComponent` | No | Login form |
| `/home` | `HomeComponent` | Yes | Dashboard (default redirect) |
| `/gallery` | `GalleryComponent` | Yes | Main photo browser |
| `/sync` | `SyncComponent` | Yes | Configure and run directory sync |
| `/convert` | `ConvertComponent` | Yes | Configure and run PNG→JPEG conversion |
| `/duplicates` | `DuplicatesComponent` | Yes | Find and remove duplicate images |
| `/admin/users` | `UserAdminComponent` | Yes | User administration |

**API communication:**
- Standard HTTP calls go through Angular's `HttpClient` in the `core/services/` classes.
- Long-running operations (catalog, sync, convert) use the browser's native `EventSource` to consume SSE streams from the backend.
- Authentication uses HttpOnly cookies — no `Authorization` header is needed; the browser attaches the cookie to all same-origin requests automatically, including `<img>` and `EventSource`.

**Gallery modes:**
- **Thumbnails mode** — paginated grid; each card shows the 200×150 thumbnail (`thumbnailUrl`).
- **Viewer mode** — full-screen; loads the original file (`imageUrl`) with zoom controls. Double-click a thumbnail to enter; press the grid icon to return.

### Key Conventions

- **Standalone components only** — never add `NgModule`.
- **Strict TypeScript** — `strict: true` in `tsconfig.json`; no `any` unless unavoidable.
- **Angular control flow** — use `@if`, `@for`, `@switch` (Angular 17+ syntax); avoid `*ngIf` / `*ngFor` directives.
- **Angular Material** — use Material components for all UI elements; import only the specific Material modules each component needs.
- **New features** — add a folder under `features/`, create a standalone component, and register a lazy route in `app.routes.ts`.
- **New API calls** — add a method to the relevant service in `core/services/`; keep HTTP logic out of components.
- **Styles** — global styles in `src/styles.scss`; component-specific styles in the component's `.scss` file using BEM-like naming.
