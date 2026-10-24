# Feature 32 — folder-watch-service

**As a** photo library owner, **I want** the catalog to update itself when files change in my folders, **so that** I never have to start a catalog run manually after adding files.

## Acceptance Criteria
- AC1: Given the feature is enabled, when the application starts, then it watches every configured root folder; when disabled, it does not.
- AC2: Given a file is created, modified or deleted, when the event settles after a debounce, then an incremental catalog run is triggered.
- AC3: Given a rapid batch of files, when they arrive, then they coalesce into one catalog run.
- AC4: Given a catalog run is already in progress, when a watch event triggers another, then it is skipped.

## Technical notes

Add a Java NIO `WatchService` that monitors all configured root catalog folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events and triggers incremental catalog updates automatically; reuses the existing `CatalogAssetsUseCase` and Spring Batch `JobLauncher` as the execution path; keeps the catalog in sync without any manual user action

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
