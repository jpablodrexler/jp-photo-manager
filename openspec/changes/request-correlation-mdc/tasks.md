## 1. Backend — RequestCorrelationFilter

- [ ] 1.1 Create `infrastructure/web/filter/RequestCorrelationFilter.java` implementing `jakarta.servlet.Filter`
- [ ] 1.2 In `doFilter()`: generate `UUID.randomUUID().toString()` as `requestId`; extract username from `SecurityContextHolder` (use `"anonymous"` if null)
- [ ] 1.3 Call `MDC.put("requestId", requestId)` and `MDC.put("username", username)`
- [ ] 1.4 Set `response.setHeader("X-Request-ID", requestId)`
- [ ] 1.5 Call `chain.doFilter(request, response)` inside a `try/finally` that calls `MDC.clear()` in the `finally` block

## 2. Filter registration

- [ ] 2.1 Register `RequestCorrelationFilter` as a `FilterRegistrationBean` with `Ordered.HIGHEST_PRECEDENCE` in `AppConfig` (or `SecurityConfig`)

## 3. CORS update

- [ ] 3.1 Add `X-Request-ID` to the `exposedHeaders` list in the CORS configuration so browser clients can read the header

## 4. Backend unit tests

- [ ] 4.1 Test that the filter sets `X-Request-ID` response header with a valid UUID
- [ ] 4.2 Test that `MDC.get("requestId")` equals the response header value during request processing
- [ ] 4.3 Test that MDC is cleared after the filter chain completes
- [ ] 4.4 Test that an unauthenticated request sets `MDC.get("username")` to `"anonymous"`

## 5. Frontend — HTTP interceptor update

- [ ] 5.1 In the HTTP interceptor's error handler, read `error.headers.get('X-Request-ID')`
- [ ] 5.2 If present, append `[Request ID: <id>]` to the snackbar error message

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
