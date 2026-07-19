## MODIFIED Requirements

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
