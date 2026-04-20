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
