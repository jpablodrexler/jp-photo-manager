## 1. Backend — ErrorResponse DTO

- [ ] 1.1 Create `infrastructure/web/dto/ErrorResponse.java` as a Java record: `ErrorResponse(int status, String message, Instant timestamp)`

## 2. Backend — GlobalExceptionHandler

- [ ] 2.1 Create `infrastructure/web/exception/GlobalExceptionHandler.java` annotated with `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`
- [ ] 2.2 Add handler for `ResourceNotFoundException` returning `ResponseEntity<ErrorResponse>` with status 404
- [ ] 2.3 Add handler for `ValidationException` returning `ResponseEntity<ErrorResponse>` with status 400
- [ ] 2.4 Add handler for `AccessDeniedException` returning `ResponseEntity<ErrorResponse>` with status 403
- [ ] 2.5 Add handler for `AuthenticationException` returning `ResponseEntity<ErrorResponse>` with status 401
- [ ] 2.6 Add catch-all handler for `Exception` returning 500 with message `"An unexpected error occurred"` (log the actual exception server-side via `@Slf4j`)

## 3. Backend unit tests

- [ ] 3.1 Test that `ResourceNotFoundException` produces a 404 `ErrorResponse` with the exception message
- [ ] 3.2 Test that `ValidationException` produces a 400 `ErrorResponse`
- [ ] 3.3 Test that an unhandled `Exception` produces a 500 `ErrorResponse` with the generic message (not the exception detail)
- [ ] 3.4 Test that the `timestamp` field is non-null and recent

## 4. Frontend — GlobalErrorHandler

- [ ] 4.1 Create `core/error-handler/global-error-handler.ts` implementing Angular's `ErrorHandler`; inject `MatSnackBar` and call `snackBar.open(err.message, 'Dismiss', { duration: 5000 })` in `handleError()`
- [ ] 4.2 Register in `app.config.ts`: `{ provide: ErrorHandler, useClass: GlobalErrorHandler }`

## 5. Frontend — HTTP interceptor extension

- [ ] 5.1 Extend the existing HTTP interceptor (`core/interceptors/`) to inspect error responses: if `error.error?.message` exists, pass it to `MatSnackBar`; otherwise use `"An unexpected error occurred"`

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
