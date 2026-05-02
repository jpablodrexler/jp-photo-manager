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
│              PostgreSQL 18 database                      │
│  host: $POSTGRES_HOST  db: $POSTGRES_DB                  │
└─────────────────────────────────────────────────────────┘
```

### Project structure

```
JPPhotoManagerWeb/
├── backend/            # Java 21 + Spring Boot 3 Maven project
│   ├── Dockerfile      # Multi-stage build (Maven → JRE Alpine)
│   └── .dockerignore
├── frontend/           # Angular 19 npm project
│   ├── Dockerfile      # Multi-stage build (Node → Nginx Alpine)
│   ├── nginx.conf      # Serves SPA + reverse-proxies /api to backend
│   └── .dockerignore
├── docker-compose.yml  # Orchestrates db, backend, and frontend
└── .env.example        # Template for local Docker configuration
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

- **Database:** PostgreSQL 18
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
| `photomanager.thumbnails-directory` | `~/.photomanager/thumbnails` | Thumbnail storage path — overridden by `THUMBNAILS_DIR` env var |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `photomanager` | Database name |
| `POSTGRES_USERNAME` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `CATALOG_DIR` | *(unset — falls back to `~/Pictures`)* | Overrides `initial-directory` and `root-catalog-folders` |
| `THUMBNAILS_DIR` | *(unset — falls back to `~/.photomanager/thumbnails`)* | Overrides `thumbnails-directory` |

### Running the backend

**Prerequisites:** Java 21, Maven 3.9+, PostgreSQL 18 (or Docker)

Start a local PostgreSQL instance if you don't have one:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:18
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

**SSE (Server-Sent Events)** is a web standard where the server pushes a stream of text events to the client over a single long-lived HTTP connection. It is one-way (server → client only), HTTP-based — the client makes a regular `GET` request and the response stays open while the server writes `data: ...` lines as events occur — and browsers handle reconnection automatically if the connection drops.

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

## Running with Docker Compose

The fastest way to run the full stack. No local Java, Maven, Node.js, or PostgreSQL installation required.

### Prerequisites

- Docker 24+
- Docker Compose v2 (`docker compose` — not the legacy `docker-compose`)

### Setup

1. Copy the environment template and fill in your values:
   ```bash
   cd JPPhotoManagerWeb
   cp .env.example .env
   ```

2. Edit `.env` — the only required change is `HOST_IMAGE_DIR`:

   | Variable | Description |
   |---|---|
   | `HOST_IMAGE_DIR` | **Required.** Absolute path on your machine to the directory containing images to catalogue (e.g. `/home/yourname/Pictures`). Mounted read-write so all write features work on your actual files. |
   | `JWT_SECRET` | **Required.** HS256 signing secret. Generate with `openssl rand -base64 32`. |
   | `POSTGRES_DB` | Database name (default: `photomanager`). |
   | `POSTGRES_USERNAME` | Database user (default: `postgres`). |
   | `POSTGRES_PASSWORD` | Database password (default: `postgres`). |

3. Build and start all three services:
   ```bash
   docker compose up --build
   ```

4. Open `http://localhost` in your browser.

### Services

| Service | Container | Exposed port | Description |
|---|---|---|---|
| `db` | `postgres:18` | *(internal only)* | PostgreSQL 18; data persisted in the `pgdata` named volume |
| `backend` | JRE 21 Alpine | *(internal only)* | Spring Boot API; `HOST_IMAGE_DIR` bind-mounted at `/catalog` |
| `frontend` | Nginx Alpine | `80` | Angular SPA; reverse-proxies `/api` to the backend |

### Volume behaviour

| Volume | Type | Description |
|---|---|---|
| `pgdata` | Named Docker volume | PostgreSQL data — survives `docker compose down`, removed by `docker compose down -v` |
| `thumbnails` | Named Docker volume | Generated thumbnail files — survives `docker compose down`, removed by `docker compose down -v` |
| `HOST_IMAGE_DIR` | Bind mount (read-write) | Your photos directory — changes made by the app are reflected on your host filesystem |

### Common commands

```bash
# Start (build images on first run or after code changes)
docker compose up --build

# Start without rebuilding
docker compose up

# Stop (keeps volumes — data preserved)
docker compose down

# Stop and wipe all volumes (full reset — deletes DB and thumbnails)
docker compose down -v

# View logs for a specific service
docker compose logs -f backend
```

### Linux file permission note

If write operations (delete, move, convert) fail with `AccessDeniedException`, the container user's UID doesn't match the owner of `HOST_IMAGE_DIR`. Fix by adding `user` to the `backend` service in `docker-compose.yml`:

```yaml
backend:
  user: "${UID}:${GID}"
```

Then start with:
```bash
UID=$(id -u) GID=$(id -g) docker compose up
```

---

## Running the full application (without Docker)

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

---

## Catalog Process

The catalog process scans all configured root folders (`photomanager.root-catalog-folders`), generates thumbnails, computes SHA-256 hashes, and persists asset metadata to the database.

### Lifecycle

The backend owns the catalog lifecycle entirely. The gallery frontend no longer triggers catalog runs on page load.

**Startup:** `CatalogScheduler` listens for `ApplicationReadyEvent` and immediately submits the first catalog run to a dedicated single-thread `ThreadPoolTaskScheduler`.

**Periodic repetition:** After each run completes, the scheduler waits `photomanager.catalog-cooldown-minutes` (default: 2 minutes) before starting the next run. The delay is measured from the **end** of the previous run (fixed delay, not fixed rate), so runs never overlap.

**Manual trigger:** The `GET /api/assets/catalog` SSE endpoint remains available for manual or troubleshooting use. If a run is already in progress the request is silently skipped and the SSE stream completes immediately.

### Distributed Lock

A single-row `catalog_run_state` table acts as a distributed lock across all JVM instances:

| Column | Type | Description |
|---|---|---|
| `id` | integer (always 1) | Single-row primary key |
| `is_running` | boolean | Whether a run is active |
| `started_at` | timestamptz | When the current run started |
| `last_heartbeat_at` | timestamptz | Last heartbeat from the running instance |
| `instance_id` | varchar | UUID of the JVM that holds the lock |

Before each run the backend executes an atomic `UPDATE … WHERE id=1 AND is_running=false`. Only one instance succeeds; all others skip. The lock is released in a `finally` block using `WHERE instance_id = :thisInstance` to avoid accidentally releasing another instance's lock.

### Heartbeat

To keep long-running catalog runs alive, `last_heartbeat_at` is refreshed after every `photomanager.catalog-batch-size` (default: 1000) assets are saved. The heartbeat update runs in its own transaction (`propagation = REQUIRES_NEW`) so it is immediately visible to all JVM instances, even while the enclosing folder transaction is still open.

### Stale Run Detection

A `@Scheduled` task runs every 60 seconds. It computes `threshold = now - catalog-timeout minutes` (default: 60 minutes) and:

1. If this JVM holds the lock and `last_heartbeat_at < threshold`: interrupts the catalog thread and releases the DB lock.
2. Releases any locks held by other (crashed) instances whose heartbeat is also older than the threshold.

The catalog folder loop checks `Thread.currentThread().isInterrupted()` at the start of each folder iteration and returns early if set, ensuring clean shutdown on interruption.

### Configuration

| Property | Default | Description |
|---|---|---|
| `photomanager.catalog-cooldown-minutes` | `2` | Minutes to wait between catalog runs (fixed delay from end of previous run) |
| `photomanager.catalog-batch-size` | `1000` | Assets saved between heartbeat refreshes |
| `photomanager.catalog-timeout` | `60` | Minutes without a heartbeat before a run is considered stale |

---

## Logging

Application logs are written to two outputs simultaneously:

- **File:** `~/.photomanager/logs/photomanager.log` — structured **JSON** format (one JSON object per line, using `logstash-logback-encoder`). Each entry includes `@timestamp`, `level`, `logger_name`, `thread_name`, `message`, and any MDC fields or exception details.
- **Console:** human-readable plain-text format (`yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message`).

### Log rotation

Logs rotate **daily**. Rotated files are compressed and stored alongside the active log file as `photomanager.log.yyyy-MM-dd.gz`. Files older than **30 days** are deleted automatically.

### Configuration

Logging is configured entirely via `src/main/resources/logback-spring.xml`. The `logging.*` properties in `application.yml` have no effect while `logback-spring.xml` is present — all tuning must happen in that file.

---

## Authentication

The application uses **JWT Bearer token** authentication. All `/api/**` endpoints except `POST /api/auth/login` require a valid `Authorization: Bearer <token>` header.

### Public endpoint

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate; returns `{ "token": "..." }` |

### JWT flow

1. `POST /api/auth/login` with `{ "username": "...", "password": "..." }` → receive JWT
2. Include `Authorization: Bearer <token>` on all subsequent API calls
3. The frontend stores the token in `localStorage` and attaches it automatically via an HTTP interceptor

### Configuration properties

| Property | Default | Description |
|---|---|---|
| `photomanager.jwt-secret` | *(empty — must be set)* | HS256 signing secret (≥ 32 bytes) |
| `photomanager.jwt-expiry-hours` | `24` | Token validity in hours |

### Setup (local development)

1. Copy `src/main/resources/application-local.yml.example` to `src/main/resources/application-local.yml`
2. Generate a secure secret:
   ```bash
   openssl rand -base64 32
   ```
3. Paste the output into `photomanager.jwt-secret` in `application-local.yml`

> **Important:** The application **will not start** if `photomanager.jwt-secret` is blank. `application-local.yml` is git-ignored and must never be committed.

### Setup (Docker Compose)

Set `JWT_SECRET` in `JPPhotoManagerWeb/.env`:
```bash
echo "JWT_SECRET=$(openssl rand -base64 32)" >> JPPhotoManagerWeb/.env
```

### Default admin user

On first startup, if no users exist in the database, the application automatically creates a default administrator:

| Username | Password |
|---|---|
| `admin` | `admin` |

**Change this password immediately** after first login using the **User Administration** page (`/admin/users`).

### User Administration

Navigate to **Users** in the navigation bar (or `/admin/users`) to:
- View all users
- Add new users
- Change a user's password
- Delete users

There is no self-registration; all user management is done by an authenticated administrator.
