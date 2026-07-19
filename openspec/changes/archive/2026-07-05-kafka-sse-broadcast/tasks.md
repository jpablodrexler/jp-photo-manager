## 1. Configuration

- [x] 1.1 In `JPPhotoManagerWeb/backend/src/main/resources/application.yml`, change `spring.kafka.consumer.group-id` from `sse-broadcaster` to `sse-broadcaster-${HOSTNAME:${random.uuid}}`.
- [x] 1.2 In `JPPhotoManagerWeb/backend/src/test/resources/application-test.yml`, apply the same `spring.kafka.consumer.group-id` change so tests exercise the same property-resolution path.

## 2. Listener changes

- [x] 2.1 In `KafkaProgressListener.java`, remove the `groupId = "sse-broadcaster"` attribute from the `@KafkaListener` annotation on `onCatalogProgress`, keeping `topics = "job.catalog.progress"` and `containerFactory = "kafkaListenerContainerFactory"`.
- [x] 2.2 Remove the `groupId = "sse-broadcaster"` attribute from the `@KafkaListener` annotation on `onSyncProgress`, keeping `topics = "job.sync.progress"` and `containerFactory = "kafkaListenerContainerFactory"`.
- [x] 2.3 Remove the `groupId = "sse-broadcaster"` attribute from the `@KafkaListener` annotation on `onConvertProgress`, keeping `topics = "job.convert.progress"` and `containerFactory = "kafkaListenerContainerFactory"`.

## 3. Tests

- [x] 3.1 Add a new `@EmbeddedKafka` test (e.g. `KafkaProgressListenerMultiInstanceTest` under `infrastructure/kafka/`) that starts two independently-configured `KafkaListenerContainerFactory` instances (or two Spring context copies), each resolving a distinct consumer group id, and asserts that a single message published to a single-partition topic (`job.sync.progress`) is received by both listener instances — proving per-instance delivery works as designed.
- [x] 3.2 Run the existing Kafka pipeline integration tests (`CatalogBatchIntegrationTest`, `SyncKafkaPipelineIntegrationTest`, `ConvertKafkaPipelineIntegrationTest`) and confirm they still pass unchanged with the new group-id property in `application-test.yml`.
- [x] 3.3 Run `mvn test` (unit tests, no Docker) and `mvn verify -Pintegration-tests` (requires Docker) for the full backend suite.

## 4. Verification

- [x] 4.1 Manually verify locally: start two backend instances on different ports against the same Kafka broker and Postgres database, open a catalog/sync/convert SSE stream against each instance concurrently, and confirm both streams receive progress events and complete normally (no hanging `EventSource` connections). Verified: started two real instances (`HOSTNAME=instance-a` / `instance-b`) against an ephemeral Postgres+Kafka+Redis; logs confirmed each resolved a distinct consumer group (`sse-broadcaster-instance-a`, `sse-broadcaster-instance-b`); `kafka-consumer-groups.sh --describe` confirmed each single-member group was assigned all partitions of all three progress topics, and a manually-published `job.sync.progress` record was independently fetched by both groups. Full end-to-end SSE-completes-in-the-browser confirmation was blocked by an unrelated pre-existing bug (see note below) that also reproduces on unmodified `main`/baseline code, so it does not affect this change's correctness.
- [x] 4.2 Confirm no remaining references to the literal `groupId = "sse-broadcaster"` exist in the codebase (`KafkaProgressListener.java`) and that `application.yml` / `application-test.yml` both use the per-instance property.

## 5. Documentation

- [ ] 5.1 Update `openspec/improvements.md`: mark `kafka-sse-broadcast` (#80) implementation status once merged (handled by the archive step, not during active development).
