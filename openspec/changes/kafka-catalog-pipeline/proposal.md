## Why

The current `SseNotificationRegistry` is a `ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>>` that lives in a single JVM — SSE progress events for catalog, sync, and convert operations never cross instance boundaries, so any horizontally scaled deployment silently drops progress updates for browsers connected to a different node. Replacing it with Kafka topics removes the single-JVM constraint and makes multi-instance deployments viable.

## What Changes

- Replace `SseNotificationRegistry` with Kafka producers/consumers for all long-running operation progress events (catalog, sync, convert).
- Each app instance runs a `@KafkaListener` on all job-progress topics and fans out to its locally connected `SseEmitter` clients.
- Topics introduced: `job.catalog.progress`, `job.sync.progress`, `job.convert.progress`, `asset.cataloged`, `asset.deleted`.
- `CatalogItemWriteListener`, `SyncAssetsUseCaseImpl`, and `ConvertAssetsUseCaseImpl` publish to Kafka instead of calling `registry.get(runId)`.
- `SseNotificationRegistry` class is deleted.
- Docker Compose gains a `kafka` service (Apache Kafka 3.9 in KRaft mode); no ZooKeeper required.
- `application.yml` gains `spring.kafka.bootstrap-servers` property.
- No Flyway migration — this change does not touch the PostgreSQL schema.

## Capabilities

### New Capabilities

- `kafka-catalog-pipeline`: Kafka-backed SSE progress broadcasting; replaces the in-memory registry so that catalog, sync, and convert progress events are delivered to browser clients regardless of which app instance they are connected to; also publishes durable `asset.cataloged` and `asset.deleted` events consumed by future improvements (#73 mongodb-audit-log, #76 kafka-async-upload, #77 kafka-catalog-coordination, #80 redis-sse-pubsub).

### Modified Capabilities

<!-- No existing spec-level requirements change. The SSE endpoints, event shapes, and client-observable behaviour are identical; only the internal delivery mechanism changes. -->

## Impact

- **Backend**: `CatalogItemWriteListener`, `SyncAssetsUseCaseImpl`, `ConvertAssetsUseCaseImpl` — replace registry calls with `KafkaTemplate.send()`; new `KafkaProgressListener` component subscribes and drives `SseEmitter` fan-out; `SseNotificationRegistry` removed; `AppConfig` gains Kafka producer/consumer beans; `pom.xml` adds `spring-kafka`.
- **Infrastructure**: `docker-compose.yml` gains `kafka` service (apache/kafka:3.9.0, KRaft mode, port 9092); `application.yml` gains bootstrap-servers config and topic retention properties.
- **Tests**: `CatalogItemWriteListenerTest`, `SyncAssetsUseCaseImplTest`, `ConvertAssetsUseCaseImplTest` updated to verify Kafka publishes instead of registry calls; new `KafkaProgressListenerTest`; integration tests use `@EmbeddedKafka`.
- **Downstream improvements unblocked**: #73, #76, #77, #80 all depend on the Kafka infrastructure introduced here.
