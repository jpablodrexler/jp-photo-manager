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

Clean architecture in a single Maven module (`com.jpablodrexler.photo-manager`), mirroring the original desktop app's layer separation:

```
api/                  → REST controllers + request/response DTOs
application/          → PhotoManagerFacade (orchestration) + application DTOs
domain/
  entity/             → JPA entities (Asset, Folder, …)
  enums/              → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum
  repository/         → Spring Data JPA interfaces
  service/            → Domain service interfaces
infrastructure/
  service/            → Service implementations, StorageServiceImpl, ThumbnailStorageService
config/               → AppConfig (CORS, async executor)
```

**Dependency flow:** `api` → `application` → `domain` ← `infrastructure`

**Key domain services** (interfaces in `domain/service/`, implementations in `infrastructure/service/`):
- `CatalogAssetsService` — recursively scans folders, generates thumbnails (200×150 px), computes SHA-256 hashes, persists to DB; runs asynchronously and streams progress via `Consumer<CatalogChangeNotification>`
- `SyncAssetsService` — copies new files between directory pairs, optionally deletes files missing from the source
- `ConvertAssetsService` — converts PNG images to JPEG
- `FindDuplicatedAssetsService` — groups assets by hash, filters out stale entries
- `MoveAssetsService` — copies or moves files on disk and updates the DB record
- `StorageService` — file I/O, thumbnail generation, EXIF rotation (Apache Commons Imaging), SHA-256

**Persistence:** PostgreSQL via Spring Data JPA + Hibernate. Schema managed by **Flyway**; migrations live in `src/main/resources/db/migration/`. Connection is configured via environment variables (see table below).

**Local development prerequisite:** PostgreSQL 15+ must be running. Quickstart:
```bash
docker run -d --name photomanager-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=photomanager -p 5432:5432 postgres:15
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

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/assets` | Paginated assets for a folder (`folderPath`, `page`, `sort`) |
| `GET` | `/api/assets/{id}/thumbnail` | 200×150 JPEG thumbnail |
| `GET` | `/api/assets/{id}/image` | Full-size original image |
| `GET` | `/api/assets/catalog` | SSE stream: catalog progress events |
| `GET` | `/api/assets/duplicates` | Grouped duplicate assets |
| `POST` | `/api/assets/move` | Move/copy assets to a destination folder |
| `DELETE` | `/api/assets` | Remove assets from catalog (optionally delete files) |
| `GET` | `/api/folders` | Catalogued folders (optionally filtered by `parentPath`) |
| `GET` | `/api/folders/drives` | Available filesystem roots |
| `GET` | `/api/folders/initial` | Configured initial folder |
| `GET` | `/api/folders/recent-paths` | Recently used destination paths |
| `GET` | `/api/sync/configuration` | Load sync directory pairs |
| `PUT` | `/api/sync/configuration` | Save sync directory pairs |
| `GET` | `/api/sync/run` | SSE stream: run sync and stream status |
| `GET` | `/api/convert/configuration` | Load convert directory pairs |
| `PUT` | `/api/convert/configuration` | Save convert directory pairs |
| `GET` | `/api/convert/run` | SSE stream: run convert and stream status |

### Testing Conventions

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

Integration tests annotate with `@SpringBootTest`, extend `PostgresIntegrationTest` (which starts a Testcontainers PostgreSQL container via `@ServiceConnection`), and use the `test` Spring profile (`application-test.yml`). Flyway is enabled in tests so the real schema is applied before each test run. Requires Docker to be running.

### Key Conventions

- **Java version:** 21 (use records, sealed classes, pattern matching where appropriate).
- **Lombok:** use `@Data`, `@RequiredArgsConstructor`, `@Slf4j` — avoid writing boilerplate manually.
- **Transactions:** read-only queries use `@Transactional(readOnly = true)`; writes use `@Transactional`.
- **Async:** long-running methods are annotated `@Async` and return `CompletableFuture<T>`; the thread pool is configured in `AppConfig`.
- **Logging:** use the SLF4J logger injected by `@Slf4j`, not `System.out`.
- **New services:** declare the interface in `domain/service/`, implement in `infrastructure/service/`, and let Spring autowire via the interface. No manual bean registration needed — `@Service` is sufficient.
- **New endpoints:** add a `@RestController` in `api/`; keep controllers thin (delegate immediately to `PhotoManagerFacade`).

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

**API communication:**
- Standard HTTP calls go through Angular's `HttpClient` in the `core/services/` classes.
- Long-running operations (catalog, sync, convert) use the browser's native `EventSource` to consume SSE streams from the backend.

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
