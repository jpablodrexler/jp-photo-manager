# Feature 64 — catalog-spring-batch

**As a** maintainer of the project, **I want** cataloging to run as a Spring Batch job, **so that** it processes folders in parallel with restartable, observable runs.

## Acceptance Criteria
- AC1: Given several folders, when a catalog run starts, then they are processed in parallel partitions (grid size configurable, default 4).
- AC2: Given the chunk size setting (default 50), when assets are written, then transaction boundaries follow it.
- AC3: Given the migration, when it runs, then the old run-state table is removed and the job repository takes its role.
- AC4: Given a catalog run, when it progresses, then SSE clients receive progress events from the job.

## Technical notes

Replace the custom `@Async` + `@Transactional` catalog loop with a Spring Batch job; add `spring-boot-starter-batch` to `pom.xml`; the new job lives in a new `infrastructure/batch/` package composed of `CatalogJobConfig`, `CatalogFolderPartitioner`, `CatalogFileItemReader`, `CatalogAssetItemProcessor`, `CatalogAssetItemWriter`, and `CatalogItemWriteListener`; `CatalogAssetsUseCaseImpl` becomes a thin adapter calling `JobLauncher.run(catalogJob, jobParameters)`; the `catalog_run_state` table and related classes are removed — Spring Batch's `JobRepository` replaces their run-state role; chunk size configurable via `photomanager.catalog-chunk-size` (default 50); folders processed in parallel via `PartitionStep` with `photomanager.catalog-partition-grid-size` (default 4); SSE consumers registered in a singleton `SseNotificationRegistry` keyed by job execution ID

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
