## Why

Backend log lines currently contain no correlation identifier linking a specific HTTP request to its log output. When a user reports an error, engineers cannot isolate the relevant log entries. Adding a `requestId` UUID and the authenticated `username` to SLF4J MDC at the start of each request makes every log line for that request traceable, and surfacing the `X-Request-ID` header to the Angular frontend lets the error snackbar display the ID for the user to report.

## What Changes

- A new `RequestCorrelationFilter` (servlet `Filter`) injects a `requestId` UUID and `username` into SLF4J MDC at request start and clears them on completion
- The filter sets `X-Request-ID: <uuid>` on every HTTP response
- `logstash-logback-encoder` (already configured) automatically includes MDC fields in JSON log lines
- The Angular `GlobalErrorHandler` reads the `X-Request-ID` response header and appends it to the error snackbar message

## Capabilities

### New Capabilities

- `request-correlation-mdc`: Every HTTP request is tagged with a UUID `requestId` and `username` in SLF4J MDC. The ID is returned in the `X-Request-ID` response header, enabling frontend-to-backend log correlation.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/filter/RequestCorrelationFilter.java` — new servlet filter
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/AppConfig.java` — register the filter
- `JPPhotoManagerWeb/backend/src/test/` — tests for MDC population and response header
- `JPPhotoManagerWeb/frontend/src/app/core/interceptors/http-error.interceptor.ts` — extract `X-Request-ID` from error responses and include in snackbar message
