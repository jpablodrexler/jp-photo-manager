## 1. Dependency and configuration

- [x] 1.1 Add `micrometer-registry-prometheus` to `pom.xml`
- [x] 1.2 Add to `application.yml`:
  - `management.endpoints.web.exposure.include: health,prometheus`
  - `management.metrics.export.prometheus.enabled: true`
- [x] 1.3 Add `/actuator/prometheus` to `permitAll()` in `SecurityConfig`

## 2. Catalog counter

- [x] 2.1 Inject `MeterRegistry` into `CatalogAssetsUseCaseImpl`
- [x] 2.2 Register `Counter.builder("photomanager_catalog_assets_total").description("Total assets cataloged").register(meterRegistry)`
- [x] 2.3 Call `counter.increment()` after each asset is successfully persisted

## 3. Thumbnail generation timer

- [x] 3.1 Inject `MeterRegistry` into `StorageServiceImpl`
- [x] 3.2 Register `Timer.builder("photomanager_thumbnail_generation_seconds").description("Thumbnail generation latency").register(meterRegistry)`
- [x] 3.3 Wrap `generateThumbnail()` body with `timer.record(() -> { ... })`

## 4. Active SSE connections gauge

- [x] 4.1 Add `AtomicInteger sseConnectionCount = new AtomicInteger(0)` to `AssetController`
- [x] 4.2 Register `Gauge.builder("photomanager_active_sse_connections", sseConnectionCount, AtomicInteger::get).description("Active SSE connections").register(meterRegistry)` in `AssetController` constructor
- [x] 4.3 Increment `sseConnectionCount` when creating an `SseEmitter`; decrement in `onCompletion`, `onTimeout`, and `onError` callbacks

## 5. Docker Compose monitoring stack

- [x] 5.1 Add `prometheus` service to `docker-compose.yml` using `prom/prometheus:latest`, mounting `prometheus.yml`
- [x] 5.2 Add `grafana` service using `grafana/grafana:latest`, port 3000
- [x] 5.3 Create `JPPhotoManagerWeb/prometheus.yml` with scrape config: `scrape_interval: 15s`, `targets: ['backend:8080']`, `metrics_path: /actuator/prometheus`

## 6. Backend unit tests

- [x] 6.1 Test that the catalog counter increments for each cataloged asset
- [x] 6.2 Test that the thumbnail timer records a non-zero duration
- [x] 6.3 Test that the SSE gauge increments on emitter creation and decrements on completion

## 7. Testing and Commit

- [x] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 7.3 Commit all changes (only after both test suites pass)
