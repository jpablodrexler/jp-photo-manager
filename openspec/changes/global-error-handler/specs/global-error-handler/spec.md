# global-error-handler

All backend 4xx/5xx responses return a consistent `{ status, message, timestamp }` JSON body. All unhandled Angular component errors display a `MatSnackBar` notification with the error message.

---

## ADDED Requirements

### Requirement: Backend returns a consistent error response body

All 4xx and 5xx responses from the backend SHALL include a JSON body with `status` (int), `message` (String), and `timestamp` (Instant). The `GlobalExceptionHandler` (`@RestControllerAdvice`) SHALL handle `ResourceNotFoundException` (404), `ValidationException` (400), `AccessDeniedException` (403), `AuthenticationException` (401), and any other `Exception` (500).

#### Scenario: ResourceNotFoundException returns 404 with error body

- **GIVEN** a request for a non-existent asset
- **WHEN** the use case throws `ResourceNotFoundException`
- **THEN** the response is `404 Not Found` with body `{ "status": 404, "message": "Asset not found", "timestamp": "<ISO-8601>" }`

#### Scenario: ValidationException returns 400 with error body

- **GIVEN** a request with an invalid parameter
- **WHEN** the use case throws `ValidationException`
- **THEN** the response is `400 Bad Request` with body `{ "status": 400, "message": "<validation message>", "timestamp": "<ISO-8601>" }`

#### Scenario: AccessDeniedException returns 403 with error body

- **GIVEN** a VIEWER user attempting a write operation
- **WHEN** Spring Security throws `AccessDeniedException`
- **THEN** the response is `403 Forbidden` with body `{ "status": 403, "message": "Access denied", "timestamp": "<ISO-8601>" }`

#### Scenario: Unexpected exception returns 500 without stack trace

- **GIVEN** an unhandled runtime exception occurs during request processing
- **WHEN** the exception propagates to `GlobalExceptionHandler`
- **THEN** the response is `500 Internal Server Error` with body `{ "status": 500, "message": "An unexpected error occurred", "timestamp": "<ISO-8601>" }` and the stack trace is NOT included in the response body

### Requirement: Angular HTTP interceptor extracts the error message

The Angular HTTP interceptor SHALL inspect error responses: if the body contains a `message` field, surface it via `MatSnackBar`; otherwise display a generic message.

#### Scenario: Interceptor shows specific error message from backend

- **GIVEN** the backend returns a 404 response with body `{ "status": 404, "message": "Asset not found", "timestamp": "..." }`
- **WHEN** an Angular component makes a request that triggers the error
- **THEN** a `MatSnackBar` notification displays "Asset not found"

#### Scenario: Interceptor shows generic message when body has no message field

- **GIVEN** the backend returns an error response without a `message` field
- **WHEN** an Angular component makes a request that triggers the error
- **THEN** a `MatSnackBar` notification displays "An unexpected error occurred"

### Requirement: Angular GlobalErrorHandler catches unhandled component errors

The Angular `GlobalErrorHandler` (`implements ErrorHandler`) SHALL catch all unhandled component errors and display a `MatSnackBar` notification with a short error message.

#### Scenario: Unhandled component error shows snackbar

- **GIVEN** an Angular component throws an unhandled error
- **WHEN** the error propagates to Angular's error handler
- **THEN** a `MatSnackBar` notification is displayed with the error message and the error is not silently swallowed
