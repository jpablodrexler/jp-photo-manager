## Context

The backend uses Spring MVC; `ResponseEntityExceptionHandler` handles Spring-specific exceptions. Currently no `@RestControllerAdvice` class provides a uniform error body. The frontend has an HTTP interceptor (added for JWT refresh) that could be extended to extract error messages from the response body.

## Goals / Non-Goals

**Goals:**
- `GlobalExceptionHandler` (`@RestControllerAdvice`) catches: `ResourceNotFoundException` (404), `ValidationException` (400), `AccessDeniedException` (403), `AuthenticationException` (401), and any other `Exception` (500)
- All responses use the `ErrorResponse` record: `{ int status, String message, Instant timestamp }`
- Angular `GlobalErrorHandler` catches component errors and shows a `MatSnackBar`
- HTTP interceptor extracts `message` from error response body

**Non-Goals:**
- Stack trace exposure in production (never include in `ErrorResponse`)
- Localized error messages (a separate `multi-language-i18n` concern)
- Custom error pages for non-API requests

## Decisions

### 1. `ErrorResponse` record with status, message, timestamp

**Decision:** Define `ErrorResponse(int status, String message, Instant timestamp)` as the uniform error body.

**Rationale:** `status` mirrors the HTTP status code for clients that parse the body rather than the header. `message` is human-readable. `timestamp` aids log correlation.

### 2. Never expose stack traces

**Decision:** The `message` field for 500 errors is always `"An unexpected error occurred"` (or a configurable generic message). The actual exception detail is logged server-side only.

**Rationale:** Stack traces in API responses are a security vulnerability that reveals implementation details.

### 3. Angular `ErrorHandler` override

**Decision:** Create `GlobalErrorHandler implements ErrorHandler` that calls `MatSnackBar.open()` with a short error message. Register it in `app.config.ts` as `{ provide: ErrorHandler, useClass: GlobalErrorHandler }`.

**Rationale:** Without an `ErrorHandler` override, unhandled errors in Angular components are only logged to the browser console and invisible to the user.

### 4. HTTP interceptor extends existing auth interceptor

**Decision:** Extend the existing HTTP interceptor to inspect error responses: if the body has a `message` field (from `GlobalExceptionHandler`), pass it to the snackbar; otherwise use a generic message.

**Rationale:** Centralizing error display in the interceptor avoids duplicate error handling in each component's `.subscribe({ error: ... })` callback.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Generic 500 message unhelpful for debugging | Low | Detailed error is logged server-side with a `requestId` (see `request-correlation-mdc` #43) |
| Interceptor suppresses per-component error handling | Low | Components can still handle errors in `.subscribe({ error })` — the interceptor fires first but does not consume the error |
