## 1. Backend — ErrorResponse DTO

- [x] 1.1 Create `infrastructure/web/dto/ErrorResponse.java` as a Java record: `ErrorResponse(int status, String message, Instant timestamp)`
  - **Adapted:** the codebase already has an equivalent `ErrorResponseDto` record at
    `infrastructure/web/dto/response/ErrorResponseDto.java` (`timestamp: String, status: int,
    error: String, message: String`), consumed by an existing `GlobalExceptionHandler` and its
    tests. It already satisfies the spec's required `{ status, message, timestamp }` fields (as a
    superset with an extra `error` reason-phrase field). Introducing a second, differently-shaped
    `ErrorResponse` record would have fragmented the API's error contract (two record types, two
    timestamp representations) for no behavioural gain, so `ErrorResponseDto` was kept as the one
    canonical error body instead of creating a duplicate.

## 2. Backend — GlobalExceptionHandler

- [x] 2.1 Create `infrastructure/web/exception/GlobalExceptionHandler.java` annotated with `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`
  - **Adapted:** an existing `@RestControllerAdvice GlobalExceptionHandler` already lived at
    `infrastructure/web/GlobalExceptionHandler.java` (wrong package per this project's own
    `java-developer` skill / `CLAUDE.md`, which document `infrastructure/web/exception/` as its
    home). Creating a second, separate `@RestControllerAdvice` class as literally instructed would
    have produced two Spring Boot beans both declaring `@ExceptionHandler(Exception.class)` and
    other overlapping types — an ambiguous-handler conflict at runtime. Moved the existing class to
    `infrastructure/web/exception/GlobalExceptionHandler.java` instead of creating a duplicate, and
    extended it in place (see 2.2–2.6). It does not extend `ResponseEntityExceptionHandler` — it
    already owns explicit `@ExceptionHandler` methods for every Spring MVC exception this codebase
    actually raises (`MethodArgumentNotValidException`, `HandlerMethodValidationException`,
    `HttpMessageNotReadableException`), so extending the base class would add no behavior beyond
    what's already covered.
- [x] 2.2 Add handler for `ResourceNotFoundException` returning `ResponseEntity<ErrorResponseDto>` with status 404
  - **Adapted:** this codebase has no single generic `ResourceNotFoundException` — "not found" is
    modeled per-resource (`AssetNotFoundException`, `FolderNotFoundException`,
    `AlbumNotFoundException`, `TagNotFoundException`, `UserNotFoundException`,
    `SearchPresetNotFoundException`), plus `EntityNotFoundException` and `NoSuchElementException`.
    All of these already had 404 handlers before this change; no new handler was needed here.
- [x] 2.3 Add handler for `ValidationException` returning `ResponseEntity<ErrorResponseDto>` with status 400
  - **Adapted:** no `ValidationException` type exists in this codebase; validation failures surface
    as `MethodArgumentNotValidException`, `HandlerMethodValidationException`, or
    `IllegalArgumentException`, all already handled with a 400 `ErrorResponseDto` before this
    change. No new handler was needed here.
- [x] 2.4 Add handler for `AccessDeniedException` returning `ResponseEntity<ErrorResponseDto>` with status 403
  - Already present in the pre-existing handler; carried over unchanged during the move.
- [x] 2.5 Add handler for `AuthenticationException` returning `ResponseEntity<ErrorResponseDto>` with status 401
  - Added `@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)` →
    401 with message `"Invalid username or password."`. Also added a handler for
    `InvalidRefreshTokenException` (an existing 401-triggering exception type in
    `infrastructure/web/exception/`) since it had the same "no response body" gap. Removed the
    local `try/catch` blocks in `AuthController.login()`/`refresh()` that previously returned a
    bare `ResponseEntity.status(401).build()` with no JSON body at all — exactly the "some return
    nothing" problem called out in `proposal.md`'s Why section — so these now propagate to
    `GlobalExceptionHandler` and get a consistent body.
- [x] 2.6 Add catch-all handler for `Exception` returning 500 with message `"An unexpected error occurred"` (log the actual exception server-side via `@Slf4j`)
  - Updated the existing catch-all handler's message from `"An internal error occurred."` to the
    spec's exact wording `"An unexpected error occurred"`. The exception itself continues to be
    logged server-side only via the pre-existing `@Slf4j log.error("Unhandled exception", ex)` call.

## 3. Backend unit tests

- [x] 3.1 Test that `ResourceNotFoundException` produces a 404 `ErrorResponse` with the exception message
  - Added `resourceNotFoundException_returns404WithErrorBodyContainingExceptionMessage` in
    `GlobalExceptionHandlerTest`, using `AssetNotFoundException` as the concrete stand-in (see 2.2).
- [x] 3.2 Test that `ValidationException` produces a 400 `ErrorResponse`
  - Existing `illegalArgumentException_returns400WithErrorBody` test covers this (stand-in per 2.3).
- [x] 3.3 Test that an unhandled `Exception` produces a 500 `ErrorResponse` with the generic message (not the exception detail)
  - Updated `genericException_returns500WithGenericMessageNotExceptionDetail` to assert the new
    generic message and that the thrown exception's own detail string is absent from the body.
- [x] 3.4 Test that the `timestamp` field is non-null and recent
  - Added `anyErrorResponse_timestampIsNonNullAndRecent`, parsing the JSON body and asserting the
    `timestamp` is within 10 seconds of `Instant.now()`.
  - Also added `authenticationException_returns401WithErrorBody`,
    `invalidRefreshTokenException_returns401WithErrorBody` (covering 2.5), and updated
    `AuthControllerTest`/`AuthControllerRefreshTest` to import `GlobalExceptionHandler` and assert
    the new response bodies on the login/refresh failure paths that previously returned no body.

## 4. Frontend — GlobalErrorHandler

- [x] 4.1 Create `core/error-handler/global-error-handler.ts` implementing Angular's `ErrorHandler`; inject `MatSnackBar` and call `snackBar.open(err.message, 'Dismiss', { duration: 5000 })` in `handleError()`
  - Created with a `DEFAULT_MESSAGE` fallback (`"An unexpected error occurred"`) for errors without
    a `message` (e.g. non-`Error` thrown values). Omitted a `console.error` call that would
    otherwise be typical for an `ErrorHandler` override, per this project's explicit "No
    console.log" frontend convention (no console usage exists anywhere else in `src/app`).
  - Added `core/error-handler/global-error-handler.cy.ts` Cypress component test.
- [x] 4.2 Register in `app.config.ts`: `{ provide: ErrorHandler, useClass: GlobalErrorHandler }`

## 5. Frontend — HTTP interceptor extension

- [x] 5.1 Extend the existing HTTP interceptor (`core/interceptors/`) to inspect error responses: if `error.error?.message` exists, pass it to `MatSnackBar`; otherwise use `"An unexpected error occurred"`
  - Extended `auth.interceptor.ts` (the only existing HTTP interceptor) to show a `MatSnackBar` at
    every terminal `throwError` point, without disturbing the existing 401-refresh-and-retry flow:
    a successful refresh+retry shows no snackbar; a failed refresh, a 401 on the refresh endpoint
    itself, and any other error status all show one. Updated `auth.interceptor.cy.ts` with a
    `MatSnackBar` stub provider and two new test cases for the message-present /
    message-absent scenarios.

## 6. Testing and Commit

- [x] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test` — 670 tests, 0 failures, 0 errors.
- [x] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test` — 360 tests, 0 failures.
- [ ] 6.3 Commit all changes (only after both test suites pass)
  - **Intentionally left uncommitted.** No commits are made as part of this automated
    implementation workflow; the working tree is left ready for the user (or a subsequent review
    step) to commit.
