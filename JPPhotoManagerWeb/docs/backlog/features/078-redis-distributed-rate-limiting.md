# Feature 78 — redis-distributed-rate-limiting

**As a** administrator, **I want** rate limits shared across instances through Redis, **so that** attackers cannot multiply their allowance by hitting different instances.

## Acceptance Criteria
- AC1: Given requests spread across two instances, when the same IP exceeds the limit, then it is rejected.
- AC2: Given login and catalog limits, when exceeded, then 429 is returned with a Retry-After header; within the limit, the request succeeds.
- AC3: Given Redis is unavailable, when a request arrives, then it is allowed through.
- AC4: Given Docker Compose, when it starts, then Redis runs alongside PostgreSQL.

## Technical notes

Upgrade `RateLimitFilter` from in-memory Bucket4j (backed by `ConcurrentHashMap<String, Bucket>`) to a Redis-backed store using the `bucket4j-redis` / `bucket4j-lettuce` extension; the current implementation is per-JVM: two running instances each allow the full configured rate (10 login attempts/min per IP, 5 catalog triggers/hour per IP) independently, giving attackers proportionally more headroom; the Redis upgrade shares token-bucket counters across all instances with zero business-logic change — only `RateLimitFilter.createBucket()` switches from `Bucket.builder()...build()` to `proxyManager.builder().build(bucketKey, configSupplier)`; **fills the multi-instance production-safety gap left by `api-rate-limiting` (#39)**, which is already implemented but only protects single-instance deployments

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
