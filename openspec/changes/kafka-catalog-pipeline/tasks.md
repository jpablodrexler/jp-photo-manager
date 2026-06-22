## 1. Infrastructure Setup

- [x] 1.1 Add `spring-kafka` dependency to `JPPhotoManagerWeb/backend/pom.xml`
- [x] 1.2 Add `spring-kafka-test` dependency (test scope) to `pom.xml`
- [x] 1.3 Add `kafka` service to `JPPhotoManagerWeb/docker-compose.yml` (`apache/kafka:3.9.0`, KRaft mode, port 9092, no ZooKeeper)
- [x] 1.4 Add `spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}` to `src/main/resources/application.yml`
- [x] 1.5 Add `spring.kafka.bootstrap-servers: ${spring.embedded.kafka.brokers}` to `src/test/resources/application-test.yml`

## 2. Kafka Message DTOs

- [x] 2.1 Create `application/dto/CatalogProgressMessage.java` — record with `long runId`, `CatalogChangeNotification notification`, `boolean done`
- [x] 2.2 Create `application/dto/SyncProgressMessage.java` — record with `long runId`, `String status`, `List<SyncAssetsResult> results`, `boolean done`
- [x] 2.3 Create `application/dto/ConvertProgressMessage.java` — record with `long runId`, `String status`, `List<ConvertAssetsResult> results`, `boolean done`
- [x] 2.4 Create `application/dto/AssetCatalogedEvent.java` — record with `Long assetId`, `String folderPath`, `Instant timestamp`
- [x] 2.5 Create `application/dto/AssetDeletedEvent.java` — record with `Long assetId`, `String folderPath`, `Instant timestamp`, `boolean permanent`

## 3. Kafka Topic Configuration

- [x] 3.1 Create `infrastructure/config/KafkaTopicConfig.java` with `@Configuration` and `@Bean NewTopic` declarations for all five topics (`job.catalog.progress` 3 partitions 1h retention, `job.sync.progress` 1 partition 1h, `job.convert.progress` 1 partition 1h, `asset.cataloged` 3 partitions 7d, `asset.deleted` 3 partitions 7d)

## 4. KafkaProgressRegistry

- [x] 4.1 Create `infrastructure/service/KafkaProgressRegistry.java` — `@Service` holding `ConcurrentHashMap<Long, SseEmitter>`; methods `register(long runId, SseEmitter)`, `get(long runId)`, `remove(long runId)`
- [x] 4.2 Write unit test `KafkaProgressRegistryTest` — register, get, remove; verify thread-safety with concurrent access

## 5. KafkaProgressListener

- [x] 5.1 Create `infrastructure/kafka/KafkaProgressListener.java` — `@Component` with three `@KafkaListener` methods (one per progress topic, consumer group `sse-broadcaster`)
- [x] 5.2 Implement catalog listener: look up emitter by `runId`; send `catalog` SSE event for each non-done message; call `emitter.complete()` and `registry.remove(runId)` on `done=true`; swallow `SseEmitter` I/O errors with log warning; skip silently if no emitter found for `runId`
- [x] 5.3 Implement sync listener: send `status` SSE events for non-done messages; send `results` SSE event then `emitter.complete()` on `done=true`
- [x] 5.4 Implement convert listener: same pattern as sync listener
- [x] 5.5 Write `KafkaProgressListenerTest` with `@EmbeddedKafka` — verify catalog events routed to emitter; verify `emitter.complete()` called on done; verify unknown runId is silently skipped

## 6. Modify Catalog Batch Components

- [x] 6.1 Change `CatalogAssetsUseCase.execute(Consumer<CatalogChangeNotification>) → CompletableFuture<Void>` interface to `execute(long runId) → CompletableFuture<Void>` (kept return type for integration-test blocking)
- [x] 6.2 Update `CatalogAssetsUseCaseImpl`: remove `SseNotificationRegistry` dependency; accept `runId` parameter; remove consumer wrapping and registry calls; keep Spring Batch job launch with `runId` job parameter
- [x] 6.3 Add `KafkaTemplate<String, Object>` injection to `CatalogAssetItemWriter`
- [x] 6.4 In `CatalogAssetItemWriter.write()`: replace `consumer.accept(...)` calls with `kafkaTemplate.send("job.catalog.progress", runId.toString(), message)` and `kafkaTemplate.send("asset.cataloged", ...)` for each new asset
- [x] 6.5 In `CatalogAssetItemWriter.afterStep()`: replace `consumer.accept(...)` with Kafka publish for `ASSET_DELETED`; also publish to `asset.deleted`
- [x] 6.6 In `CatalogAssetItemWriter.ensureFolderExists()`: replace `consumer.accept(...)` with Kafka publish for `FOLDER_CREATED`
- [x] 6.7 Remove `SseNotificationRegistry` from `CatalogAssetItemWriter` constructor and `CatalogJobConfig` wiring
- [x] 6.8 Update `CatalogItemWriteListener.afterJob()`: replace `sseNotificationRegistry.complete(runId)` and `.remove(runId)` with `kafkaTemplate.send("job.catalog.progress", runId.toString(), CatalogProgressMessage.done(runId))`; remove `SseNotificationRegistry` dependency
- [x] 6.9 Update `CatalogJobConfig` to inject `KafkaTemplate` beans instead of `SseNotificationRegistry` when constructing step/listener beans

## 7. Modify Sync and Convert Use Cases

- [x] 7.1 Change `SyncAssetsUseCase.execute(Consumer<String>) → CompletableFuture<List<SyncAssetsResult>>` to `execute(long runId) → void`
- [x] 7.2 Update `SyncAssetsUseCaseImpl`: inject `KafkaTemplate<String, SyncProgressMessage>`; replace `Consumer<String>` callback calls with Kafka publishes to `job.sync.progress`; publish `done=true` message with results list when complete
- [x] 7.3 Change `ConvertAssetsUseCase.execute(Consumer<String>) → CompletableFuture<List<ConvertAssetsResult>>` to `execute(long runId) → void`
- [x] 7.4 Update `ConvertAssetsUseCaseImpl`: inject `KafkaTemplate<String, ConvertProgressMessage>`; replace callback calls with Kafka publishes to `job.convert.progress`; publish `done=true` with results when complete

## 8. Modify Controllers

- [x] 8.1 Update `AssetController.catalogAssets()`: inject `KafkaProgressRegistry`; generate `runId`; register emitter; call `catalogAssetsUseCase.execute(runId)`; return emitter immediately (remove `ExecutorService` and `CompletableFuture.get()` blocking)
- [x] 8.2 Update `SyncController.run()`: inject `KafkaProgressRegistry`; generate `runId`; register emitter; call `syncAssetsUseCase.execute(runId)`; return emitter immediately
- [x] 8.3 Update `ConvertController.run()`: same pattern as SyncController

## 9. Delete SseNotificationRegistry

- [x] 9.1 Delete `infrastructure/service/SseNotificationRegistry.java`
- [x] 9.2 Verify no remaining references to `SseNotificationRegistry` in the codebase (`mvn compile` succeeds)

## 10. Update Tests

- [x] 10.1 Update `CatalogAssetsUseCaseImplTest`: remove `SseNotificationRegistry` mock; verify `KafkaTemplate.send()` is called (or update to use new signature); fix compilation errors from interface change
- [x] 10.2 Update `CatalogBatchIntegrationTest`: add `@EmbeddedKafka`; verify `job.catalog.progress` topic receives expected messages and `done=true` at completion
- [x] 10.3 Update `SyncAssetsUseCaseImplTest`: remove consumer mock; verify `KafkaTemplate.send()` called with `done=true` and results
- [x] 10.4 Update `ConvertAssetsUseCaseImplTest`: same as sync
- [x] 10.5 Update `AssetController` tests (if any): remove `CompletableFuture` blocking assumption; verify `catalogAssetsUseCase.execute(runId)` called with a valid runId
- [x] 10.6 Run full unit test suite (`mvn test`) and confirm all tests pass
- [ ] 10.7 Run integration tests (`mvn verify -Pintegration-tests`) with Docker running and confirm all tests pass
