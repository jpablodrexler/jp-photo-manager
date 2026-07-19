## Context

The backend uses Spring Boot 3.4 with Spring Security. Bucket4j is a Java rate-limiting library based on the token-bucket algorithm. The `bucket4j-spring-boot-starter` integrates with Spring MVC via a `HandlerInterceptor` or `Filter`. Rate limit configuration can be expressed in `application.yml`.

## Goals / Non-Goals

**Goals:**
- 10 requests/minute per IP on `POST /api/auth/login`
- 5 requests/hour per IP on `POST /api/assets/catalog`
- `429 Too Many Requests` with `Retry-After: <seconds>` header on limit breach
- Consistent `ErrorResponse` body for 429 via `GlobalExceptionHandler`
- In-memory bucket storage (per-JVM, no distributed cache required)

**Non-Goals:**
- Distributed rate limiting across multiple instances (requires Redis — a separate concern)
- Rate limiting on all endpoints (only login and catalog are sensitive enough to warrant it)
- Per-user rate limits (IP-based is sufficient for the current threat model)

## Decisions

### 1. Bucket4j token-bucket algorithm

**Decision:** Use Bucket4j with a `Refill.intervally()` strategy: a fixed quota refilled at the start of each interval (not greedy/smooth refill).

**Rationale:** Interval refill is simpler to reason about for security limits. A login attacker gets exactly 10 attempts per minute, not a burst of 10 at the boundary.

### 2. In-memory `ConcurrentHashMap<String, Bucket>`

**Decision:** Store one `Bucket` per client IP in a `ConcurrentHashMap` within the filter bean.

**Rationale:** The application runs as a single instance. A distributed store adds operational complexity without benefit. The map is bounded by the number of unique client IPs, which is small in a personal photo manager.

### 3. `Retry-After` header

**Decision:** Include `Retry-After: <remaining seconds>` in every 429 response.

**Rationale:** Clients (and the Angular interceptor) can use this to display a human-readable "try again in N seconds" message.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| IP spoofing bypasses per-IP limits | Low | The app is a personal photo manager; sophisticated attackers are not the primary concern |
| In-memory buckets reset on restart | Low | Acceptable — limits are intended to slow brute-force, not guarantee exact enforcement across restarts |
