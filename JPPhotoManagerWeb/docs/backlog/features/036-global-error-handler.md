# Feature 36 — global-error-handler

**As a** photo library owner, **I want** clear error messages for failures, **so that** I understand what went wrong instead of seeing a raw HTTP status.

## Acceptance Criteria
- AC1: Given a missing resource, a validation error or an access-denied error, when the backend answers, then it returns 404, 400 or 403 with a { status, message, timestamp } body.
- AC2: Given an unexpected exception, when the backend answers, then it returns 500 with the same body and no stack trace.
- AC3: Given an error response with a message, when the interceptor handles it, then it shows that message; without one, a generic message.
- AC4: Given an unhandled component error, when it occurs, then a snackbar notification is shown.

## Technical notes

Override Angular's `ErrorHandler` to display a `MatSnackBar` notification for all unhandled component errors; extend the existing backend `GlobalExceptionHandler` to return a consistent `{ status, message, timestamp }` JSON body for every 4xx and 5xx response so the frontend interceptor can surface a human-readable message rather than showing a raw HTTP status

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
