# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this folder.

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

**Run all tests:**
```bash
cd JPPhotoManagerWeb/backend
mvn test
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

**Hexagonal Architecture** (Ports & Adapters) in a single Maven module (`com.jpablodrexler.photo-manager`). The domain is completely free of framework dependencies; all Spring, JPA, and file-I/O concerns live exclusively in the `infrastructure/` package tree.

```
domain/
  model/              → Plain POJO domain objects (Asset, Folder, Album, User…)
  port/
    in/               → Use-case interfaces (driving ports) — one interface, one method each
      asset/          →   GetAssetsUseCase, GetAssetImageUseCase, GetAssetExifUseCase,
                          DownloadAssetsUseCase, RateAssetUseCase, MoveAssetsUseCase,
                          UploadAssetUseCase, DeleteAssetsUseCase
      catalog/        →   CatalogAssetsUseCase, GetDuplicatedAssetsUseCase
      album/          →   GetAlbumsUseCase, CreateAlbumUseCase, GetAlbumUseCase,
                          UpdateAlbumUseCase, DeleteAlbumUseCase,
                          AddAssetsToAlbumUseCase, RemoveAssetsFromAlbumUseCase
      sync/           →   GetSyncConfigUseCase, SaveSyncConfigUseCase, SyncAssetsUseCase
      convert/        →   GetConvertConfigUseCase, SaveConvertConfigUseCase, ConvertAssetsUseCase
      folder/         →   GetSubFoldersUseCase, GetDrivesUseCase,
                          GetInitialFolderUseCase, GetRecentTargetPathsUseCase
      recycle/        →   GetDeletedAssetsUseCase, RestoreAssetsUseCase, PurgeAssetsUseCase
      search/         →   GetSearchPresetsUseCase, CreateSearchPresetUseCase, DeleteSearchPresetUseCase
      home/           →   GetHomeStatsUseCase
      user/           →   ListUsersUseCase, CreateUserUseCase, UpdatePasswordUseCase, DeleteUserUseCase
    out/              → Secondary port interfaces (driven ports) — plain Java only
                        AssetRepositoryPort, FolderRepositoryPort, AlbumRepositoryPort,
                        StoragePort, ThumbnailPort, HashCalculatorPort, JwtTokenPort…
  service/            → Stateless pure-domain logic (FindDuplicatedAssetsService)
  enums/              → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum

application/
  dto/                → Framework-free application DTOs (AssetFilter, PaginatedResult,
                        CatalogChangeNotification, SyncAssetsResult…)
  usecase/            → One implementation class per interface (@Service @Transactional only)
    asset/   catalog/   album/   sync/   convert/
    folder/  recycle/   search/  home/   user/   ← mirrors port/in subpackage layout

infrastructure/
  persistence/
    entity/           → @Entity JPA classes (AssetEntity, FolderEntity…)
    jpa/              → Spring Data JPA interfaces (JpaAssetRepository…)
    adapter/          → Implements domain/port/out persistence ports
    mapper/           → Entity ↔ domain model conversions
  web/
    controller/       → @RestController primary adapters (AssetController…)
    dto/              → HTTP request/response DTOs
    mapper/           → Domain model ↔ HTTP DTO conversions
    exception/        → GlobalExceptionHandler, domain exceptions
  service/            → Secondary service adapters (StorageServiceAdapter,
                        ThumbnailStorageServiceAdapter, JwtTokenAdapter,
                        CatalogScheduler, JwtAuthenticationFilter…)
  config/             → AppConfig, SecurityConfig, DataInitializer, UserConfig
```

**Dependency flow:** Primary Adapters → `port/in` → Use Cases → `port/out` ← Secondary Adapters

**Key rules when adding code:**
- `domain/` must have zero `jakarta.*` or `org.springframework.*` imports.
- `domain/port/out/` interfaces use plain Java types — never `JpaRepository`, `Page`, or `Pageable`.
- `application/usecase/` implementations may only carry `@Service` and `@Transactional` — no other Spring annotations.
- All `@Entity`, `@RestController`, and Spring Data repository interfaces belong exclusively in `infrastructure/`.

**Key use cases** (interfaces in `domain/port/in/`, implementations in `application/usecase/`):
- `CatalogAssetsUseCase` — recursively scans folders, generates thumbnails (200×150 px), computes SHA-256 hashes, persists to DB; runs asynchronously and streams progress via `Consumer<CatalogChangeNotification>`
- `SyncAssetsUseCase` — copies new files between directory pairs, optionally deletes files missing from the source
- `ConvertAssetsUseCase` — converts PNG images to JPEG
- `GetDuplicatedAssetsUseCase` — groups assets by hash, filters out stale entries
- `MutateAssetsUseCase` — move, copy, rate, upload, and delete assets
- `StoragePort` / `StorageServiceAdapter` — file I/O, thumbnail generation, EXIF rotation (Apache Commons Imaging), SHA-256

**Persistence:** PostgreSQL via Spring Data JPA + Hibernate. Schema managed by **Flyway**; migrations live in `src/main/resources/db/migration/`. Connection is configured via environment variables (see table below).

**Local development prerequisite:** PostgreSQL 18+ must be running. Quickstart:
```bash
docker run -d --name photomanager-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=photomanager -p 5432:5432 postgres:18
```

**Thumbnails:** stored as `{assetId}.bin` files under `~/.photomanager/thumbnails/` managed by `ThumbnailStorageService`.

**Real-time progress:** long-running operations (catalog, sync, convert) use Spring's `SseEmitter` to stream status events to the frontend.

**Configuration:** `src/main/resources/application.yml`. Key properties:

| Property | Default | Description |
|---|---|---|
| `photomanager.initial-directory` | `~/Pictures` | Starting folder shown in the UI |
| `photomanager.root-catalog-folders` | `~/Pictures` | Semicolon-separated roots to catalog |
| `photomanager.catalog-batch-size` | `1000` | Files processed per catalog pass |
| `photomanager.thumbnails-directory` | `~/.photomanager/thumbnails` | Thumbnail storage path |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `photomanager` | Database name |
| `POSTGRES_USERNAME` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |

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

Tests use **JUnit 5** + **Mockito** + **AssertJ**. Mock the domain port interfaces — never Spring Data repository interfaces or concrete implementations. Typical unit test pattern:

```java
@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock StoragePort storagePort;
    @Mock AssetRepositoryPort assetRepository;
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
- **New use case:** declare a single-method interface in `domain/port/in/<subpackage>/`; create one implementation class in the mirrored `application/usecase/<subpackage>/` path (annotate `@Service @Transactional` only; inject only `domain/port/out/` interfaces); inject the interface into the relevant controller in `infrastructure/web/controller/`.
- **New secondary service:** declare a driven port interface in `domain/port/out/`; implement it as an adapter in `infrastructure/service/` or `infrastructure/persistence/adapter/` (annotate `@Service`; all framework imports stay here).
- **New endpoints:** add a `@RestController` in `infrastructure/web/controller/`; keep controllers thin (translate HTTP ↔ domain types via `infrastructure/web/mapper/`, then delegate to use-case interfaces).

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
