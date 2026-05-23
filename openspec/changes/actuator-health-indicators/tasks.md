## 1. Dependency and configuration

- [ ] 1.1 Add `spring-boot-starter-actuator` to `pom.xml`
- [ ] 1.2 Add to `application.yml`:
  - `management.endpoints.web.exposure.include: health`
  - `management.endpoint.health.show-details: always`
  - `management.health.defaults.enabled: true`

## 2. Security configuration

- [ ] 2.1 Add `/actuator/health` to the `permitAll()` list in `SecurityConfig` (before the authenticated filter chain)

## 3. DatabaseHealthIndicator

- [ ] 3.1 Create `infrastructure/health/DatabaseHealthIndicator.java` implementing `HealthIndicator`
- [ ] 3.2 Inject `DataSource`; execute `SELECT 1`; return `Health.up()` on success, `Health.down(exception)` on failure

## 4. ThumbnailStorageHealthIndicator

- [ ] 4.1 Create `infrastructure/health/ThumbnailStorageHealthIndicator.java` implementing `HealthIndicator`
- [ ] 4.2 Inject `@Value("${photomanager.thumbnails-directory}")` path; check `Files.isDirectory()` and `Files.isWritable()`; return `Health.up()` or `Health.down()`

## 5. GeocodingHealthIndicator

- [ ] 5.1 Create `infrastructure/health/GeocodingHealthIndicator.java` implementing `HealthIndicator`
- [ ] 5.2 Send a HEAD request to the geocoding base URL with a 2-second timeout
- [ ] 5.3 Return `Health.up()` on 2xx, `Health.status("DEGRADED").build()` on timeout or connection error
- [ ] 5.4 Register `DEGRADED` in the `HealthAggregator` order so it does not bring the aggregate DOWN: add a `StatusAggregator` bean with ordering `UP > DEGRADED > UNKNOWN > DOWN`

## 6. Backend unit tests

- [ ] 6.1 Test `DatabaseHealthIndicator` returns UP when `SELECT 1` succeeds
- [ ] 6.2 Test `DatabaseHealthIndicator` returns DOWN when the datasource throws
- [ ] 6.3 Test `ThumbnailStorageHealthIndicator` returns UP for a writable directory
- [ ] 6.4 Test `ThumbnailStorageHealthIndicator` returns DOWN for a non-writable path
- [ ] 6.5 Test `GeocodingHealthIndicator` returns DEGRADED when the HTTP call fails

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
