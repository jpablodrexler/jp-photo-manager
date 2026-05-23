## Context

The `GeocodingPort` domain interface is implemented by a `GeocodingAdapter` that calls the Nominatim HTTP API. The adapter is called during asset load when GPS coordinates are present. Resilience4j is the standard fault-tolerance library for Spring Boot 3 and integrates via annotations.

## Goals / Non-Goals

**Goals:**
- `@CircuitBreaker(name = "geocoding", fallbackMethod = "fallbackGeocode")` on the `GeocodingAdapter.reverseGeocode()` method
- Fallback returns `Optional.empty()` — the caller already handles absent address gracefully
- Configuration: 50% failure threshold, sliding window of 10 calls, 10s wait in OPEN state, 3 permitted calls in HALF_OPEN
- Expose circuit breaker metrics via Spring Boot Actuator (`/actuator/health`)

**Non-Goals:**
- Circuit breakers on other adapters (only geocoding calls an external service)
- Retry logic (the circuit breaker fast-fails; retries would worsen load on an unhealthy service)
- Bulkhead or time limiter (out of scope for this change)

## Decisions

### 1. `@CircuitBreaker` on the adapter, not the use case

**Decision:** Annotate `GeocodingAdapter.reverseGeocode()` directly, not the use case that calls it.

**Rationale:** The circuit breaker wraps the outbound HTTP call. Placing it on the adapter keeps the domain use case free of infrastructure concerns.

### 2. Fallback returns `Optional.empty()`

**Decision:** The fallback method signature matches `reverseGeocode()` and returns `Optional.empty()`.

**Rationale:** The `GetAssetExifUseCaseImpl` already handles `Optional.empty()` from `GeocodingPort` — no changes needed upstream. The map view shows no address label when geocoding is unavailable.

### 3. Count-based sliding window (10 calls)

**Decision:** Use a count-based sliding window of 10 calls rather than time-based.

**Rationale:** Geocoding calls are infrequent (one per asset view). A time-based window might never accumulate enough calls to trip the breaker. A count-based window reacts to consecutive failures regardless of timing.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Circuit stays open after external service recovers | Low | 10s wait + HALF_OPEN probe automatically closes the circuit |
| False positives from transient network hiccups | Low | 50% threshold over 10 calls requires 5 consecutive failures to open |
