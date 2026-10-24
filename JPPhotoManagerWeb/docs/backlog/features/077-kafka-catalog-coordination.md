# Feature 77 — kafka-catalog-coordination

**As a** administrator, **I want** only one instance to run a scheduled catalog scan, **so that** multiple instances do not double the disk I/O or write duplicates.

## Acceptance Criteria
- AC1: Given several instances, when the schedule interval elapses, then one catalog request is published to a single-partition topic.
- AC2: Given the request, when the consumer group receives it, then only one member processes it and the others skip.
- AC3: Given the scheduler, when it triggers, then it publishes the request instead of scanning directly.

## Technical notes

Prevent duplicate concurrent catalog scans when multiple app instances are deployed; `CatalogScheduler` currently uses `@Scheduled(fixedDelay)` on every JVM — two instances each trigger a full directory traversal simultaneously, doubling disk I/O and risking duplicate database writes; a single-partition `catalog.requests` Kafka topic provides natural leader election via Kafka consumer groups: only one member processes a `CatalogJobRequested` event while others skip; replace the `@Scheduled` trigger in `CatalogScheduler` with a Kafka producer that publishes to `catalog.requests` on the same interval; requires the Kafka infrastructure introduced by `kafka-catalog-pipeline` (#75)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
