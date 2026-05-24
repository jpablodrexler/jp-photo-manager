## 1. Dependency

- [x] 1.1 Add `spring-boot-starter-batch` to `pom.xml`
- [x] 1.2 Set `spring.batch.job.enabled: false` and `spring.batch.initialize-schema: never` in `application.yml` (Flyway handles schema)

## 2. Database migration

- [x] 2.1 Create `V16__spring_batch_schema.sql`: include Spring Batch's PostgreSQL DDL (create 6 `BATCH_*` tables + 3 sequences); add `DROP TABLE IF EXISTS catalog_run_state` at the end

## 3. SseNotificationRegistry

- [x] 3.1 Create `infrastructure/service/SseNotificationRegistry.java` annotated with `@Service`
- [x] 3.2 Maintain `ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>> registry` (keyed by run ID)
- [x] 3.3 Methods: `void register(long runId, Consumer<CatalogChangeNotification> consumer, CompletableFuture<Void> completion)`, `Consumer<CatalogChangeNotification> get(long runId)`, `void complete(long runId)`, `void remove(long runId)`

## 4. Spring Batch infrastructure/batch/ package

- [x] 4.1 Create `CatalogJobConfig.java` (`@Configuration`): define `Job catalogJob(Step catalogPartitionStep)`, `Step catalogPartitionStep(...)` using `PartitionStep` with `gridSize = ${photomanager.catalog-partition-grid-size:4}` and `TaskExecutorPartitionHandler` backed by a `ThreadPoolTaskExecutor`
- [x] 4.2 Create `CatalogFolderPartitioner.java` implementing `Partitioner`: scan root catalog folders; emit one `ExecutionContext` per folder with `folderPath` key
- [x] 4.3 Create `CatalogFileItemReader.java` implementing `ItemReader<Path>`: list files in the partition's folder that are not in the `assets` table
- [x] 4.4 Create `CatalogAssetItemProcessor.java` implementing `ItemProcessor<Path, CatalogBatchItem>`: compute SHA-256, generate thumbnail, read EXIF (extract logic from current `CatalogFolderServiceImpl`)
- [x] 4.5 Create `CatalogAssetItemWriter.java` implementing `ItemWriter<CatalogBatchItem>`: save asset + exif rows; delete soft-deleted assets; call `sseNotificationRegistry.get(runId)?.accept(notification)` for each written asset
- [x] 4.6 Create `CatalogItemWriteListener.java` implementing `ItemWriteListener<CatalogBatchItem>` and `JobExecutionListener`: on job completion complete the SSE future and remove from registry

## 5. CatalogAssetsUseCaseImpl refactoring

- [x] 5.1 Replace the `@Async` loop with `JobLauncher.run(catalogJob, jobParametersBuilder.toJobParameters())`
- [x] 5.2 Register SSE consumer with `sseNotificationRegistry.register(runId, consumer, completionFuture)` before starting the job
- [x] 5.3 Remove `CatalogStateRepository`, `CatalogRunStateEntity`, `JpaCatalogStateRepository`, and `CatalogStateRepositoryImpl` — replaced by `JobRepository`

## 6. Backend integration tests

- [x] 6.1 Write a Spring Batch integration test using `@SpringBatchTest` that runs the catalog job against a temp directory with 3 image files
- [x] 6.2 Test that exactly 3 `Asset` rows are created
- [x] 6.3 Test that a second run of the same job skips already-cataloged files (idempotency)
- [x] 6.4 Test that `SseNotificationRegistry` receives notifications during the job run

## 7. Testing and Commit

- [x] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 7.3 Commit all changes (only after both test suites pass)
