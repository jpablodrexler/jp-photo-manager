## Why

The GPS map view feature calls an external geocoding API to reverse-geocode asset coordinates. When the geocoding service is slow or unavailable, every asset load blocks until the request times out, degrading the entire gallery experience. A circuit breaker around the `GeocodingPort` adapter prevents cascading failures and fast-fails when the external service is unhealthy.

## What Changes

- Add `resilience4j-spring-boot3` to the backend
- Annotate the `GeocodingPort` infrastructure adapter with `@CircuitBreaker(name = "geocoding", fallbackMethod = "fallback")`
- The fallback returns `Optional.empty()` so the UI gracefully omits the address label
- Circuit breaker configuration in `application.yml`: 50% failure threshold, 10-call sliding window, 10s wait in OPEN state

## Capabilities

### New Capabilities

- `circuit-breaker`: The geocoding adapter is protected by a Resilience4j circuit breaker. When the external geocoding service fails repeatedly, the circuit opens and subsequent calls fast-fail with a fallback (no address) instead of blocking.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `resilience4j-spring-boot3`
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — circuit breaker configuration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/adapter/out/geocoding/GeocodingAdapter.java` — add `@CircuitBreaker` annotation and fallback method
- `JPPhotoManagerWeb/backend/src/test/` — tests for circuit breaker fallback behavior
