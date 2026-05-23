## 1. Dependency

- [ ] 1.1 Add `spring-boot-starter-batch` to `pom.xml`
- [ ] 1.2 Set `spring.batch.job.enabled: false` and `spring.batch.initialize-schema: never` in `application.yml` (Flyway handles schema)

## 2. Database migration

- [ ] 2.1 Create `V27__spring_batch_schema.sql`: include Spring Batch's PostgreSQL DDL (create 9 `BATCH_*` tables); add `DROP TABLE IF EXISTS catalog_run_state` at the end

## 3. SseNotificationRegistry

- [ ] 3.1 Create `infrastructure/service/SseNotificationRegistry.java` annotated with `@Service`
- [ ] 3.2 Maintain `ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>> registry` (keyed by job execution ID)
- [ ] 3.3 Methods: `void register(long jobId, Consumer<CatalogChangeNotification> consumer)`, `Consumer<CatalogChangeNotification> get(long jobId)`, `void remove(long jobId)`

## 4. Spring Batch infrastructure/batch/ package

- [ ] 4.1 Create `CatalogJobConfig.java` (`@Configuration`): define `Job catalogJob(Step catalogPartitionStep)`, `Step catalogPartitionStep(...)` using `PartitionStep` with `gridSize = ${photomanager.catalog-partition-grid-size:4}` and `TaskExecutorPartitionHandler` backed by a `ThreadPoolTaskExecutor`
- [ ] 4.2 Create `CatalogFolderPartitioner.java` implementing `Partitioner`: scan root catalog folders; emit one `ExecutionContext` per folder with `folderPath` key
- [ ] 4.3 Create `CatalogFileItemReader.java` implementing `ItemReader<Path>`: list files in the partition's folder that are not in the `assets` table
- [ ] 4.4 Create `CatalogAssetItemProcessor.java` implementing `ItemProcessor<Path, Asset>`: compute SHA-256, generate thumbnail, read EXIF (extract logic from current `CatalogFolderServiceImpl`)
- [ ] 4.5 Create `CatalogAssetItemWriter.java` implementing `ItemWriter<Asset>`: save asset + exif rows; delete soft-deleted assets; call `sseNotificationRegistry.get(jobId)?.accept(notification)` for each written asset
- [ ] 4.6 Create `CatalogItemWriteListener.java` implementing `ItemWriteListener<Asset>`: after write, remove completed partition entries from registry; on job completion send `done` event

## 5. CatalogAssetsUseCaseImpl refactoring

- [ ] 5.1 Replace the `@Async` loop with `JobLauncher.run(catalogJob, jobParametersBuilder.toJobParameters())`
- [ ] 5.2 Get the returned `JobExecution.id`; register the SSE consumer with `sseNotificationRegistry.register(jobId, consumer)`
- [ ] 5.3 Remove `CatalogStateRepository`, `CatalogRunStateEntity`, `JpaCatalogStateRepository`, and `CatalogStateRepositoryImpl` — replaced by `JobRepository`

## 6. Backend integration tests

- [ ] 6.1 Write a Spring Batch integration test using `@SpringBatchTest` that runs the catalog job against a temp directory with 3 image files
- [ ] 6.2 Test that exactly 3 `Asset` rows are created
- [ ] 6.3 Test that a second run of the same job skips already-cataloged files (idempotency)
- [ ] 6.4 Test that `SseNotificationRegistry` receives notifications during the job run

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
