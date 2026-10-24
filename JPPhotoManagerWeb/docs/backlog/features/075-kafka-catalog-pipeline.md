# Feature 75 — kafka-catalog-pipeline

**As a** administrator, **I want** progress events broadcast through Kafka instead of memory, **so that** the application can run as several instances behind a load balancer.

## Acceptance Criteria
- AC1: Given a catalog run, when assets are written, then progress messages are published; completion publishes a done message; creating a folder also publishes progress.
- AC2: Given new and deleted assets, when a catalog run handles them, then they are published to asset.cataloged and asset.deleted.
- AC3: Given sync and convert, when they progress or complete, then their progress and results are published.
- AC4: Given a progress event, when an instance holds the SSE client, then it is routed to it; a done event completes the stream; a message for an unknown run id is skipped.
- AC5: Given the SSE endpoints, when called, then they return the emitter without blocking.
- AC6: Given the build and startup, when they run, then there is no in-memory registry class, the topics exist, Kafka starts with docker compose up and the application connects.

## Technical notes

Replace the in-memory `SseNotificationRegistry` (a `ConcurrentHashMap` in a single JVM) with Kafka topics so catalog, sync, and convert progress events are broadcast across all running app instances; topics: `asset.cataloged` (writer: `CatalogAssetItemWriter`), `asset.deleted` (writer: `DeleteAssetsUseCaseImpl`), `job.catalog.progress` (writer: `CatalogItemWriteListener`), `job.sync.progress` (writer: `SyncAssetsUseCaseImpl`), `job.convert.progress` (writer: `ConvertAssetsUseCaseImpl`); SSE controllers on each instance subscribe via a Kafka consumer and fan-out progress events to connected `SseEmitter` clients; the `SseNotificationRegistry` class is deleted; eliminates the single-JVM constraint that currently prevents horizontal scaling; **prerequisite for `kafka-async-upload` (#76), `kafka-catalog-coordination` (#77), `kafka-sse-broadcast` (#80), and `mongodb-audit-log` (#73)**

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
