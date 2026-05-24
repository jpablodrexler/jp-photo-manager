## Why

The current catalog implementation uses a custom `@Async` loop with a single large transaction per folder. This approach does not support restartability (a crash loses all progress), parallel folder processing, or configurable chunk sizes for memory management. Spring Batch provides these capabilities with a proven, standardized API and replaces the custom loop with a robust job execution framework.

## What Changes

- Replace the custom `@Async` catalog loop with a Spring Batch job in a new `infrastructure/batch/` package
- Six new classes: `CatalogJobConfig`, `CatalogFolderPartitioner`, `CatalogFileItemReader`, `CatalogAssetItemProcessor`, `CatalogAssetItemWriter`, `CatalogItemWriteListener`
- Spring Batch's own `JobRepository` replaces `catalog_run_state` (the custom run-state table is dropped)
- Chunk size and partition grid size are configurable; SSE notifications are forwarded via `SseNotificationRegistry`
- Flyway V27 creates Spring Batch schema tables and drops `catalog_run_state`

## Capabilities

### New Capabilities

_(none — replaces existing catalog infrastructure)_

### Modified Capabilities

- **`catalog-assets`**: The catalog operation now uses Spring Batch. It supports configurable chunk size and parallel folder processing via partitioned steps. The `CatalogAssetsUseCaseImpl` becomes a thin adapter calling `JobLauncher`. The `catalog_run_state` table is replaced by Spring Batch's `BATCH_JOB_EXECUTION` tables.

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `spring-boot-starter-batch`
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V27__spring_batch_schema.sql` — new Flyway migration (creates 9 Spring Batch tables, drops `catalog_run_state`)
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/batch/` — 6 new classes
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CatalogAssetsUseCaseImpl.java` — refactored to call `JobLauncher`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/SseNotificationRegistry.java` — new singleton for SSE consumer registration
- `JPPhotoManagerWeb/backend/src/test/` — Spring Batch integration tests
