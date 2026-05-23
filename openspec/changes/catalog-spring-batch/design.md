## Context

Spring Batch's `PartitionedStep` allows one master step (`CatalogFolderPartitioner`) to partition work into sub-steps, each processed by a `TaskExecutor` thread pool. Each partition handles one folder. `CatalogFileItemReader` lists new files in the partition's folder. `CatalogAssetItemProcessor` applies hash, thumbnail, and EXIF extraction. `CatalogAssetItemWriter` persists and sends notifications. `SseNotificationRegistry` is a singleton `ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>>` keyed by Spring Batch job execution ID, allowing `CatalogItemWriteListener` to look up the SSE consumer without passing it as a method argument.

## Goals / Non-Goals

**Goals:**
- `CatalogJobConfig` defines: a `Job` with one `PartitionedStep`, a `ConcurrentTaskExecutor` with grid size `${photomanager.catalog-partition-grid-size:4}`, and chunk size `${photomanager.catalog-chunk-size:50}`
- `CatalogFolderPartitioner` scans root catalog folders and emits one `ExecutionContext` per folder with `folderPath` and `startIndex` parameters
- `CatalogFileItemReader` reads files in the partition's folder not yet in `assets` table (paged by `startIndex`)
- `CatalogAssetItemProcessor` is stateless; computes hash, generates thumbnail, reads EXIF (reuses existing logic from `CatalogFolderServiceImpl`)
- `CatalogAssetItemWriter` saves assets and exif; deletes assets whose files were removed; calls `SseNotificationRegistry.getConsumer(jobExecutionId).accept(notification)` if present
- `CatalogAssetsUseCaseImpl` calls `jobLauncher.run(catalogJob, jobParameters)` and puts the SSE consumer into `SseNotificationRegistry` keyed by `JobExecution.id`; returns immediately (job runs async via `AsyncJobLauncher`)
- Flyway V27: `CREATE TABLE BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, ... (use Spring Batch's provided schema SQL for PostgreSQL); `DROP TABLE IF EXISTS catalog_run_state`

**Non-Goals:**
- Restart-from-last-failed-chunk (Spring Batch supports this but the UI does not expose restart; a simple re-run is sufficient)
- Spring Batch Admin UI
- Persisting SSE consumer across process restarts (SSE is ephemeral by nature)

## Decisions

### 1. `AsyncJobLauncher` so `CatalogAssetsUseCaseImpl.execute()` returns immediately

**Decision:** Configure `JobLauncher` with `SimpleAsyncTaskExecutor` so `jobLauncher.run()` returns a `JobExecution` immediately and the job runs in a background thread.

**Rationale:** The controller returns the `SseEmitter` to the HTTP client before the job completes. The job must run asynchronously.

### 2. `SseNotificationRegistry` decouples SSE from Spring Batch internals

**Decision:** A singleton `SseNotificationRegistry` keyed by `JobExecution.id` stores the `Consumer<CatalogChangeNotification>` registered by the controller when the SSE connection opens.

**Rationale:** Spring Batch `ItemWriter` beans are Spring-managed; they cannot accept the SSE consumer as a constructor argument (it's a per-request object). The registry provides a thread-safe lookup.

### 3. Spring Batch PostgreSQL schema via Flyway V27

**Decision:** Include Spring Batch's provided `schema-postgresql.sql` content in Flyway V27, then `DROP TABLE IF EXISTS catalog_run_state`.

**Rationale:** Flyway manages all schema changes. Letting Spring Batch auto-initialize its schema (`spring.batch.initialize-schema: never` in test/prod) avoids conflicts with Flyway.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Spring Batch adds 9 new tables | Low | They are managed by Flyway V27; no operational impact |
| Partition threads compete for DB connection pool | Medium | Configure pool size to accommodate `gridSize + 5` connections |
| `SseNotificationRegistry` memory leak if SSE connection is never closed | Low | Clear the entry when the job completes or the SSE emitter times out |
