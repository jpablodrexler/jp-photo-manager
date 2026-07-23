## Context

Spring Boot uses SLF4J with `logback-spring.xml`. If `logstash-logback-encoder` is already configured, any key present in `MDC` at log time is automatically included in the JSON log output. A servlet `Filter` is the correct place to set and clear MDC because it wraps the entire request lifecycle including filters that run after it.

## Goals / Non-Goals

**Goals:**
- `RequestCorrelationFilter` sets `MDC.put("requestId", UUID.randomUUID().toString())` and `MDC.put("username", <principal name or "anonymous">)` at the start of every request
- Clears both MDC entries in a `finally` block to prevent thread-pool leakage
- Sets `X-Request-ID: <requestId>` response header
- Angular HTTP interceptor reads `X-Request-ID` from error responses and includes it in the snackbar: "Error: Asset not found [Request ID: abc-123]"

**Non-Goals:**
- Distributed tracing (OpenTelemetry/Zipkin) — MDC is for log correlation only
- Propagating the request ID to downstream HTTP calls (no downstream services exist)
- Storing request IDs in the database

## Decisions

### 1. Generate `requestId` in the filter, not use an incoming header

**Decision:** Always generate a new UUID regardless of any incoming `X-Request-ID` header.

**Rationale:** Accepting caller-supplied IDs would allow clients to influence log fields, creating a potential log injection vector. Internal IDs are always authoritative.

### 2. `FilterRegistrationBean` with highest precedence

**Decision:** Register `RequestCorrelationFilter` at `Ordered.HIGHEST_PRECEDENCE` so the `requestId` is in MDC before any other filter (including Spring Security) logs.

**Rationale:** Security audit logging should include the `requestId` for correlation. If the filter runs after security filters, auth failure logs lack the ID.

### 3. Clear MDC in `finally`, not `afterCompletion`

**Decision:** Use a `try/finally` around `chain.doFilter()` to clear MDC.

**Rationale:** `afterCompletion` is not available in plain servlet filters (only Spring `HandlerInterceptor`). A `finally` block guarantees cleanup even when the request thread throws.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| MDC leak on thread reuse (thread pool) | Medium | `finally` block always calls `MDC.clear()` after request completes |
| `X-Request-ID` header exposed to CORS clients | Low | Add `X-Request-ID` to `exposedHeaders` in CORS configuration |
