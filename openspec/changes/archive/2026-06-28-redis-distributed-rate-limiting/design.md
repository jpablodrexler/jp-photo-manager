## Context

`RateLimitFilter` enforces per-IP rate limits on `POST /api/auth/login` (10 req/min) and `GET /api/assets/catalog` (5 req/hour) using the Bucket4j token-bucket library. Token-bucket state is held in a `ConcurrentHashMap<String, Bucket>` local to each JVM. A horizontally scaled deployment with N instances gives each JVM an independent bucket, allowing a client to accumulate N× the intended limit by distributing requests across instances. The fix is to back the bucket store with Redis so all instances share one set of counters.

The `bucket4j-redis` extension (`com.bucket4j:bucket4j-redis`) provides `LettuceBasedProxyManager<String>` as a drop-in replacement for the in-memory `Bucket.builder()` approach. The only change in `RateLimitFilter` is how a bucket is resolved for a given key; the token-consumption logic, IP resolution, and `Retry-After` header are unchanged.

## Goals / Non-Goals

**Goals:**
- Rate-limit counters are stored in Redis and shared across all running instances.
- Zero change to the configured limits (10 req/min for login, 5 req/hour for catalog).
- Unit tests continue to pass without a running Redis server, using an in-memory proxy manager.
- Redis is provisioned in `docker-compose.yml` for local development.

**Non-Goals:**
- Adding rate limits to new endpoints (out of scope for this change).
- Persisting counters across Redis restarts (TTL-backed counters reset on Redis restart; this is acceptable since the window duration is short).
- Distributed tracing or observability for rate-limit events beyond the existing `log.warn`.
- Redis authentication or TLS (deferred; configure at the infrastructure layer for production).

## Decisions

### D1 — Lettuce over Jedis as the Redis client

Lettuce is the reactive, thread-safe Redis client already included transitively by `spring-boot-starter-data-redis`. Jedis requires connection pooling boilerplate and is synchronous only. Lettuce's `StatefulRedisConnection` is reused across threads by the `LettuceBasedProxyManager` without additional configuration.

**Alternative considered:** Jedis — rejected because it adds a dependency and requires an explicit connection pool configuration that Lettuce avoids.

### D2 — Inject `ProxyManager` as a constructor dependency into `RateLimitFilter`

Rather than instantiating `LettuceBasedProxyManager` inside the filter, inject it as a constructor argument. In tests, the existing `MockHttpServletRequest`-based setup can inject a `SimpleBucketListener`-based in-memory stand-in (`LocalBucketProxyManager`), keeping the unit test free of Redis. In production, `AppConfig` provides the `LettuceBasedProxyManager` bean wired to the real Lettuce connection.

**Alternative considered:** Constructing the proxy manager directly inside the filter — rejected because it makes the filter untestable without a live Redis connection.

### D3 — `allkeys-lru` eviction policy on Redis

Rate-limit buckets are short-lived (the login bucket refills in 60 s; the catalog bucket in 1 h). With `allkeys-lru`, Redis evicts the least-recently-used keys when memory fills up, which naturally removes buckets for IPs that have not been seen recently. No explicit TTL management is needed in the application.

**Alternative considered:** Setting an explicit TTL per bucket via `bucket4j-redis` expiry configuration — viable but adds complexity when `allkeys-lru` achieves the same effect with zero application code.

### D4 — Fail-open on Redis connection loss

If the Redis connection is unavailable, `LettuceBasedProxyManager` throws `BucketNotFoundException` or a Lettuce connectivity exception. The filter catches these, logs a warning, and allows the request through (fail-open) rather than returning `503`. Brute-force protection degrades gracefully during Redis outages rather than blocking all legitimate traffic.

**Alternative considered:** Fail-closed (return 503 on Redis error) — rejected because an accidental Redis restart would take the login page down for all users.

### D5 — Redis 7-alpine in `docker-compose.yml` with a 256 MB memory cap

Redis is added as a named service. A memory cap keeps the container bounded; `allkeys-lru` handles eviction at that boundary. No authentication is configured for local development; production deployments should use `requirepass` or Redis ACLs at the infrastructure layer.

## Risks / Trade-offs

- **Redis is a new runtime dependency.** Local dev must run `docker-compose up redis` or the full stack. Mitigation: document this in the README; the existing `docker-compose.yml` already requires PostgreSQL, so adding one more service is low friction.
- **Fail-open degrades brute-force protection during Redis outages.** Mitigation: alert on Redis connectivity errors via the existing Prometheus metrics (`redis_connected` gauge); ops team can respond before an attacker exploits the window.
- **`allkeys-lru` may evict active buckets under sustained traffic.** Mitigation: size the Redis memory cap (256 MB) well above what rate-limit buckets require (a few hundred bytes per active IP); in practice, thousands of distinct IPs can be tracked in this budget.
- **Unit tests use an in-memory proxy manager, not a real Redis.** Behaviour of `LettuceBasedProxyManager` is covered only by integration tests (requires a Redis Testcontainer). Adding a `@IntegrationTest` class that starts Redis via Testcontainers is recommended but deferred to a follow-up.

## Migration Plan

1. Add `spring-boot-starter-data-redis` and `com.bucket4j:bucket4j-redis` to `pom.xml`.
2. Add `redis` service to `docker-compose.yml`.
3. Add `REDIS_HOST` / `REDIS_PORT` properties to `application.yml` (defaults: `localhost`, `6379`).
4. Add `RedisConnectionFactory` and `LettuceBasedProxyManager<String>` beans to `AppConfig`.
5. Refactor `RateLimitFilter`: remove `ConcurrentHashMap`; accept `ProxyManager<String>` constructor argument; resolve bucket via `proxyManager.builder().build(bucketKey, configSupplier)`.
6. Update `RateLimitFilterTest`: inject an in-memory proxy manager (provided by `bucket4j-redis` test utilities or a simple `Map`-backed stub).
7. Deploy. No database migration, no frontend change, no API change.

**Rollback:** Revert to the previous `RateLimitFilter` implementation. The `ConcurrentHashMap` version can be kept in a feature flag (`photomanager.rate-limit.backend: memory | redis`) for a zero-risk cutover, though given the low risk this is optional.

## Open Questions

- Should the Redis memory cap be configurable in `application.yml` or hardcoded in `docker-compose.yml` only?  
  → Hardcode in `docker-compose.yml` for local dev; production sets its own Redis `maxmemory` via infrastructure tooling.
- Is a Testcontainer integration test for `RateLimitFilter` + Redis in scope for this change?  
  → Deferred; unit tests with an in-memory proxy manager are sufficient for the initial delivery.
