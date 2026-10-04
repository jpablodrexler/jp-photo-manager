# Feature 32 — folder-watch-service

Add a Java NIO `WatchService` that monitors all configured root catalog folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE` events and triggers incremental catalog updates automatically; reuses the existing `CatalogAssetsUseCase` and Spring Batch `JobLauncher` as the execution path; keeps the catalog in sync without any manual user action
