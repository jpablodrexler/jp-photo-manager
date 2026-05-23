## 1. Dependency and configuration

- [ ] 1.1 Add `micrometer-registry-prometheus` to `pom.xml`
- [ ] 1.2 Add to `application.yml`:
  - `management.endpoints.web.exposure.include: health,prometheus`
  - `management.metrics.export.prometheus.enabled: true`
- [ ] 1.3 Add `/actuator/prometheus` to `permitAll()` in `SecurityConfig`

## 2. Catalog counter

- [ ] 2.1 Inject `MeterRegistry` into `CatalogAssetsUseCaseImpl`
- [ ] 2.2 Register `Counter.builder("photomanager_catalog_assets_total").description("Total assets cataloged").register(meterRegistry)`
- [ ] 2.3 Call `counter.increment()` after each asset is successfully persisted

## 3. Thumbnail generation timer

- [ ] 3.1 Inject `MeterRegistry` into `StorageServiceImpl`
- [ ] 3.2 Register `Timer.builder("photomanager_thumbnail_generation_seconds").description("Thumbnail generation latency").register(meterRegistry)`
- [ ] 3.3 Wrap `generateThumbnail()` body with `timer.record(() -> { ... })`

## 4. Active SSE connections gauge

- [ ] 4.1 Add `AtomicInteger sseConnectionCount = new AtomicInteger(0)` to `AssetController`
- [ ] 4.2 Register `Gauge.builder("photomanager_active_sse_connections", sseConnectionCount, AtomicInteger::get).description("Active SSE connections").register(meterRegistry)` in `AssetController` constructor
- [ ] 4.3 Increment `sseConnectionCount` when creating an `SseEmitter`; decrement in `onCompletion`, `onTimeout`, and `onError` callbacks

## 5. Docker Compose monitoring stack

- [ ] 5.1 Add `prometheus` service to `docker-compose.yml` using `prom/prometheus:latest`, mounting `prometheus.yml`
- [ ] 5.2 Add `grafana` service using `grafana/grafana:latest`, port 3000
- [ ] 5.3 Create `JPPhotoManagerWeb/prometheus.yml` with scrape config: `scrape_interval: 15s`, `targets: ['backend:8080']`, `metrics_path: /actuator/prometheus`

## 6. Backend unit tests

- [ ] 6.1 Test that the catalog counter increments for each cataloged asset
- [ ] 6.2 Test that the thumbnail timer records a non-zero duration
- [ ] 6.3 Test that the SSE gauge increments on emitter creation and decrements on completion

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
