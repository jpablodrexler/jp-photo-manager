# Feature 42 — metrics-prometheus

**As a** administrator, **I want** Prometheus metrics for cataloging, thumbnails and live connections, with a dashboard, **so that** I can watch how the application behaves over time.

## Acceptance Criteria
- AC1: Given /actuator/prometheus, when it is requested without authentication, then it is accessible.
- AC2: Given cataloging, when assets are cataloged, then a counter reflects the total and a timer exposes count and sum per folder.
- AC3: Given thumbnails, when they are generated, then a timer records their duration.
- AC4: Given SSE connections, when one opens or closes, then the gauge increases or decreases.
- AC5: Given Docker Compose, when it starts, then Prometheus and Grafana run with a pre-built dashboard.

## Technical notes

Add `micrometer-registry-prometheus` to `pom.xml` and expose `/actuator/prometheus`; instrument three application-specific metrics not covered by Spring Boot's default JVM/HTTP metrics: a `Timer` on catalog duration per folder, a `Timer` on thumbnail generation, and a `Gauge` tracking active SSE connections (catalog, sync, convert); add Prometheus and Grafana services to `docker-compose.yml` with a pre-built dashboard

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
