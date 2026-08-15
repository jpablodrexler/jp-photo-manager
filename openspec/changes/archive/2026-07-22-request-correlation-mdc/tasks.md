## 1. Backend — RequestCorrelationFilter

- [x] 1.1 Create `infrastructure/web/filter/RequestCorrelationFilter.java` implementing `jakarta.servlet.Filter`
- [x] 1.2 In `doFilter()`: generate `UUID.randomUUID().toString()` as `requestId`; extract username from `SecurityContextHolder` (use `"anonymous"` if null)
- [x] 1.3 Call `MDC.put("requestId", requestId)` and `MDC.put("username", username)`
- [x] 1.4 Set `response.setHeader("X-Request-ID", requestId)`
- [x] 1.5 Call `chain.doFilter(request, response)` inside a `try/finally` that calls `MDC.clear()` in the `finally` block

## 2. Filter registration

- [x] 2.1 Register `RequestCorrelationFilter` as a `FilterRegistrationBean` with `Ordered.HIGHEST_PRECEDENCE` in `AppConfig` (or `SecurityConfig`)

## 3. CORS update

- [x] 3.1 Add `X-Request-ID` to the `exposedHeaders` list in the CORS configuration so browser clients can read the header

## 4. Backend unit tests

- [x] 4.1 Test that the filter sets `X-Request-ID` response header with a valid UUID
- [x] 4.2 Test that `MDC.get("requestId")` equals the response header value during request processing
- [x] 4.3 Test that MDC is cleared after the filter chain completes
- [x] 4.4 Test that an unauthenticated request sets `MDC.get("username")` to `"anonymous"`

## 5. Frontend — HTTP interceptor update

- [x] 5.1 In the HTTP interceptor's error handler, read `error.headers.get('X-Request-ID')`
- [x] 5.2 If present, append `[Request ID: <id>]` to the snackbar error message

## 6. Testing and Commit

- [x] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test` — 677 tests, 0 failures, 0 errors (2 skipped, pre-existing)
- [x] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test` — 362 tests, all passing
- [x] 6.3 Commit all changes (only after both test suites pass) — both suites pass; the actual `git commit` is intentionally deferred to the user per repository policy (commits are only created when explicitly requested) and this workflow's no-commit-during-implementation guardrail
