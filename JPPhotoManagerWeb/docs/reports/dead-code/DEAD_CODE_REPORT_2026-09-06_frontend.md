# Dead Code Report (frontend) — 2026-09-06

**Commit:** c0f4ef0
**Generated:** 2026-09-06T03:02:03.702Z
**Scope:** frontend (see knip.jsonc for entry points and intentional ignores)

**Total findings:** 26 (2 unused file(s), 0 unused export(s), 13 unused type(s), 5 unused dependenc(y/ies), 6 unlisted dependenc(y/ies))

## Unused files

Never imported/referenced from any entry point.

- src/app/core/models/audio-metadata.model.ts
- src/app/core/models/tag.model.ts

## Unused dependencies

Declared in package.json but never imported — verify still needed before removing (see knip.jsonc for known name-resolved exceptions already excluded).

| Dependency |
| --- |
| @secretlint/secretlint-rule-preset-recommend |
| @stryker-mutator/core |
| license-checker-rseidelsohn |
| secretlint |
| typescript-eslint |

## Unlisted dependencies

Imported/referenced in source or config but missing from package.json (relying on a transitive install, or referencing a package that was never installed at all — check both) — should be declared directly or removed.

| File | Dependency |
| --- | --- |
| angular.json | karma-jasmine |
| angular.json | karma-chrome-launcher |
| angular.json | karma-jasmine-html-reporter |
| angular.json | karma-coverage |
| angular.json | jasmine-core |
| stryker.conf.mjs | @stryker-mutator/command-runner |

## Unused types

| File | Line | Type |
| --- | --- | --- |
| src/app/core/models/album.model.ts | 41 | AlbumAssetIdsRequest |
| src/app/core/models/asset.model.ts | 1 | ImageRotation |
| src/app/core/models/asset.model.ts | 2 | FileType |
| src/app/core/models/asset.model.ts | 27 | ProcessingStatus |
| src/app/core/models/asset.model.ts | 40 | RenameAssetsRequest |
| src/app/core/models/analytics.model.ts | 1 | FolderStorageEntry |
| src/app/core/models/analytics.model.ts | 6 | FormatEntry |
| src/app/core/models/analytics.model.ts | 11 | MonthlyCountEntry |
| src/app/core/models/analytics.model.ts | 16 | RatingEntry |
| src/app/core/models/analytics.model.ts | 33 | ChartSeries |
| src/app/core/models/catalog-notification.model.ts | 3 | CatalogNotificationAsset |
| src/app/core/models/background-sync.model.ts | 3 | SyncQueueEntry |
| src/app/core/models/background-sync.model.ts | 17 | SyncManager |
