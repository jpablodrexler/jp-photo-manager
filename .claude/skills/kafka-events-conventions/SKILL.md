---
name: kafka-events-conventions
description: >
  Kafka event/messaging conventions for the JPPhotoManager Spring Boot
  backend. TRIGGER whenever work adds or changes a `@KafkaListener`, a
  `kafkaTemplate.send(...)` call, a new event/progress-message DTO in
  `application/dto/`, or touches `infrastructure/kafka/**` or
  `UploadProcessorKafkaConfig` — including when implementing OpenSpec tasks
  that need async processing or cross-instance notification. Do not wait to
  be asked: apply these conventions proactively whenever new Kafka-backed
  code is written. Encodes topic naming, the consumer-group decision (shared
  vs. per-instance), retry/failure handling, and message-key/ordering rules.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Kafka Events Conventions Skill

This backend uses Kafka for two distinct purposes. Identify which one a new
listener or producer belongs to before writing it — they have opposite
consumer-group requirements (§2).

| Purpose | Topic shape | Example topics |
|---|---|---|
| Domain lifecycle events — something happened, multiple independent consumers may care | `<entity>.<verb>` | `asset.cataloged`, `asset.deleted`, `asset.uploaded` |
| Job/run progress streams — feed an `SseEmitter` for a specific run or asset | `job.<jobtype>.progress` | `job.catalog.progress`, `job.sync.progress`, `job.convert.progress`, `job.upload.progress` |

New topics should fit one of these two shapes. A topic that's neither a
past-tense domain event nor a `job.*.progress` stream is a signal to stop and
check whether the use case actually needs Kafka, or a direct synchronous call
would do.

---

## 1. Producing an Event

Inject `KafkaTemplate<String, Object>` (the project-wide default; don't
declare a narrower generic type) and send with the run/entity ID as the
**message key**, never null:

```java
kafkaTemplate.send("job.sync.progress", String.valueOf(runId), progressMessage);
```

🔴 Flag a `kafkaTemplate.send(...)` call with a `null` or constant key for an
event that's scoped to a specific run/asset — Kafka only guarantees ordering
*within a partition*, and the default partitioner assigns by key hash. A
missing or constant key means events for the same run can be processed
out of order by different partitions/consumers, which breaks any listener
(like `KafkaProgressListener`) that assumes progress messages for one
`runId`/`assetId` arrive in order.

🟡 Flag a new progress-message DTO that doesn't carry a `done()`-style
boolean (or equivalent terminal marker) if consumers need to distinguish an
intermediate update from the final message — every existing progress DTO
(`CatalogProgressMessage`, `SyncProgressMessage`, `ConvertProgressMessage`,
`UploadProgressMessage`) does this, and both `KafkaProgressListener` (SSE
forwarding) and `AuditLogKafkaListener` (only logs on `done()`) depend on it.

---

## 2. The Consumer-Group Decision

This is the single most consequential choice when adding a `@KafkaListener` —
getting it backwards either silently drops events on some instances or
duplicates work N times across instances.

**Option A — every instance must process every event (per-instance group):**
Omit `groupId` on `@KafkaListener` so it inherits the application default,
configured in `application.yml`:

```yaml
group-id: sse-broadcaster-${HOSTNAME:${random.uuid}}
```

This resolves to a *different* group per running instance, so Kafka treats
each instance as its own independent consumer group — every instance gets its
own copy of every message. Use this shape only when the listener's side
effect is instance-local and every instance needs to perform it — the sole
existing example is `KafkaProgressListener`, which forwards progress messages
to `SseEmitter`s held in that instance's own in-memory registry; a client
connected to instance A would never see progress if only instance B (a
different consumer-group member under a *shared* group) happened to receive
the message.

**Option B — exactly one instance processes each event (shared, named
group):** Declare an explicit, stable `groupId` on `@KafkaListener`:

```java
private static final String CONSUMER_GROUP = "asset-search-cache-invalidator";

@KafkaListener(topics = "asset.cataloged", groupId = CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory")
```

Kafka's consumer-group partition assignment guarantees exactly one member of
a shared group processes each message. Use this for anything where doing the
work N times (once per running instance) would be wrong or wasteful:
persisting to a database, evicting a shared cache, mutating shared state.
Existing examples: `asset-search-cache-invalidator`
(`AssetSearchCacheInvalidationListener`), `audit-log-writer`
(`AuditLogKafkaListener`).

🔴 Flag any new `@KafkaListener` whose side effect is a database write, a
shared-cache eviction, or any other action that must happen exactly once
across the fleet, but which omits `groupId` — it will inherit the per-instance
default group and run once **per running instance**, e.g. writing N duplicate
audit-log entries for a single event in an N-instance deployment.

🟡 Flag a new `@KafkaListener` whose side effect is instance-local (updating
in-memory state only that instance holds) but which sets an explicit shared
`groupId` — only one instance will receive the event, silently breaking the
feature on every other instance.

🟡 Flag a new shared consumer group name that doesn't describe what it does
in `kebab-case` ending in a role noun, matching `asset-search-cache-invalidator`
and `audit-log-writer` (not `group1`, not the topic name repeated).

---

## 3. Failure Handling: Retry-and-Fail vs. Best-Effort

The project applies two different failure-handling shapes depending on
whether the listener's work is on the critical path for user-visible state.

**Must-complete work (retry with bounded backoff, then mark failed):** the
upload-processing pipeline (`AssetHashProcessor`, `AssetExifProcessor`,
`AssetThumbnailProcessor`, each with its own container factory in
`UploadProcessorKafkaConfig`) uses a `DefaultErrorHandler` with
`FixedBackOff(2000ms, 3 retries)`. When retries are exhausted, the recoverer
marks the asset `ProcessingStatus.FAILED` and publishes a failed
`job.upload.progress` message so the SSE observer surfaces the failure
instead of hanging — the asset can later be re-triggered via
`POST /api/assets/{id}/reprocess`. Follow this shape for any new
must-complete async stage: a dedicated container factory, a bounded
`FixedBackOff`, and an explicit terminal-failure state the user can see and
recover from.

**Best-effort work (catch, log `WARN`, move on):** `AuditLogKafkaListener`
wraps each write in try/catch and logs a `WARN` on failure rather than
letting the exception trigger Kafka's default retry/redelivery — an audit
log entry is not worth retry-storming or blocking the consumer over. Follow
this shape for logging/telemetry-style consumers where losing an individual
event is acceptable and retrying risks head-of-line blocking the rest of the
topic.

🔴 Flag a new must-complete listener (one where losing the event means a
user-visible operation silently never finishes) that has no retry policy and
no terminal-failure signal — it will fail once, log an exception, and leave
the operation stuck with no way for the user to know or retry.

🟡 Flag a new best-effort listener that lets an exception propagate instead
of catching and logging — an unhandled exception triggers the shared
`kafkaListenerContainerFactory`'s default error handling (retry then
potentially stop the container), which is the wrong failure mode for
"nice to have, not critical" work.

---

## 4. Consumer Idempotency

Kafka can redeliver a message on consumer-group rebalance or a restart before
an offset commits. A new listener's handler should tolerate processing the
same event twice without incorrect side effects.

🟡 Flag a new listener whose handler is not naturally idempotent (e.g.
incrementing a counter, appending without a dedup key) and has no explicit
dedup strategy. Cache-eviction listeners are naturally idempotent (evicting
an already-evicted key is a no-op) — that's not a coincidence, it's why
event-driven eviction (§ redis-caching-conventions §3) is a safe default for
new cache-invalidation triggers. A new listener that persists new rows
(rather than upserting/evicting) needs a real answer to "what happens if this
fires twice for the same event," even if the answer is "acceptable, here's
why."

---

## 5. Topic & DTO Placement

- Event/message DTOs live in `application/dto/` as records (matching
  `AssetCatalogedEvent`, `AssetDeletedEvent`, `AssetUploadedEvent`,
  `CatalogProgressMessage`, `SyncProgressMessage`, `ConvertProgressMessage`,
  `UploadProgressMessage`) — framework-free, no Kafka/Spring imports.
- Listener classes live in `infrastructure/kafka/` as `@Component` (not
  `@Service` — these are inbound adapters, matching the project's
  `infrastructure/` placement for adapters generally).
- Producing code (a use case calling `kafkaTemplate.send(...)`) stays in
  `application/usecase/` alongside the rest of that use case's logic — don't
  extract a separate "publisher" class unless multiple use cases need to
  publish the same event shape.

🟡 Flag a new event/message DTO placed outside `application/dto/`, or a new
listener placed outside `infrastructure/kafka/`.

---

## Code Style Rules

- Every `kafkaTemplate.send(...)` for a run/entity-scoped event uses that
  ID (as a `String`) for the message key — never `null`, never a constant (§1).
- Decide the consumer-group shape deliberately for every new
  `@KafkaListener` — per-instance default (state is instance-local) or an
  explicit, named shared group (state is shared/must-process-once) (§2).
- Must-complete async stages get a dedicated container factory with bounded
  retry and a terminal-failure signal; best-effort consumers catch and log
  `WARN` instead of retrying (§3).
- New topics are named `<entity>.<verb>` (domain events) or
  `job.<jobtype>.progress` (progress streams) (intro table).
- New listener handlers are idempotent, or the PR/task explains why
  duplicate delivery is acceptable (§4).
