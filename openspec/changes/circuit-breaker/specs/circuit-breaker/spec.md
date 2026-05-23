# circuit-breaker

The geocoding adapter is protected by a Resilience4j circuit breaker. When the external geocoding service fails repeatedly, the circuit opens and subsequent calls fast-fail with a fallback (no address) instead of blocking.

---

## ADDED Requirements

### Requirement: Geocoding adapter is protected by a circuit breaker

The `GeocodingAdapter` SHALL be annotated with a Resilience4j `@CircuitBreaker`. When the failure rate exceeds 50% over a 10-call sliding window, the circuit SHALL open and subsequent calls SHALL fast-fail with the fallback.

#### Scenario: Circuit opens after repeated geocoding failures

- **GIVEN** the external geocoding service has returned errors for 5 of the last 10 calls (50% failure rate)
- **WHEN** the next call to `reverseGeocode()` is made
- **THEN** the circuit breaker opens and the call returns immediately without contacting the external service

#### Scenario: Fallback returns empty when circuit is open

- **GIVEN** the circuit breaker is in OPEN state
- **WHEN** `reverseGeocode(lat, lon)` is called
- **THEN** the fallback method returns `Optional.empty()` and the asset is displayed without an address label

#### Scenario: Circuit transitions to HALF_OPEN after wait duration

- **GIVEN** the circuit breaker is in OPEN state and 10 seconds have elapsed
- **WHEN** the next call arrives
- **THEN** the circuit transitions to HALF_OPEN and allows up to 3 probe calls through to the external service

### Requirement: Circuit breaker state is visible in Actuator health

The `/actuator/health` endpoint SHALL include the `geocoding` circuit breaker state (CLOSED, OPEN, or HALF_OPEN).

#### Scenario: Actuator health reflects open circuit

- **GIVEN** the geocoding circuit breaker is in OPEN state
- **WHEN** `GET /actuator/health` is called
- **THEN** the response includes `"geocoding": { "status": "CIRCUIT_OPEN" }` or similar Resilience4j health detail
