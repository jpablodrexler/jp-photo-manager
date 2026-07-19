## Why

`RateLimitFilter` stores token-bucket state in a `ConcurrentHashMap` inside each JVM. When multiple application instances run (behind a load balancer), every instance maintains an independent bucket, so a client can effectively make N× the configured limit of requests by spreading them across N instances. This is a production-safety gap that must be closed before horizontal scaling is used in any environment.

## What Changes

- Replace the `ConcurrentHashMap<String, Bucket>` in `RateLimitFilter` with a `LettuceBasedProxyManager` backed by Redis, so all instances share the same token-bucket counters.
- Add `spring-boot-starter-data-redis` and `com.bucket4j:bucket4j-redis` to `pom.xml`.
- Add a `RedisConnectionFactory` bean (Lettuce driver) to `AppConfig`; configure via `REDIS_HOST` / `REDIS_PORT` environment variables.
- Add a `redis` service to `docker-compose.yml` (`redis:7-alpine`, port 6379, `allkeys-lru` eviction).
- Add `REDIS_HOST` and `REDIS_PORT` configuration to `application.yml`.
- Update `RateLimitFilterTest` to exercise the filter without Redis by supplying an in-memory proxy manager (the `bucket4j-redis` API supports this).

## Capabilities

### New Capabilities

- `redis-distributed-rate-limiting`: Rate-limit counters are stored in Redis and shared across all application instances; a client IP is subject to the same configured limits regardless of which instance handles each request.

### Modified Capabilities

- `api-rate-limiting`: Add a requirement that the per-IP rate limits (`POST /api/auth/login` 10 req/min, `GET /api/assets/catalog` 5 req/hour) are enforced collectively across all running instances, not independently per JVM.

## Impact

- **Backend**: `RateLimitFilter.java`, `AppConfig.java`, `pom.xml`, `application.yml`
- **Tests**: `RateLimitFilterTest.java` (unit tests continue to work; proxy manager is injected, allowing an in-memory stand-in in tests)
- **Infrastructure**: Requires Redis 7+ at runtime; `docker-compose.yml` gains a `redis` service
- **No schema change**: No Flyway migration required
- **No frontend change**: Rate-limit behaviour is transparent to the Angular SPA
