## Why

Every application instance currently joins the exact same hardcoded Kafka consumer group, `sse-broadcaster` (`spring.kafka.consumer.group-id: sse-broadcaster` in `application.yml`, reiterated via the `groupId = "sse-broadcaster"` attribute on all three `@KafkaListener` methods in `KafkaProgressListener`). A Kafka consumer group only ever delivers a given partition to one member at a time, so in a multi-instance deployment: `job.sync.progress` and `job.convert.progress` (1 partition each) are consumed by exactly one instance cluster-wide, and `job.catalog.progress` (3 partitions) can have its partitions spread across instances that may not be the one holding the browser's `SseEmitter` for a given `runId`. In both cases the progress event is silently dropped — the SSE stream returned by `GET /api/assets/catalog`, `/api/sync/run`, or `/api/convert/run` hangs with no further events and never completes, even though the underlying job finished successfully. This is a correctness bug in the multi-instance SSE fan-out introduced by `kafka-catalog-pipeline` (#75), and it needs fixing before this application is scaled beyond a single instance.

## What Changes

- Give each application instance its own unique Kafka consumer group by changing `spring.kafka.consumer.group-id` in `application.yml` (and `application-test.yml`) to `sse-broadcaster-${HOSTNAME:${random.uuid}}`, so every JVM process joins a distinct, single-member consumer group.
- Remove the hardcoded `groupId = "sse-broadcaster"` attribute from all three `@KafkaListener` methods (`onCatalogProgress`, `onSyncProgress`, `onConvertProgress`) in `KafkaProgressListener`, so each listener inherits the per-instance group id configured above rather than overriding it back to the shared literal.
- No change to `KafkaProgressRegistry`: its existing "look up the local emitter for this `runId`; skip silently if absent" behavior is exactly what makes per-instance-group delivery safe — every instance now receives every progress message, forwards it to a locally-registered `SseEmitter` if one exists, and does nothing otherwise.
- No new topic, partition count, retention policy, Maven dependency, or Docker service is introduced.

## Capabilities

### New Capabilities

(none — this change modifies delivery semantics of an existing capability)

### Modified Capabilities

- `kafka-catalog-pipeline`: the "KafkaProgressListener fans out events to local SSE clients" requirement changes from a single shared consumer group (`sse-broadcaster`) to a per-instance unique consumer group, so that every instance in a multi-instance deployment receives every progress message instead of only one instance cluster-wide. The "Kafka configuration in application.yml" requirement changes to document the per-instance group-id property.

## Impact

- **Code:** `KafkaProgressListener.java` (drop the `groupId` attribute from all three `@KafkaListener` annotations); `application.yml` and `application-test.yml` (change `spring.kafka.consumer.group-id` to a per-instance unique value).
- **Behavior:** No API contract change — `GET /api/assets/catalog`, `/api/sync/run`, and `/api/convert/run` keep the same request/response shape. The fix is purely about which instance(s) receive and act on each Kafka message.
- **Tests:** Existing `@EmbeddedKafka` integration tests (`CatalogBatchIntegrationTest`, `SyncKafkaPipelineIntegrationTest`, `ConvertKafkaPipelineIntegrationTest`) continue to work unchanged since a single JVM test process still ends up as the sole member of its own consumer group. A new test should verify that two independently-configured listener containers (simulating two instances, each with a distinct group id) both receive the same message from a single-partition topic.
- **Dependencies:** None added or removed.
- **Deployment:** No schema, infrastructure, or configuration provisioning change beyond the `application.yml` property edit.
