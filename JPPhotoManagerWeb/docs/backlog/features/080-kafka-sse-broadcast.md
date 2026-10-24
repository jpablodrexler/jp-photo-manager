# Feature 80 — kafka-sse-broadcast

**As a** administrator, **I want** every instance to receive every progress message, **so that** the browser's progress stream never silently loses events when several instances run.

## Acceptance Criteria
- AC1: Given two instances, when a single-partition message is published, then both receive it.
- AC2: Given the catalog topic with several partitions, when an instance subscribes, then all partitions are assigned to it.
- AC3: Given each instance, when it starts, then it resolves a distinct consumer group id, falling back to a random UUID when HOSTNAME is unset.
- AC4: Given a progress event, when an instance has no local client, then it is skipped; with one, it is forwarded and a done event completes the stream.
- AC5: Given startup, when the application runs, then it connects to Kafka.

## Technical notes

Fix multi-instance SSE delivery without adding Redis: today every instance shares one hardcoded Kafka consumer group (`sse-broadcaster`), so `job.sync.progress`/`job.convert.progress` (1 partition each) are only ever consumed by a single instance cluster-wide, and `job.catalog.progress` (3 partitions) events can land on an instance other than the one holding the browser's `SseEmitter` — both cases silently drop progress events (see `KafkaProgressRegistry`/`KafkaProgressListener`, introduced by #75); give each instance its own consumer group instead (`spring.kafka.consumer.group-id: sse-broadcaster-${HOSTNAME:${random.uuid}}`, and drop the hardcoded `groupId` attribute from the three `@KafkaListener` methods); a consumer group with a single member is always assigned every partition of its topic, so every instance now receives every progress message and forwards it to any locally-connected `SseEmitter`/observer; reuses the existing `KafkaProgressRegistry` local-lookup-and-skip-if-absent logic unchanged; no new infrastructure, dependency, topic, or schema change — only the group-id property and the three annotations change; requires `kafka-catalog-pipeline` (#75)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
