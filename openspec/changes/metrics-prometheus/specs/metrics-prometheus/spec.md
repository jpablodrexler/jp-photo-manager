# metrics-prometheus

Application metrics are exposed at `/actuator/prometheus` in Prometheus text format. A local Prometheus + Grafana stack is available via `docker-compose.yml` for development monitoring.

---

## ADDED Requirements

### Requirement: Prometheus metrics endpoint is exposed

`GET /actuator/prometheus` SHALL return application metrics in Prometheus text exposition format without requiring authentication.

#### Scenario: Metrics endpoint is accessible without authentication

- **GIVEN** the backend is running
- **WHEN** `GET /actuator/prometheus` is called without an Authorization header
- **THEN** the response is `200 OK` with `Content-Type: text/plain; version=0.0.4` and Prometheus-formatted metric lines

### Requirement: Catalog counter is incremented per cataloged asset

`photomanager_catalog_assets_total` SHALL be incremented once for each asset successfully cataloged.

#### Scenario: Counter reflects total cataloged assets

- **GIVEN** the backend has cataloged 150 assets since startup
- **WHEN** `GET /actuator/prometheus` is called
- **THEN** the response includes `photomanager_catalog_assets_total 150.0`

### Requirement: Thumbnail generation is instrumented with a Timer

`photomanager_thumbnail_generation_seconds` SHALL record the latency of each `generateThumbnail()` invocation.

#### Scenario: Timer exposes count and sum

- **GIVEN** thumbnail generation has completed 50 times
- **WHEN** `GET /actuator/prometheus` is called
- **THEN** the response includes `photomanager_thumbnail_generation_seconds_count 50.0` and `photomanager_thumbnail_generation_seconds_sum <total seconds>`

### Requirement: Active SSE connections are tracked with a Gauge

`photomanager_active_sse_connections` SHALL reflect the number of currently open SSE connections.

#### Scenario: Gauge increases when SSE connection opens

- **GIVEN** no SSE connections are active
- **WHEN** a client connects to a SSE endpoint
- **THEN** `photomanager_active_sse_connections` becomes `1.0`

#### Scenario: Gauge decreases when SSE connection closes

- **GIVEN** one SSE connection is active
- **WHEN** the connection completes or times out
- **THEN** `photomanager_active_sse_connections` returns to `0.0`
