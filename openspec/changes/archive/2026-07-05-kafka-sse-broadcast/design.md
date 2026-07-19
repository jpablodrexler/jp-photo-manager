## Context

`kafka-catalog-pipeline` (#75) introduced Kafka-backed progress delivery: use cases publish `CatalogProgressMessage` / `SyncProgressMessage` / `ConvertProgressMessage` events, and `KafkaProgressListener` (a `@KafkaListener` per topic) consumes them and forwards each event to a locally-registered `SseEmitter` looked up in `KafkaProgressRegistry` by `runId`. This works correctly on a single instance because that one process is also the sole Kafka consumer.

The listener methods are pinned to a hardcoded consumer group, `groupId = "sse-broadcaster"`, and `application.yml` sets the same literal via `spring.kafka.consumer.group-id: sse-broadcaster`. Kafka guarantees that within one consumer group, each partition is assigned to exactly one member. When a second application instance is started (e.g. behind a load balancer for horizontal scaling), both instances join the *same* group:

- `job.sync.progress` and `job.convert.progress` have 1 partition each → only one of the two instances is ever assigned that partition. If a user's SSE connection lands on the instance that Kafka does *not* route the partition to, that user's progress events are never delivered — the browser's `EventSource` sits open with no events and the request appears to hang.
- `job.catalog.progress` has 3 partitions → they are split across the two instances. Whichever partition a given `runId`'s messages hash onto may or may not be assigned to the instance holding that `runId`'s emitter, so roughly half of concurrent catalog runs would silently lose their progress stream in a two-instance deployment.

The existing `KafkaProgressRegistry` lookup ("get the local emitter for `runId`; do nothing if absent") was written anticipating multi-instance delivery — the fix that was missing is making sure every instance actually *receives* every message in the first place, so each instance can check its own local registry and act only when relevant.

## Goals / Non-Goals

**Goals:**
- Every application instance receives every message published to `job.catalog.progress`, `job.sync.progress`, and `job.convert.progress`, regardless of how many instances are running.
- No change to message ordering guarantees, topic/partition/retention configuration, or the existing local-registry-lookup-and-skip-if-absent behavior in `KafkaProgressListener`.
- No new infrastructure dependency (no Redis Pub/Sub, no sticky load-balancer session affinity).

**Non-Goals:**
- Redis Pub/Sub-based SSE fan-out (considered and explicitly rejected for this change — see Decisions).
- Changing how `asset.cataloged` / `asset.deleted` durable events are consumed; those already use dedicated consumer groups for downstream systems (audit log, search indexer) and are unaffected by this change.
- Deduplicating processing work across instances (e.g. `KafkaProgressRegistry.getCatalogObservers()` broadcast paths) — every instance already independently forwards to its own local observers; this is unchanged.

## Decisions

**Decision: give each instance its own single-member Kafka consumer group, instead of introducing Redis Pub/Sub.**

Kafka's consumer-group protocol guarantees that a consumer group with exactly one member is assigned *all* partitions of every topic it subscribes to. If every application instance uses a unique group id, each instance independently receives every message on every partition — which is exactly the "every instance receives every progress message" property this change needs, with no new moving parts.

Alternative considered: dual-publish to a Redis Pub/Sub channel in addition to Kafka (the architecture sketched during the original #80 proposal, before it was redefined — see improvements.md history) and have `RedisMessageListenerContainer` fan out to local emitters instead of `@KafkaListener`. Rejected for this change because it requires provisioning a new piece of infrastructure (Redis) purely to fix a consumer-group misconfiguration that has a one-line fix within the technology already in use. Redis Pub/Sub remains a reasonable future direction for `redis-thumbnail-cache` (#81) / `redis-search-tag-cache` (#82) style workloads, but is unnecessary overhead for this bug fix.

**Decision: `spring.kafka.consumer.group-id: sse-broadcaster-${HOSTNAME:${random.uuid}}` in `application.yml`, and drop the `groupId` attribute from the three `@KafkaListener` annotations.**

Spring's property placeholder resolution supports nested defaults: `${HOSTNAME:${random.uuid}}` resolves to the `HOSTNAME` environment variable when set (the normal case in a container orchestrator like Docker/Kubernetes, where each container gets a distinct hostname) and falls back to a random UUID otherwise (e.g. local development, or environments where `HOSTNAME` is not propagated into the JVM). Either way, each JVM process ends up with a distinct group id.

The `groupId` attribute on `@KafkaListener` takes precedence over `spring.kafka.consumer.group-id` when both are set. Because the three listener methods currently hardcode `groupId = "sse-broadcaster"`, simply changing the YAML property alone would have no effect — the annotation attribute would keep overriding it back to the old shared literal. Removing the attribute lets the listener container factory fall through to the Spring Boot-managed consumer group id, which now resolves per-instance.

**Decision: keep the group-id prefix `sse-broadcaster-` rather than switching to something unrelated.**

Preserves operational continuity: log lines, Kafka consumer-group listings (`kafka-consumer-groups.sh --list`), and any existing monitoring dashboards that grep for `sse-broadcaster` still match, just with a per-instance suffix appended.

## Risks / Trade-offs

**[Risk] New instances joining with a fresh, never-before-seen consumer group id have no committed offset, so `auto-offset-reset: latest` (already configured) means they only see messages published *after* they start consuming.** → Mitigation: this is actually the correct behavior for this use case. Progress events are meaningful only to a live, currently-open SSE connection; there is no scenario where a newly-started instance needs to replay historical progress messages from before it existed. This matches the existing single-instance behavior (a restarted instance today also starts fresh with `latest`).

**[Risk] Consumer-group churn: because the group id is unique per instance and never reused, Kafka accumulates orphaned/empty consumer groups whenever an instance is stopped and never restarted with the same id (e.g. rolling deployments, autoscaling scale-down).** → Mitigation: Kafka automatically expires empty consumer groups after `offsets.retention.minutes` (broker default 7 days) with no operator action required; this is a well-understood, self-cleaning pattern for ephemeral per-instance groups and requires no additional code.

**[Risk] Each instance now performs a full poll-and-decode pass over every partition of every progress topic, instead of splitting the work across instances.** → Mitigation: these three topics carry only short-lived, 1-hour-retention progress notifications for in-flight catalog/sync/convert runs — message volume is proportional to active user operations, not to catalog size. The per-partition workload increase from single-member consumption is negligible compared to the cost of a silently-hung SSE stream.

## Migration Plan

1. Update `application.yml`: change `spring.kafka.consumer.group-id` from `sse-broadcaster` to `sse-broadcaster-${HOSTNAME:${random.uuid}}`.
2. Update `application-test.yml` the same way, so `@EmbeddedKafka` integration tests exercise the same property resolution path (a single JVM test process still ends up as the sole member of its own group, so existing tests keep passing unchanged).
3. Remove `groupId = "sse-broadcaster"` from the three `@KafkaListener` annotations in `KafkaProgressListener` (`onCatalogProgress`, `onSyncProgress`, `onConvertProgress`), leaving `containerFactory = "kafkaListenerContainerFactory"` in place.
4. No Flyway migration, no new Docker service, no dependency bump.
5. **Rollback:** revert the two YAML changes and re-add the `groupId` attribute to the three listener methods; this restores the exact pre-change behavior (shared group, single-instance-effective delivery) with no data loss, since no persisted state depends on the group id.

## Open Questions

None — this is a self-contained configuration fix scoped entirely to `KafkaProgressListener` and the two `application*.yml` files.
