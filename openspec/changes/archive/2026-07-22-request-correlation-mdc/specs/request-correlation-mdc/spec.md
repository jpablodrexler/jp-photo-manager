# request-correlation-mdc

Every HTTP request is tagged with a UUID `requestId` and `username` in SLF4J MDC. The ID is returned in the `X-Request-ID` response header, enabling frontend-to-backend log correlation.

---

## ADDED Requirements

### Requirement: Every HTTP response includes an X-Request-ID header

The `RequestCorrelationFilter` SHALL set a `X-Request-ID` response header containing a UUID for every HTTP request processed by the backend.

#### Scenario: X-Request-ID header is present on successful response

- **GIVEN** an authenticated user makes a request to `GET /api/assets`
- **WHEN** the response is returned
- **THEN** the response includes `X-Request-ID: <UUID>` where the UUID is a valid RFC 4122 v4 UUID

#### Scenario: X-Request-ID header is present on error responses

- **GIVEN** a request to a non-existent resource
- **WHEN** the backend returns `404 Not Found`
- **THEN** the `404` response also includes `X-Request-ID: <UUID>`

### Requirement: MDC contains requestId and username for all log lines within a request

Every SLF4J log statement emitted during request processing SHALL have `requestId` and `username` fields available in MDC.

#### Scenario: MDC fields are set at request start

- **GIVEN** an authenticated request arrives
- **WHEN** the request is being processed
- **THEN** `MDC.get("requestId")` returns the same UUID as the `X-Request-ID` response header, and `MDC.get("username")` returns the authenticated user's username

#### Scenario: MDC is cleared after request completes

- **GIVEN** a request has been processed
- **WHEN** the request thread is returned to the pool
- **THEN** `MDC.get("requestId")` and `MDC.get("username")` return null (MDC is cleared)

#### Scenario: Anonymous requests use "anonymous" as username

- **GIVEN** an unauthenticated request to a public endpoint (e.g., `/actuator/health`)
- **WHEN** the request is processed
- **THEN** `MDC.get("username")` is `"anonymous"`

### Requirement: Angular error snackbar includes the request ID

When the HTTP interceptor catches an error response that includes an `X-Request-ID` header, the `MatSnackBar` message SHALL append the request ID so users can report it.

#### Scenario: Error snackbar shows request ID

- **GIVEN** the backend returns a `404` response with `X-Request-ID: abc-123`
- **WHEN** the Angular interceptor catches the error
- **THEN** the snackbar displays "Asset not found [Request ID: abc-123]"
