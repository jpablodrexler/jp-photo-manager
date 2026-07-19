### Requirement: Catalog progress events published to Kafka

During catalog execution, the system SHALL publish a `CatalogProgressMessage` to the `job.catalog.progress` Kafka topic for every asset written, folder created, and asset deleted. When the catalog job finishes, the system SHALL publish a final `CatalogProgressMessage` with `done=true`.

Each message SHALL carry: `runId` (long), `notification` (CatalogChangeNotification with reason, asset/folder data), and `done` (boolean).

All messages for a given `runId` SHALL use `runId.toString()` as the Kafka message key to guarantee same-partition ordering.

#### Scenario: Asset written during catalog publishes progress message
- **WHEN** `CatalogAssetItemWriter.write()` persists an asset with `runId=42`
- **THEN** a `CatalogProgressMessage{runId=42, notification.reason=ASSET_CREATED, done=false}` is published to `job.catalog.progress`

#### Scenario: Catalog job completion publishes done message
- **WHEN** `CatalogItemWriteListener.afterJob()` is called after the Spring Batch job finishes
- **THEN** a `CatalogProgressMessage{runId=42, done=true}` is published to `job.catalog.progress` as the last message for that runId

#### Scenario: Folder created during catalog publishes progress message
- **WHEN** `CatalogAssetItemWriter` creates a new folder entry
- **THEN** a `CatalogProgressMessage{runId, notification.reason=FOLDER_CREATED, done=false}` is published

---

### Requirement: Sync progress events published to Kafka

During sync execution, the system SHALL publish a `SyncProgressMessage` to the `job.sync.progress` topic for every status update. When sync completes, the system SHALL publish a final `SyncProgressMessage` with `done=true` and the full `results` list.

#### Scenario: Sync status update published per file processed
- **WHEN** `SyncAssetsUseCaseImpl.execute(runId)` processes each file
- **THEN** a `SyncProgressMessage{runId, status="...", done=false}` is published to `job.sync.progress`

#### Scenario: Sync completion published with results
- **WHEN** sync finishes successfully
- **THEN** a `SyncProgressMessage{runId, done=true, results=[...]}` is published as the final message

---

### Requirement: Convert progress events published to Kafka

During convert execution, the system SHALL publish a `ConvertProgressMessage` to the `job.convert.progress` topic for every status update, and a final message with `done=true` and results when complete.

#### Scenario: Convert completion published with results
- **WHEN** convert finishes
- **THEN** a `ConvertProgressMessage{runId, done=true, results=[...]}` is published to `job.convert.progress`

---

### Requirement: Durable asset lifecycle events published to Kafka

When an asset is created or deleted during cataloging, the system SHALL publish a durable event to a separate topic for downstream consumers.

- `asset.cataloged` — carries `AssetCatalogedEvent{assetId, folderPath, timestamp}` with 7-day retention.
- `asset.deleted` — carries `AssetDeletedEvent{assetId, folderPath, timestamp, permanent}` with 7-day retention.

These topics are independent of `job.catalog.progress` and are retained for future consumers (audit log, search indexer).

#### Scenario: New asset during catalog publishes to asset.cataloged
- **WHEN** a new asset is persisted during a catalog run
- **THEN** an `AssetCatalogedEvent{assetId, folderPath, timestamp}` is published to `asset.cataloged`

#### Scenario: Deleted asset during catalog publishes to asset.deleted
- **WHEN** an asset is removed during stale-asset cleanup in `CatalogAssetItemWriter.afterStep()`
- **THEN** an `AssetDeletedEvent{assetId, folderPath, timestamp, permanent=false}` is published to `asset.deleted`

---

### Requirement: KafkaProgressListener fans out events to local SSE clients

Each application instance SHALL run a `@KafkaListener` (consumer group `sse-broadcaster-{instance}`, where `{instance}` is a per-JVM-unique suffix resolved from `spring.kafka.consumer.group-id`) subscribed to `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`. The `@KafkaListener` annotations SHALL NOT hardcode a `groupId` attribute; the group id SHALL be inherited from `spring.kafka.consumer.group-id` so that it resolves independently for each instance. On receiving a message the listener SHALL look up the `SseEmitter` registered locally for that `runId` and write the event to it.

Because each instance uses its own single-member consumer group, Kafka SHALL assign every partition of every subscribed topic to that instance, so every instance receives every message regardless of how many instances are running.

If no local emitter is registered for the `runId` (the SSE connection is on a different instance), the message SHALL be silently skipped.

When a message with `done=true` is received, the listener SHALL call `emitter.complete()` and remove the `runId` from the local registry.

#### Scenario: Progress event routed to local SSE client
- **WHEN** a `CatalogProgressMessage{runId=42, done=false}` arrives on a Kafka partition
- **AND** this instance has an `SseEmitter` registered for `runId=42`
- **THEN** the listener sends a `catalog` SSE event containing the `notification` to that emitter

#### Scenario: Done event completes the SSE stream
- **WHEN** a `CatalogProgressMessage{runId=42, done=true}` arrives
- **AND** this instance has an `SseEmitter` registered for `runId=42`
- **THEN** the listener calls `emitter.complete()` and removes `runId=42` from `KafkaProgressRegistry`

#### Scenario: Message for unknown runId is skipped
- **WHEN** a progress message for `runId=99` arrives
- **AND** this instance has no emitter registered for `runId=99`
- **THEN** the message is skipped with no error; the emitter on the other instance receives the event normally

#### Scenario: Two instances both receive the same single-partition progress message
- **WHEN** two application instances are running, each configured with its own unique consumer group id
- **AND** a `SyncProgressMessage{runId=7, done=false}` is published to `job.sync.progress` (1 partition)
- **THEN** both instances' `KafkaProgressListener.onSyncProgress` methods are invoked with that message
- **AND** the instance holding the `SseEmitter` for `runId=7` forwards the event to the browser, while the other instance finds no local emitter and skips it silently

#### Scenario: Catalog partitions are all assigned to a single-member group
- **WHEN** one application instance subscribes to `job.catalog.progress` (3 partitions) using a consumer group in which it is the only member
- **THEN** Kafka assigns all 3 partitions to that instance, so it receives progress messages regardless of which partition a given `runId`'s messages hash onto

---

### Requirement: Controllers register SseEmitter before launching job

`AssetController.catalogAssets()`, `SyncController.run()`, and `ConvertController.run()` SHALL each:
1. Create a `SseEmitter`.
2. Generate a `runId = System.currentTimeMillis()`.
3. Register `(runId, emitter)` in `KafkaProgressRegistry`.
4. Call the respective use-case `execute(runId)` (non-blocking; do not call `CompletableFuture.get()` on the HTTP thread).
5. Return the emitter immediately.

#### Scenario: Catalog SSE endpoint returns emitter without blocking
- **WHEN** `GET /api/assets/catalog` is called
- **THEN** the HTTP thread registers the emitter and returns it immediately; the response stream stays open while Kafka delivers progress events

#### Scenario: Sync SSE endpoint follows the same non-blocking pattern
- **WHEN** `GET /api/sync/run` is called
- **THEN** the HTTP thread registers the emitter and returns it immediately; sync results arrive as the final SSE event when `done=true` is received

---

### Requirement: SseNotificationRegistry removed

The `SseNotificationRegistry` class SHALL be deleted. No class in the codebase SHALL reference or depend on it after this change.

#### Scenario: Build succeeds without SseNotificationRegistry
- **WHEN** the backend is compiled after this change
- **THEN** no compilation errors or unresolved references to `SseNotificationRegistry` exist

---

### Requirement: Kafka topic configuration

The following topics SHALL be created automatically on application startup (via `@Bean KafkaAdmin` topic declarations):

| Topic | Partitions | Retention |
|---|---|---|
| `job.catalog.progress` | 3 | 1 hour |
| `job.sync.progress` | 1 | 1 hour |
| `job.convert.progress` | 1 | 1 hour |
| `asset.cataloged` | 3 | 7 days |
| `asset.deleted` | 3 | 7 days |

#### Scenario: Topics exist on startup
- **WHEN** the application starts against a fresh Kafka cluster
- **THEN** all five topics listed above are created with the specified partition counts and retention policies

---

### Requirement: Kafka service in docker-compose.yml

`docker-compose.yml` SHALL include a `kafka` service using `apache/kafka:3.9.0` in KRaft mode on port 9092 with no ZooKeeper dependency.

#### Scenario: Kafka container starts with docker compose up
- **WHEN** `docker compose up` is run
- **THEN** the `kafka` container starts successfully and is reachable at `localhost:9092`

---

### Requirement: Kafka configuration in application.yml

`application.yml` SHALL define `spring.kafka.bootstrap-servers` defaulting to `localhost:9092` with environment-variable override (`${KAFKA_BOOTSTRAP:localhost:9092}`).

`application.yml` SHALL define `spring.kafka.consumer.group-id` as `sse-broadcaster-${HOSTNAME:${random.uuid}}`, so that each application instance resolves a distinct, single-member consumer group: the `HOSTNAME` environment variable when present (e.g. the container id in Docker/Kubernetes deployments), falling back to a randomly generated UUID otherwise.

`application-test.yml` SHALL override `spring.kafka.bootstrap-servers` to the `@EmbeddedKafka` broker address, and SHALL apply the same per-instance `spring.kafka.consumer.group-id` pattern, so unit and integration tests require no external Kafka and exercise the same group-id resolution path as production.

#### Scenario: Application connects to Kafka on startup
- **WHEN** the application starts with a running Kafka broker at `KAFKA_BOOTSTRAP`
- **THEN** the producer and consumer connect successfully and no Kafka connection errors appear in the logs

#### Scenario: Each instance resolves a distinct consumer group id
- **WHEN** two application instances start in a containerized environment where `HOSTNAME` is set to a distinct value per container
- **THEN** each instance's `spring.kafka.consumer.group-id` resolves to `sse-broadcaster-{that container's HOSTNAME}`, distinct from the other instance's group id

#### Scenario: Group id falls back to a random UUID when HOSTNAME is unset
- **WHEN** the application starts in an environment where the `HOSTNAME` environment variable is not set
- **THEN** `spring.kafka.consumer.group-id` resolves to `sse-broadcaster-{a randomly generated UUID}`
