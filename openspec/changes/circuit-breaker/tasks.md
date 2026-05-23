## 1. Dependency

- [ ] 1.1 Add `resilience4j-spring-boot3` and `spring-boot-starter-aop` to `pom.xml` (AOP is required for Resilience4j annotations)

## 2. Circuit breaker configuration

- [ ] 2.1 Add `resilience4j.circuitbreaker.instances.geocoding` to `application.yml`:
  - `slidingWindowType: COUNT_BASED`
  - `slidingWindowSize: 10`
  - `failureRateThreshold: 50`
  - `waitDurationInOpenState: 10s`
  - `permittedNumberOfCallsInHalfOpenState: 3`
  - `registerHealthIndicator: true`

## 3. GeocodingAdapter update

- [ ] 3.1 Annotate `GeocodingAdapter.reverseGeocode()` with `@CircuitBreaker(name = "geocoding", fallbackMethod = "fallbackGeocode")`
- [ ] 3.2 Add `fallbackGeocode(double lat, double lon, Exception ex)` method returning `Optional.empty()`; log the exception at WARN level

## 4. Actuator exposure

- [ ] 4.1 Ensure `management.endpoints.web.exposure.include` in `application.yml` includes `health`
- [ ] 4.2 Ensure `management.health.circuitbreakers.enabled: true`

## 5. Backend unit tests

- [ ] 5.1 Test that `fallbackGeocode()` returns `Optional.empty()`
- [ ] 5.2 Test that when the geocoding HTTP call throws an exception, the fallback is invoked
- [ ] 5.3 Test circuit breaker configuration is loaded (use Resilience4j test utilities)

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
