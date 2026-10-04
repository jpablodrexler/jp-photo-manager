# Feature 36 — global-error-handler

Override Angular's `ErrorHandler` to display a `MatSnackBar` notification for all unhandled component errors; extend the existing backend `GlobalExceptionHandler` to return a consistent `{ status, message, timestamp }` JSON body for every 4xx and 5xx response so the frontend interceptor can surface a human-readable message rather than showing a raw HTTP status
