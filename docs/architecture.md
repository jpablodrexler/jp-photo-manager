# Architecture

WPF desktop application for Windows targeting .NET 8.0, structured as clean architecture across five projects:

| Project | Responsibility |
|---|---|
| `JPPhotoManager.UI` | WPF entry point, MVVM ViewModels, XAML views |
| `JPPhotoManager.Application` | `Application.cs` facade orchestrating all services |
| `JPPhotoManager.Domain` | Entities, interfaces, stateless domain services |
| `JPPhotoManager.Infrastructure` | EF Core / SQLite repositories and service implementations |
| `JPPhotoManager.Common` | Shared utilities |
| `JPPhotoManager.Tests` | xUnit tests (`Unit/` and `Integration/` subdirectories) |

**Dependency flow:** UI → Application → Domain ← Infrastructure (Infrastructure implements Domain interfaces).

## Startup sequence (`App.xaml.cs`)

1. Configures log4net from `log4net.config`.
2. Builds the DI container (`Microsoft.Extensions.DependencyInjection`) — all services registered as Singletons.
3. `App_OnStartup` checks for duplicate instances, runs EF Core migrations, then shows `MainWindow`.

## Key domain services

All in `JPPhotoManager.Domain/Services/` unless noted:

- `CatalogAssetsService` — scans folders and indexes images into the database.
- `SyncAssetsService` / `ConvertAssetsService` — copy/move/convert images between directories.
- `FindDuplicatedAssetsService` — hash-based duplicate detection.
- `MoveAssetsService` — handles conflict resolution when copying/moving.
- `AssetHashCalculatorService` (Infrastructure) — computes image hashes.

## Persistence

SQLite via EF Core 8. Connection string: `{ApplicationData}/JPPhotoManager/{FileFormat}/JPPhotoManager.db`. Migrations live in `JPPhotoManager.Infrastructure/Migrations/`.

## Configuration

`appsettings.json` (UI project) holds initial directory, batch sizes, cooldown periods, and GitHub repo info for release-update checking. User config is loaded via `UserConfigurationService`.
