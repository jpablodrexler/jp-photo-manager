## ADDED Requirements

### Requirement: EntityNotFoundException returns 404 with JSON body

When any controller method or service throws `jakarta.persistence.EntityNotFoundException`, the backend SHALL return HTTP 404 with a JSON body containing `timestamp`, `status` (404), `error` ("Not Found"), and `message` (the exception message).

#### Scenario: EntityNotFoundException produces 404
- **WHEN** a controller throws `EntityNotFoundException` with message "Asset not found"
- **THEN** the response status is 404
- **AND** the response body is JSON with `status: 404`, `error: "Not Found"`, `message: "Asset not found"`
- **AND** the response body contains a `timestamp` field in ISO-8601 format

### Requirement: IllegalArgumentException returns 400 with JSON body

When any controller method or service throws `IllegalArgumentException`, the backend SHALL return HTTP 400 with a JSON body containing `timestamp`, `status` (400), `error` ("Bad Request"), and `message` (the exception message).

#### Scenario: IllegalArgumentException produces 400
- **WHEN** a controller throws `IllegalArgumentException` with message "Invalid sort criteria"
- **THEN** the response status is 400
- **AND** the response body is JSON with `status: 400`, `error: "Bad Request"`, `message: "Invalid sort criteria"`

### Requirement: Unhandled exceptions return 500 with JSON body

When any unhandled exception propagates out of a controller, the backend SHALL return HTTP 500 with a JSON body containing `timestamp`, `status` (500), `error` ("Internal Server Error"), and a generic message. The full stack trace SHALL be logged at ERROR level but SHALL NOT appear in the response body.

#### Scenario: Unhandled exception produces 500
- **WHEN** a controller throws an unexpected `RuntimeException`
- **THEN** the response status is 500
- **AND** the response body is JSON with `status: 500`, `error: "Internal Server Error"`
- **AND** the stack trace is NOT included in the response body

#### Scenario: 500 error is logged
- **WHEN** an unhandled exception triggers the catch-all handler
- **THEN** the exception is logged at ERROR level with its full stack trace

### Requirement: Error response body is consistent

All error responses from the global exception handler SHALL use the same JSON structure: `timestamp` (ISO-8601 string), `status` (integer HTTP code), `error` (HTTP reason phrase), `message` (string).

#### Scenario: All error responses share the same structure
- **WHEN** any of the exception types is thrown
- **THEN** the response body contains exactly the fields `timestamp`, `status`, `error`, and `message`
