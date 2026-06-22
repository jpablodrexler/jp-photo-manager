## Context

Long-running operations (catalog, sync, convert) stream progress to the browser via SSE. The current delivery mechanism is `SseNotificationRegistry` — a `ConcurrentHashMap<Long, Consumer<CatalogChangeNotification>>` that registers a callback per `runId` and holds it for the lifetime of the job. Because this map lives in a single JVM, SSE events produced by a catalog job running on instance A are never delivered to a browser connected to instance B. Any multi-instance deployment silently drops progress updates for clients on the "wrong" node.

The fix is to route all progress events through Kafka topics. Each app instance subscribes to those topics and fans out events to its locally connected `SseEmitter` clients. The external observable behaviour (same SSE event shapes, same endpoints) is unchanged; only the internal delivery path changes.

**Current flow (catalog):**
1. `AssetController.catalogAssets()` creates `SseEmitter`, passes a lambda consumer to `catalogAssetsUseCase.execute(consumer)`, and blocks on `CompletableFuture.get()`.
2. `CatalogAssetsUseCaseImpl.execute()` generates a `runId`, wraps the consumer, registers it in `SseNotificationRegistry`, launches the Spring Batch job.
3. `CatalogAssetItemWriter.write()` calls `sseNotificationRegistry.get(runId)` to obtain the consumer and invokes it per asset.
4. `CatalogItemWriteListener.afterJob()` calls `sseNotificationRegistry.complete(runId)` which resolves the `CompletableFuture`, unblocking the controller thread, which then calls `emitter.complete()`.

For sync and convert the pattern is similar except the use case takes a `Consumer<String>` closure directly — `SseNotificationRegistry` is not involved — and `CompletableFuture.get()` blocks the controller.

## Goals / Non-Goals

**Goals:**
- Route catalog, sync, and convert progress events through Kafka so SSE delivery works correctly in multi-instance deployments.
- Publish durable `asset.cataloged` and `asset.deleted` events that future improvements (#73 mongodb-audit-log, #76 kafka-async-upload, #77 kafka-catalog-coordination, #80 redis-sse-pubsub) can consume.
- Remove `SseNotificationRegistry` entirely.
- Add Kafka to `docker-compose.yml` for local development.
- No change to the external SSE API shape or any frontend code.

**Non-Goals:**
- Building any of the dependent improvements (#73, #76, #77, #80) — only the Kafka infrastructure and progress producers/consumers.
- Persistent event replay for SSE clients that reconnect mid-job — `job.*.progress` topics use a short retention (1 hour); missed events are not replayed.
- Replacing the PostgreSQL distributed-lock mechanism in `scheduled-catalog` — that remains as-is.

## Decisions

### 1. Controller generates the runId (not the use case)

**Decision:** `AssetController`, `SyncController`, and `ConvertController` generate `runId = System.currentTimeMillis()` themselves, register the `SseEmitter` in a new `KafkaProgressRegistry`, then call `execute(runId)`.

**Rationale:** In the new model the controller must register the emitter *before* the job starts, so Kafka events published during early steps are not lost. If the use case generated the runId, there would be a race window between job launch and emitter registration. Passing `runId` into the use case is also a clean dependency direction — the use case accepts an opaque identifier without knowing anything about Kafka or SSE.

**Alternative considered:** Keep use case generating runId and have `execute()` return the runId as a first step before launching async work. Rejected because it complicates the `@Async` boundary and requires an extra synchronisation point.

### 2. Use case interfaces change: drop Consumer callback, accept runId

**Decision:** `CatalogAssetsUseCase.execute(Consumer<CatalogChangeNotification>)` becomes `execute(long runId)`. `SyncAssetsUseCase` and `ConvertAssetsUseCase` gain the same `runId` parameter and drop their `Consumer<String>` parameter.

**Rationale:** With Kafka as the delivery bus, use cases no longer need a callback — they publish events directly. Keeping the `Consumer` parameter would mean the use case has two notification paths (Kafka and callback), which is confusing and hard to test.

**Alternative considered:** Keep `Consumer` and have use-case implementations that also publish to Kafka. Rejected because it introduces two delivery paths and makes `SseNotificationRegistry` redundant but still present.

### 3. Three ephemeral progress topics + two durable event topics

**Decision:**
- `job.catalog.progress` — ephemeral, 1-hour retention, carries `CatalogProgressMessage{runId, notification, done}`
- `job.sync.progress` — ephemeral, 1-hour retention, carries `SyncProgressMessage{runId, status, results, done}`
- `job.convert.progress` — ephemeral, 1-hour retention, carries `ConvertProgressMessage{runId, status, results, done}`
- `asset.cataloged` — durable, 7-day retention, carries `AssetCatalogedEvent{assetId, folderPath, timestamp}`
- `asset.deleted` — durable, 7-day retention, carries `AssetDeletedEvent{assetId, folderPath, timestamp, permanent}`

**Rationale:** Progress events have no value after the job ends; short retention keeps Kafka storage lean. `asset.cataloged`/`asset.deleted` are the primary integration points for future consumers and benefit from a replay window (7 days covers a weekend outage scenario).

**Alternative considered:** One merged topic for all progress types with a discriminator field. Rejected because it forces every consumer to deserialise and discard irrelevant messages; separate topics allow targeted consumption.

### 4. `KafkaProgressRegistry` holds `SseEmitter` per runId (replaces `SseNotificationRegistry`)

**Decision:** A new `KafkaProgressRegistry` service holds `Map<Long, SseEmitter>` locally. `KafkaProgressListener` looks up the emitter by `runId` on receipt and writes to it. If no emitter is found (event arrived on a different instance), the message is silently skipped.

**Rationale:** Each instance only knows about SSE connections it accepted. A Kafka consumer on every instance ensures the correct instance delivers the event. The map entry is removed when `done=true` or when the emitter completes/errors.

### 5. Kafka in KRaft mode (no ZooKeeper)

**Decision:** `docker-compose.yml` adds `apache/kafka:3.9.0` in KRaft mode (single node, `KAFKA_PROCESS_ROLES: broker,controller`).

**Rationale:** KRaft eliminates ZooKeeper as a dependency, simplifying the local development setup to a single Kafka container. Single-node is sufficient for development; production deployments can add replicas.

### 6. `@EmbeddedKafka` for integration tests

**Decision:** Integration tests use `@EmbeddedKafka` (provided by `spring-kafka-test`) instead of a Testcontainers Kafka image.

**Rationale:** `@EmbeddedKafka` starts in-process with no Docker dependency, keeping the unit-test profile (no Docker) fast. The `integration-tests` Maven profile (Docker required) already uses Testcontainers for PostgreSQL; adding Testcontainers Kafka there is possible but deferred until needed.

## Risks / Trade-offs

- **Message ordering within a partition** — Kafka guarantees per-partition ordering. All progress messages for a given `runId` should be published to the same partition. Using `runId.toString()` as the message key achieves this because Kafka's default partitioner hashes the key. → Low risk in practice (single Spring Batch job per run).
- **Consumer group offset lag** — if the Kafka consumer falls behind (e.g. under heavy load), SSE events arrive at the browser late. For a dev/single-instance setup this is negligible. In production, ensure the `sse-broadcaster` consumer group has enough throughput. → Mitigation: set `max.poll.records=500` and keep progress message payloads small (no embedded image data).
- **Emitter not found on this instance** — if a client connects to instance A and the catalog job runs on instance B, instance A's `KafkaProgressRegistry` has no entry for that `runId`. The message is dropped silently. This is the correct behaviour in a multi-instance deployment: instance B's `KafkaProgressListener` finds the emitter on instance B and delivers there. In single-instance mode there is no issue. → Accepted trade-off. Future improvement #80 (`redis-sse-pubsub`) adds Redis Pub/Sub as an optimised cross-instance fan-out layer.
- **Lost events between emitter registration and job start** — the controller registers the emitter before calling `execute(runId)`. Any events published before the Kafka consumer assigns the partition (cold start) could be missed. → Mitigation: `auto.offset.reset=latest` on the `sse-broadcaster` consumer group; registration always precedes job launch; warm consumers (already assigned) have no lag.
- **`done` message delivery and emitter lifetime** — if the Kafka `done` message is processed before the last progress message (out-of-order within a partition is impossible; but consider consumer rebalances), the emitter might be closed early. → Mitigation: same-partition guarantee eliminates out-of-order; `done=true` messages are produced last in `afterJob()` / after `CompletableFuture` resolution.

## Migration Plan

1. Add `spring-kafka` to `pom.xml`.
2. Add `kafka` service to `docker-compose.yml`.
3. Add `spring.kafka.bootstrap-servers` to `application.yml` and `application-test.yml` (EmbeddedKafka address for tests).
4. Implement `KafkaProgressRegistry`, `KafkaProgressListener`, and progress message DTOs.
5. Modify `CatalogAssetItemWriter` and `CatalogItemWriteListener` to publish to `job.catalog.progress`; also publish to `asset.cataloged` / `asset.deleted`.
6. Modify `SyncAssetsUseCaseImpl` and `ConvertAssetsUseCaseImpl` to accept `runId` and publish to `job.sync.progress` / `job.convert.progress`.
7. Update use-case interfaces (`CatalogAssetsUseCase`, `SyncAssetsUseCase`, `ConvertAssetsUseCase`).
8. Update controllers to generate `runId`, register emitter, call `execute(runId)`, and return emitter immediately (remove `CompletableFuture.get()` blocking).
9. Delete `SseNotificationRegistry`.
10. Update tests: replace `SseNotificationRegistry` mocks with `KafkaTemplate` mocks; add `KafkaProgressListenerTest` with `@EmbeddedKafka`.

**Rollback:** revert steps 4–10; no schema change means there is no migration to undo. The Kafka container can be left running without breaking the app if it is not yet connected.

## Open Questions

- None — design is complete. Topic names, retention, and partitioning are specified above.
