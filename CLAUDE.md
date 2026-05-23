# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## First-time setup

After cloning, activate the shared git hooks:
```bash
git config core.hooksPath .githooks
```

This installs a pre-commit hook that runs the backend and/or frontend test suite whenever the corresponding files are staged, blocking commits that would break the pipeline.

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

**Startup:** `PhotoManagerApplication.java` bootstraps Spring Boot; all beans are auto-detected via `@Service` / `@RestController`. The async thread pool and CORS policy are configured in `AppConfig`.

**Key domain services** (interfaces in `domain/service/`, implementations in `infrastructure/service/`):
- `CatalogAssetsService` — recursively scans folders, generates thumbnails (200×150 px), computes SHA-256 hashes, persists to DB; runs asynchronously and streams progress via `Consumer<CatalogChangeNotification>`
- `SyncAssetsService` — copies new files between directory pairs, optionally deletes files missing from the source
- `ConvertAssetsService` — converts PNG images to JPEG
- `FindDuplicatedAssetsService` — groups assets by hash, filters out stale entries
- `MoveAssetsService` — copies or moves files on disk and updates the DB record
- `StorageService` — file I/O, thumbnail generation, EXIF rotation (Apache Commons Imaging), SHA-256

**Persistence:** PostgreSQL via Spring Data JPA + Hibernate. Schema managed by Flyway; migrations in `src/main/resources/db/migration/`. Connect using environment variables `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (defaults: `localhost`, `5432`, `photomanager`, `postgres`, `postgres`).

**Local development prerequisite:** PostgreSQL 15+ must be running locally. Quickstart:
```bash
docker run -d --name photomanager-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=photomanager -p 5432:5432 postgres:15
```

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
