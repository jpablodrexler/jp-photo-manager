# Feature 43 — request-correlation-mdc

**As a** administrator, **I want** every request tagged with a correlation id and the username in the logs, **so that** I can trace one request through the logs and match a user's error report.

## Acceptance Criteria
- AC1: Given any request, when it starts, then a request id and the username are placed in the log context, and the context is cleared when it ends.
- AC2: Given an anonymous request, when it is logged, then the username is "anonymous".
- AC3: Given a successful or an error response, when it is returned, then it carries an X-Request-ID header.
- AC4: Given an error shown in the app, when the snackbar appears, then it includes the request id.

## Technical notes

Add a servlet `Filter` that injects a `requestId` UUID and the authenticated `username` into SLF4J `MDC` at the start of each request and clears it on completion; `logstash-logback-encoder` is already configured in `logback-spring.xml` and will automatically include both fields in every JSON log line; also set `X-Request-ID` on the response so the Angular frontend can log the correlation ID alongside client-side errors from `global-error-handler` (#36)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
