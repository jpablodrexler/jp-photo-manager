## Why

The backend emits no application-level metrics. Operators cannot observe request latency, catalog throughput, or cache hit rates. Adding Micrometer with a Prometheus registry exposes these metrics at `/actuator/prometheus`, enabling Prometheus scraping and Grafana dashboards.

## What Changes

- Add `micrometer-registry-prometheus` to the backend
- Expose `/actuator/prometheus` endpoint
- Add three custom metrics:
  1. `photomanager_catalog_assets_total` (Counter) — total assets cataloged since startup
  2. `photomanager_thumbnail_generation_seconds` (Timer) — latency of `StoragePort.generateThumbnail()`
  3. `photomanager_active_sse_connections` (Gauge) — number of currently active SSE connections
- Add a `prometheus` and `grafana` service to `docker-compose.yml` for local monitoring

## Capabilities

### New Capabilities

- `metrics-prometheus`: Application metrics are exposed at `/actuator/prometheus` in Prometheus text format. A local Prometheus + Grafana stack is available via `docker-compose.yml` for development monitoring.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `micrometer-registry-prometheus`
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — expose `prometheus` actuator endpoint
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/StorageServiceImpl.java` — wrap `generateThumbnail()` with a `Timer`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — track SSE connection count with a `Gauge`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CatalogAssetsUseCaseImpl.java` — increment catalog counter per asset
- `JPPhotoManagerWeb/docker-compose.yml` — add `prometheus` and `grafana` services
- `JPPhotoManagerWeb/prometheus.yml` — Prometheus scrape configuration
