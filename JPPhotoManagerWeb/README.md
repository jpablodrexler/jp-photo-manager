# JP Photo Manager — Web Edition

A web rewrite of the JP Photo Manager desktop application. It replaces the original WPF/.NET application with a modern client–server architecture: a **Java 21 + Spring Boot 3** REST API backend and an **Angular 19** single-page application frontend.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Browser (Angular 19)                  │
│  Gallery │ Folder Tree │ Sync │ Convert │ Duplicates     │
│  HTTP + EventSource (SSE)                               │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / SSE
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot 3.4 REST API (port 8080)        │
│  api/         → REST controllers                         │
│  application/ → PhotoManagerFacade (orchestration)       │
│  domain/      → Entities, interfaces, enums              │
│  infrastructure/ → Service implementations, storage      │
└──────────────────────┬──────────────────────────────────┘
                       │ JDBC (Hibernate + PostgreSQL dialect)
┌──────────────────────▼──────────────────────────────────┐
│              PostgreSQL 15+ database                     │
│  host: $POSTGRES_HOST  db: $POSTGRES_DB                  │
└─────────────────────────────────────────────────────────┘
```

### Project structure

```
JPPhotoManagerWeb/
├── backend/    # Java 21 + Spring Boot 3 Maven project
└── frontend/   # Angular 19 npm project
```

---

## Backend

### Technologies

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring Web (REST + SSE) | managed by Spring Boot |
| Spring Data JPA | managed by Spring Boot |
| Spring Validation | managed by Spring Boot |
| Spring Actuator | managed by Spring Boot |
| Hibernate (PostgreSQL dialect) | managed by Spring Boot |
| PostgreSQL JDBC | managed by Spring Boot |
| Flyway + Flyway PostgreSQL extension | managed by Spring Boot |
| Lombok | 1.18.46 |
| MapStruct | 1.6.3 |
| Apache Commons Imaging | 1.0-alpha3 |
| GitHub API client | 1.321 |
| JUnit 5 + Mockito + AssertJ | managed by Spring Boot |
| Testcontainers (PostgreSQL) | managed by Spring Boot |

### Internal architecture

The backend follows a clean architecture with strict layering:

```
api/                     → REST controllers and request/response DTOs
application/             → PhotoManagerFacade — central orchestration facade
domain/
  entity/                → JPA entities (Asset, Folder, …)
  enums/                 → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum
  repository/            → Spring Data JPA repository interfaces
  service/               → Domain service interfaces
infrastructure/
  service/               → Service implementations, StorageServiceImpl, ThumbnailStorageService
config/                  → AppConfig (CORS, async thread pool)
```

**Dependency flow:** `api` → `application` → `domain` ← `infrastructure`

Controllers are thin: they delegate immediately to `PhotoManagerFacade`, which orchestrates all domain services.

### Key services

| Service | Description |
|---|---|
| `CatalogAssetsService` | Recursively scans folders, generates 200×150 thumbnails, computes SHA-256 hashes, persists to DB. Runs asynchronously and streams progress via SSE. |
| `SyncAssetsService` | Copies new files between configured directory pairs; optionally removes files missing from the source. |
| `ConvertAssetsService` | Converts PNG images to JPEG across configured directory pairs. |
| `FindDuplicatedAssetsService` | Groups assets by hash and filters out stale catalog entries. |
| `MoveAssetsService` | Copies or moves files on disk and updates the corresponding DB record. |
| `StorageService` | File I/O, thumbnail generation, EXIF rotation reading (Apache Commons Imaging), SHA-256 hashing. |
| `ThumbnailStorageService` | Stores and retrieves thumbnails as `{assetId}.bin` files under the configured thumbnails directory. |

### Persistence

- **Database:** PostgreSQL 15+
- **Schema migrations:** Flyway, scripts in `src/main/resources/db/migration/`
- **ORM:** Spring Data JPA with the Hibernate PostgreSQL dialect
- **Connection:** configured via environment variables `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (defaults: `localhost`, `5432`, `photomanager`, `postgres`, `postgres`)

### REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/assets` | Paginated asset list for a folder (`folderPath`, `page`, `sort`) |
| `GET` | `/api/assets/{id}/thumbnail` | 200×150 JPEG thumbnail |
| `GET` | `/api/assets/{id}/image` | Full-size original image |
| `GET` | `/api/assets/catalog` | SSE stream — catalog progress events |
| `GET` | `/api/assets/duplicates` | Grouped duplicate assets |
| `POST` | `/api/assets/move` | Move or copy assets to a destination folder |
| `DELETE` | `/api/assets` | Remove assets from catalog (optionally delete files) |
| `GET` | `/api/folders` | Catalogued folders, optionally filtered by `parentPath` |
| `GET` | `/api/folders/drives` | Available filesystem roots |
| `GET` | `/api/folders/initial` | Configured initial folder |
| `GET` | `/api/folders/recent-paths` | Recently used destination paths |
| `GET` | `/api/sync/configuration` | Load sync directory pairs |
| `PUT` | `/api/sync/configuration` | Save sync directory pairs |
| `GET` | `/api/sync/run` | SSE stream — run sync and stream status |
| `GET` | `/api/convert/configuration` | Load convert directory pairs |
| `PUT` | `/api/convert/configuration` | Save convert directory pairs |
| `GET` | `/api/convert/run` | SSE stream — run conversion and stream status |

### Configuration

All settings live in `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP server port |
| `photomanager.initial-directory` | `~/Pictures` | Starting folder shown in the UI |
| `photomanager.root-catalog-folders` | `~/Pictures` | Semicolon-separated folder roots to catalog |
| `photomanager.catalog-batch-size` | `1000` | Files processed per catalog pass |
| `photomanager.catalog-cooldown-minutes` | `2` | Minimum minutes between catalog runs |
| `photomanager.thumbnails-directory` | `~/.photomanager/thumbnails` | Thumbnail storage path |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `photomanager` | Database name |
| `POSTGRES_USERNAME` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `logging.file.name` | `~/.photomanager/logs/photomanager.log` | Log file path |

### Running the backend

**Prerequisites:** Java 21, Maven 3.9+, PostgreSQL 15+ (or Docker)

Start a local PostgreSQL instance if you don't have one:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:15
```

```bash
cd JPPhotoManagerWeb/backend

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

### Running backend tests

```bash
cd JPPhotoManagerWeb/backend

# All tests
mvn test

# Single test class
mvn test -Dtest=CatalogAssetsServiceImplTest

# Single test method
mvn test -Dtest=CatalogAssetsServiceImplTest#methodName
```

Tests use the `test` Spring profile (`src/test/resources/application-test.yml`). Unit tests (`@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`) need no database. Integration tests (`@SpringBootTest`) use Testcontainers to spin up a real PostgreSQL container automatically — Docker must be running.

---

## Frontend

### Technologies

| Technology | Version |
|---|---|
| Angular | 19 |
| Angular Material | 19 |
| Angular CDK | 19 |
| TypeScript | 5.6 |
| RxJS | 7.8 |
| Node.js (build/dev) | 22 |
| Karma + Jasmine | 6.4 / 5.4 |

### Application structure

```
src/app/
  app.component.ts/html/scss   → Shell with top navigation bar
  app.routes.ts                → Lazy routes: /gallery, /sync, /convert, /duplicates
  app.config.ts                → ApplicationConfig (HttpClient, Router, Animations)
  core/
    models/                    → TypeScript interfaces (Asset, Folder, PaginatedData, …)
    services/                  → Angular services wrapping the backend REST API
  features/
    gallery/                   → Thumbnail grid + full-size image viewer
    folder-nav/                → Folder tree (Angular CDK FlatTreeControl)
    sync/                      → Sync configuration and execution
    convert/                   → Convert configuration and execution
    duplicates/                → Duplicate detection and cleanup
  shared/
    components/thumbnail/      → Reusable thumbnail card component
    pipes/file-size.pipe.ts    → Human-readable file size formatting
```

All components are **standalone** (no NgModules). Routes are lazy-loaded:

| Path | Feature | Description |
|---|---|---|
| `/` | — | Redirects to `/gallery` |
| `/gallery` | Gallery | Paginated thumbnail grid and full-size viewer |
| `/sync` | Sync | Configure and run directory sync |
| `/convert` | Convert | Configure and run PNG→JPEG conversion |
| `/duplicates` | Duplicates | Find and remove duplicate images |

### Gallery modes

- **Thumbnails mode** — paginated grid; each card displays a 200×150 thumbnail fetched from `/api/assets/{id}/thumbnail`.
- **Viewer mode** — full-screen; loads the original file from `/api/assets/{id}/image` with zoom controls. Double-click a thumbnail to enter viewer mode; click the grid icon to return.

### Real-time progress

Long-running operations (catalog, sync, convert) use the browser's native `EventSource` API to consume SSE streams from the backend, displaying live progress without polling.

### Running the frontend

**Prerequisites:** Node.js 22, npm

```bash
cd JPPhotoManagerWeb/frontend

# Install dependencies
npm install

# Run development server (proxies /api to localhost:8080)
npm start
```

The app is available at `http://localhost:4200`. The dev server automatically proxies `/api` requests to the backend.

### Building for production

```bash
cd JPPhotoManagerWeb/frontend
npm run build:prod
```

Output goes to `dist/jp-photo-manager-ui/`.

### Running frontend tests

```bash
cd JPPhotoManagerWeb/frontend
npm test

# Headless (CI)
npm test -- --watch=false --browsers=ChromeHeadless
```

---

## Running the full application

1. Start the backend:
   ```bash
   cd JPPhotoManagerWeb/backend
   mvn spring-boot:run
   ```

2. In a separate terminal, start the frontend dev server:
   ```bash
   cd JPPhotoManagerWeb/frontend
   npm install
   npm start
   ```

3. Open `http://localhost:4200` in your browser.

---

## CI/CD

Two GitHub Actions workflows are defined in `.github/workflows/`:

| Workflow | File | Trigger |
|---|---|---|
| Web Test | `web-test.yml` | Every push and pull request |
| Web Release | `web-release.yml` | Tags matching `web-v*` |

Each workflow has separate jobs for the backend (Java 21 + Maven) and frontend (Node 22 + npm). The release workflow additionally creates a GitHub Release with the JAR and a zipped frontend dist as artifacts.
