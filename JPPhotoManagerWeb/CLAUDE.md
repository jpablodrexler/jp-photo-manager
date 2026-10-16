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

- **`backend/`** — Java 21 + Spring Boot 3.5 REST API
- **`frontend/`** — Angular 22 SPA

## Documentation

This file covers commands, package layout, and coding conventions — enough
to start working in the code. The detailed reference material (REST API
table, full configuration property table, system/database diagrams,
authentication flow, the Kafka-driven catalog pipeline, Docker Compose /
Kubernetes deployment, and a `curl` command per endpoint) lives under
`JPPhotoManagerWeb/docs/` instead of being duplicated inline here — see
`JPPhotoManagerWeb/README.md`'s Documentation table for the full index, or
jump straight to `docs/backend.md`, `docs/security.md`,
`docs/architecture.md`, `docs/database.md` or `docs/catalog-process.md`.
Keeping this split is the `docs-sync` skill's job — see that skill if either
this file or `docs/*.md` drifts from the actual code.

Non-obvious architectural decisions (a rejected alternative, a constraint
discovered the hard way) are recorded as individual, numbered ADRs under
`docs/decisions/` — see `docs/decisions/README.md` for the index. Recording
those is the `decision-record` skill's job, not `docs-sync`'s: `docs-sync`
keeps this file's *current-state* narrative accurate against the code, while
`docs/decisions/` is an append-only history of *why*.

## Kit conventions

Where the [backlog-kit](https://github.com/jpablodrexler/backlog-kit) skills read and write. The backlog of this repository lives in `JPPhotoManagerWeb/docs/backlog` (`.backlog-kit.toml` at the repository root says so), so `JPPhotoManagerWeb/` is the project root for the skills: every `docs/…` path below is `JPPhotoManagerWeb/docs/…`, and `CLAUDE.md` and `README.md` are this folder's. The WPF application and the `docs/` at the repository root are not covered by the skills. This project keeps the files it needs and four extra reference docs: `docs/catalog-process.md`, `docs/curl-reference.md`, `docs/docker-compose.md` and `docs/kubernetes.md`.

| Path | What it holds | Checked against |
| ---- | ------------- | --------------- |
| `docs/backlog/` | the backlog: `features-planned.md`, `features-implemented.md`, `bugs-open.md`, `bugs-fixed.md`, plus one brief per item in `features/` and `bugs/` | — |
| `docs/decisions/` | architecture decision records, numbered and append-only | — |
| `docs/explorations/` | notes of ideas still being explored (created when the first one is written) | — |
| `docs/reports/` | dated reports of the reviewers and the batch runs (gitignored, except the metrics snapshots) | — |
| `docs/architecture.md` | system diagram, the hexagonal backend, the database schema diagram, the frontend component hierarchy, the project structure, and the `Architecture review checks` section (what `architecture-reviewer` checks in this project) | the backend package structure, the Flyway migrations (for the schema diagram), `frontend/src/app/app.routes.ts` (for the component diagram), the top-level directory tree |
| `docs/features.md` | what a user can actually reach today (never the planned backlog) | the routes in `app.routes.ts` and the components under `frontend/src/app/` |
| `docs/product.md` | what the product is for, and the `Product scope review checks` section (the examples `product-scope-roaster` tests) | `docs/features.md` and the backlog tables |
| `docs/backend.md` | technologies, internal architecture, key services, persistence, caching, observability and custom metrics, the REST API, configuration, running and testing, and the backend's `Code review checks` plus the `Spec compliance checks` and the shared part of the `Dependency upgrade checks` sections | every `@RestController` in `infrastructure/web/controller/` (one table row per mapping, the auth column cross-checked against `SecurityConfig` and any `@PreAuthorize`), every `@Value("${…}")` and the `application.yml` defaults, the `@Entity`, Mongo and Redis classes, `AppConfig.cacheManager()`, every `photomanager_*` metric against `grafana/provisioning/dashboards/photomanager.json`, `backend/pom.xml` |
| `docs/frontend.md` | technologies, application structure, gallery modes, real-time progress, running, building and testing, and the frontend's `Code review checks` and `Dependency upgrade checks` sections | `frontend/src/app/app.routes.ts` and the `features/` folder, `frontend/angular.json`, `frontend/package.json` |
| `docs/database.md` | the stores, the Flyway workflow, and the `Database review checks` section (what `database-reviewer` checks in this project) | every Flyway migration in `backend/src/main/resources/db/migration/` in order, and the JPA entities |
| `docs/security.md` | the JWT and refresh-token flow, cookies, configuration, user administration, and the `Security review checks` section (what `security-reviewer` checks in this project) | `SecurityConfig`, `AuthController`, `JwtTokenServiceAdapter`, `RefreshTokenServiceAdapter`, the cookie names and flags, `backend/pom.xml` and `frontend/package.json` |
| `docs/catalog-process.md` | the Spring Batch catalog job, its triggers and the Kafka messages | `infrastructure/batch/**`, `infrastructure/kafka/**`, `config/KafkaTopicConfig.java` |
| `docs/curl-reference.md` | one `curl` example per REST endpoint | the same controllers as the REST API table, with the same endpoint list |
| `docs/docker-compose.md` | running with Docker Compose, including monitoring | `docker-compose.yml`, `.env.example` |
| `docs/kubernetes.md` | running with Kubernetes, and the deploy manifest parity rule | `k8s/*.yaml`, `kustomization.yaml`, `scripts/*.sh`, and `docker-compose.yml` (which `k8s/configmap.yaml` mirrors) |
| `CLAUDE.md` | this file: the lean quick-start for an agent | its commands against `pom.xml` and the `package.json` scripts, its layout against the real package structure |
| `README.md` | a short intro and the documentation index into `docs/` | which `docs/*.md` exist and what each covers |

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

**Check line coverage against the 80% minimum** (opt-in, not part of `mvn test`/`verify`/`package` — see `code-reviewer` skill §19.2):
```bash
cd JPPhotoManagerWeb/backend
mvn test
mvn jacoco:check
```

### Architecture

Hexagonal (Ports and Adapters) architecture in a single Maven module (`com.jpablodrexler.photo-manager`). Boundaries are enforced by package naming and import discipline.

```
domain/
  model/              → Pure POJO domain objects (no framework imports), including
                        AssetFilter and PaginatedResult
  port/
    in/               → Use-case interfaces (one interface, one method each), grouped by
                        subdomain: album, analytics, asset, audit, auth, catalog, convert,
                        folder, home, preference, recycle, search, sync, tag, user
    out/              → Repository and service port interfaces (driven ports)
  enums/              → ImageRotation, SortCriteria, WallpaperStyle, ReasonEnum

application/
  dto/                → Framework-free application DTOs (CatalogProgressMessage,
                        SyncProgressMessage, ConvertProgressMessage, domain events, …)
  usecase/            → One implementation class per use-case interface (@Service @Transactional),
                        grouped the same way as port/in/

infrastructure/
  persistence/
    entity/           → @Entity JPA classes
    jpa/              → Spring Data JPA interfaces (JpaXxxRepository)
    adapter/          → Implements domain/port/out/ repository interfaces (XxxRepositoryImpl)
    mapper/           → MapStruct entity ↔ domain model mappers
    mongo/            → MongoAuditLogRepository (`asset_audit_log` collection)
    redis/            → RedisRefreshTokenStore (refresh-token mirror)
  web/
    controller/       → @RestController classes (HTTP primary adapters) — 15 controllers;
                        see `docs/backend.md` for the full list
    dto/              → HTTP request/response DTOs
    mapper/           → MapStruct domain ↔ HTTP DTO mappers
    filter/           → RateLimitFilter (Bucket4j + Redis),
                        RequestCorrelationFilter (tags every request with an MDC
                        requestId/username and an X-Request-ID response header)
    exception/        → GlobalExceptionHandler and HTTP exceptions
  service/            → Service adapters (StorageServiceAdapter, ThumbnailStorageServiceAdapter,
                        JwtTokenServiceAdapter, RefreshTokenServiceAdapter, JwtAuthenticationFilter,
                        CatalogScheduler, …)
  kafka/              → KafkaProgressListener (SSE bridge), AuditLogKafkaListener,
                        AssetSearchCacheInvalidationListener, upload-pipeline processors —
                        see `kafka-events-conventions` skill and `docs/catalog-process.md`
  batch/              → Spring Batch catalog job config and item readers/writers/listeners —
                        see `docs/catalog-process.md`
  health/             → Custom Actuator health indicators (thumbnail storage, geocoding)
  config/             → AppConfig, SecurityConfig, UserConfig, MongoConfig, KafkaTopicConfig,
                        DataInitializer
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
- `JwtTokenPort` / `JwtTokenServiceAdapter` — JWT generation and validation (delegates to `JwtUtil`)

**Persistence, caching, thumbnails, and real-time progress:** covered in
detail in `docs/backend.md` (Persistence, key services) and
`docs/architecture.md` (system + database diagrams) — including the
Postgres/MongoDB/Redis split, the `asset_exif` store-migration history, the
refresh-token dual-write, and the Redis query-cache/thumbnail-cache
mechanics. Kafka topics and the catalog job's Spring Batch pipeline are
covered in `docs/catalog-process.md`. Local dev requires PostgreSQL, MongoDB,
Redis, and Kafka all running — quickstart commands are in `docs/backend.md`
("Running the backend") and the `e2e-testing` skill §1.

**Configuration:** `src/main/resources/application.yml`. The full,
up-to-date property table (every `photomanager.*` property, every
infrastructure env var, and the actuator/metrics settings) lives in
`docs/backend.md` under "Configuration" — kept there instead of duplicated
here so there's exactly one place to update when a property is added or
its default changes.

### REST API

Full endpoint table (all 15 controllers, auth requirements per endpoint) is
in `docs/backend.md` under "REST API"; runnable `curl` examples for every
endpoint are in `docs/curl-reference.md`. Interactive docs are served at
`/swagger-ui.html` when the backend is running.

### Authentication & Security

JWT stored in an HttpOnly cookie (`SameSite=Strict`, `Path=/`), plus a
separate longer-lived `refreshToken` HttpOnly cookie. Full flow diagram,
the SSE/Spring-Security async-dispatch gotcha, `JWT_SECRET` generation, and
the default admin account are documented in `docs/security.md` — read
that before touching `SecurityConfig`, `JwtAuthenticationFilter`, or
anything under `infrastructure/service/*Token*`.

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

**Run component/unit tests** (Cypress Component Testing, headless — what CI runs):
```bash
cd JPPhotoManagerWeb/frontend
npm test
```

**Check line coverage against the 80% minimum** (see `code-reviewer` skill §19.1):
```bash
cd JPPhotoManagerWeb/frontend
npm run test:coverage
npm run coverage:check
```

**Run the mocked E2E smoke tier** (no backend/infra needed — this is the E2E tier CI runs, on every push/PR):
```bash
cd JPPhotoManagerWeb/frontend
npm run test:e2e:mocked
```

**Run the maintained real-backend E2E suite** (drives a real browser against a real, locally running backend — not in CI; see `docs/frontend.md#running-the-whole-e2e-test-suite` and the `e2e-suite` skill for full prerequisites — Docker infra, `JWT_SECRET`/`POSTGRES_PORT`, and a login rate-limit interaction to know about):
```bash
cd JPPhotoManagerWeb/frontend
npm run test:e2e
```

**Lint:**
```bash
cd JPPhotoManagerWeb/frontend
npm run lint
```

### Architecture

Angular 22 SPA using **standalone components** and **lazy-loaded routes**. No NgModules.

```
src/app/
  app.component.ts/html/scss   → Shell with top navigation bar (shows nav only when logged in)
  app.routes.ts                → Lazy routes; all except /login protected by authGuard
  app.config.ts                → ApplicationConfig (HttpClient with interceptor, Router, Animations,
                                 global ErrorHandler)
  core/
    models/                    → TypeScript interfaces (Asset, Folder, PaginatedData, …)
    services/                  → Angular services wrapping the backend API
    guards/                    → auth.guard.ts — redirects unauthenticated users to /login
    interceptors/              → auth.interceptor.ts — handles 401 → refresh-and-retry or redirect
                                 to /login; also shows a MatSnackBar with the backend's error message,
                                 appending "[Request ID: <id>]" when the response carries an
                                 X-Request-ID header
    error-handler/             → global-error-handler.ts — Angular ErrorHandler override; shows a
                                 MatSnackBar for unhandled component errors
  features/
    auth/login/                → LoginComponent (/login)
    home/                      → HomeComponent (/home) — dashboard with stats
    gallery/                   → Thumbnail grid + full-size viewer
    folder-nav/                → Folder tree (Angular CDK FlatTreeControl)
    sync/                      → Sync configuration and execution
    convert/                   → Convert configuration and execution
    duplicates/                → Duplicate detection and cleanup
    albums/                    → AlbumsComponent + AlbumDetailComponent (/albums, /albums/:id)
    recycle-bin/                → Restore/purge soft-deleted assets
    analytics/                  → Storage/format/rating charts (ngx-charts)
    audio-player/                → Playback controls for streamed audio assets
    admin/users/                → UserAdminComponent (/admin/users) — add/change password/delete users
    profile/sessions/          → SessionsComponent (/profile/sessions) — view/revoke active sessions
  shared/
    components/thumbnail/      → Reusable thumbnail card component
    components/password-strength/ → Live password strength meter (colour bar + rule checklist)
    pipes/file-size.pipe.ts    → Human-readable file size formatting
```

**Routing:** full table with descriptions is in `docs/frontend.md`. Quick
reference:

| Path | Component | Auth | Description |
|---|---|---|---|
| `/login` | `LoginComponent` | No | Login form |
| `/home` | `HomeComponent` | Yes | Dashboard (default redirect) |
| `/gallery` | `GalleryComponent` | Yes | Main photo browser |
| `/sync` | `SyncComponent` | Yes | Configure and run directory sync |
| `/convert` | `ConvertComponent` | Yes | Configure and run PNG→JPEG conversion |
| `/duplicates` | `DuplicatesComponent` | Yes | Find and remove duplicate images |
| `/albums`, `/albums/:id` | `AlbumsComponent`, `AlbumDetailComponent` | Yes | Albums list and detail |
| `/recycle-bin` | `RecycleBinComponent` | Yes | Restore or purge soft-deleted assets |
| `/analytics` | `AnalyticsComponent` | Yes | Storage/format/rating charts |
| `/admin/users` | `UserAdminComponent` | Yes | User administration |
| `/profile/sessions` | `SessionsComponent` | Yes | View/revoke active sessions (device hint, last-used time) |

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
