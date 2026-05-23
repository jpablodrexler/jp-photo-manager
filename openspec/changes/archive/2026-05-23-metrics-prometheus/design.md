## Context

Micrometer is the metrics facade already bundled with Spring Boot. Adding `micrometer-registry-prometheus` on the classpath automatically enables the Prometheus text endpoint at `/actuator/prometheus` (when actuator is present). Custom metrics are registered via `MeterRegistry` injection.

## Goals / Non-Goals

**Goals:**
- Expose `/actuator/prometheus` with default JVM, HTTP, and Spring MVC metrics
- `photomanager_catalog_assets_total` Counter incremented once per successfully cataloged asset
- `photomanager_thumbnail_generation_seconds` Timer wrapping `StorageServiceImpl.generateThumbnail()`
- `photomanager_active_sse_connections` Gauge tracking the count of live `SseEmitter` connections
- `prometheus` and `grafana` services added to `docker-compose.yml`; Prometheus configured to scrape `backend:8080/actuator/prometheus` every 15s

**Non-Goals:**
- Custom Grafana dashboard JSON (out of scope; operators configure their own dashboards)
- Distributed tracing (a separate concern)
- Alerting rules in Prometheus

## Decisions

### 1. Inject `MeterRegistry` in service classes

**Decision:** Inject `MeterRegistry` into `CatalogAssetsUseCaseImpl` and `StorageServiceImpl` via constructor injection. Register counters/timers in the constructor using `Counter.builder()` and `Timer.builder()`.

**Rationale:** Constructor-registered metrics are available immediately; avoids `@PostConstruct` timing issues.

### 2. Gauge via `AtomicInteger`

**Decision:** Maintain an `AtomicInteger sseConnectionCount` in `AssetController`. Increment on `SseEmitter` creation, decrement in the completion/timeout/error callbacks. Register as `Gauge.builder("photomanager_active_sse_connections", sseConnectionCount, AtomicInteger::get)`.

**Rationale:** Gauges require a function that reads the current value. `AtomicInteger` is thread-safe and directly usable.

### 3. `/actuator/prometheus` permitted without authentication

**Decision:** Add `/actuator/prometheus` to the `permitAll()` list in `SecurityConfig` (same as `/actuator/health`).

**Rationale:** Prometheus scrapes run on the internal network; requiring authentication for scraping adds operational complexity with minimal security benefit.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Prometheus cardinality explosion from dynamic labels | Low | Use only static labels (no per-asset or per-user dimensions) |
| Docker Compose services conflict with user's existing Prometheus | Low | Document that the compose stack is optional for local development |
