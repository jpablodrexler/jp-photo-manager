## Context

Spring Boot Actuator provides `/actuator/health` out of the box with a `DataSource` health check when `spring-boot-starter-actuator` is on the classpath. Custom `HealthIndicator` beans are auto-detected and merged into the aggregate health response. The thumbnail directory path is already available via `@Value("${photomanager.thumbnails-directory}")`.

## Goals / Non-Goals

**Goals:**
- Expose `/actuator/health` with `show-details: always` (details visible without authentication)
- `DatabaseHealthIndicator` executes `SELECT 1` and reports UP/DOWN
- `ThumbnailStorageHealthIndicator` checks `Files.isWritable(thumbnailsDir)` and reports UP/DOWN
- `GeocodingHealthIndicator` sends a HEAD request to the geocoding base URL; reports UP/DEGRADED (not DOWN, since geocoding is optional)
- `/actuator/health` is permitted in `SecurityConfig` without JWT

**Non-Goals:**
- Exposing other actuator endpoints (info, metrics, env) — see `metrics-prometheus` improvement
- Authentication on health details (kept public for monitoring tools)
- Kubernetes liveness/readiness probe split (single health endpoint is sufficient)

## Decisions

### 1. `show-details: always`

**Decision:** Set `management.endpoint.health.show-details: always` so monitoring tools can read sub-indicator details without logging in.

**Rationale:** Health details contain no sensitive data (no credentials, no PII). Hiding them from unauthenticated callers adds no security benefit for a personal photo manager.

### 2. Geocoding indicator returns DEGRADED, not DOWN

**Decision:** A geocoding failure sets the aggregate status to `DEGRADED` (custom status) rather than DOWN.

**Rationale:** The geocoding service is optional — the app functions fully without it. Returning DOWN would cause monitoring alerts on every geocoding outage, which is too noisy.

### 3. `HealthIndicator` beans, not `@Health`-annotated methods

**Decision:** Implement `org.springframework.boot.actuate.health.HealthIndicator` directly rather than using `@Health` (Resilience4j annotation).

**Rationale:** Direct `HealthIndicator` implementation is the standard Spring Boot pattern and does not introduce an additional framework dependency.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| `SELECT 1` adds minor DB load | Low | Executed only on health check calls, not on every request |
| Geocoding HEAD request adds latency to health check | Low | Use a short timeout (2s); catch `IOException` and report DEGRADED |
