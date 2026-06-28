# redis-distributed-rate-limiting

Rate-limit token-bucket state is stored in Redis so that per-IP counters are shared across all running application instances. A client IP is subject to the same configured limits regardless of which instance handles each individual request.

---

## ADDED Requirements

### Requirement: Rate-limit counters are shared across all application instances

The rate-limit filter SHALL use a Redis-backed token-bucket store so that requests from the same client IP are counted globally, not per-JVM.

#### Scenario: Same IP distributed across two instances still hits the limit

- **GIVEN** two running instances of the application both connected to the same Redis
- **AND** a client IP that has made 5 login attempts handled by instance A
- **WHEN** the same client IP makes a 6th login attempt handled by instance B
- **THEN** instance B's response counts the 6 cumulative attempts and continues to allow requests until the 10th is reached

#### Scenario: Limit is reached when requests are spread across instances

- **GIVEN** two running instances sharing the same Redis
- **AND** a client IP that has made 5 login attempts on instance A and 5 on instance B (10 total)
- **WHEN** the same client IP makes an 11th login attempt on either instance
- **THEN** the response is `429 Too Many Requests` with a `Retry-After` header

### Requirement: Redis connection loss causes the filter to fail-open

When the Redis connection is unavailable, the rate-limit filter SHALL allow the request to proceed rather than returning an error, and SHALL log a warning.

#### Scenario: Redis unavailable — request is allowed through

- **GIVEN** the Redis connection is down
- **WHEN** a client makes a login request
- **THEN** the request proceeds normally (the rate limit is not enforced)
- **AND** a warning is logged indicating that Redis is unavailable

### Requirement: Redis is available in the local development environment via docker-compose

The `docker-compose.yml` SHALL include a `redis` service so that developers can run the full application stack locally without additional manual setup.

#### Scenario: docker-compose up starts Redis alongside PostgreSQL

- **WHEN** `docker-compose up` is executed
- **THEN** a Redis 7 container is started on port 6379
- **AND** the application connects to it successfully on startup
