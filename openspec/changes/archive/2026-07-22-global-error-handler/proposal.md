## Why

Backend errors currently return inconsistent response bodies depending on which Spring exception handler fires. Some return plain text, some return Spring's default `DefaultErrorAttributes` JSON, and some return nothing. The Angular frontend cannot reliably extract a human-readable message from these. Unhandled Angular component errors also go unnoticed by the user. A consistent `{ status, message, timestamp }` error body from the backend and an Angular `ErrorHandler` override that surfaces errors via `MatSnackBar` fix both problems.

## What Changes

- Override Spring's `ResponseEntityExceptionHandler` in a `@RestControllerAdvice GlobalExceptionHandler` class returning `{ status, message, timestamp }` for all 4xx and 5xx responses
- Override Angular's `ErrorHandler` to catch all unhandled component errors and display a `MatSnackBar` notification
- The Angular HTTP interceptor (already used for auth) is extended to extract the `message` field from error responses and surface it in the snackbar

## Capabilities

### New Capabilities

- `global-error-handler`: All backend 4xx/5xx responses return a consistent `{ status, message, timestamp }` JSON body. All unhandled Angular component errors display a `MatSnackBar` notification with the error message.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/exception/GlobalExceptionHandler.java` — new `@RestControllerAdvice`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/dto/ErrorResponse.java` — new DTO record
- `JPPhotoManagerWeb/backend/src/test/` — tests for each handled exception type
- `JPPhotoManagerWeb/frontend/src/app/core/error-handler/global-error-handler.ts` — new `ErrorHandler` override
- `JPPhotoManagerWeb/frontend/src/app/app.config.ts` — register `GlobalErrorHandler`
- `JPPhotoManagerWeb/frontend/src/app/core/interceptors/http-error.interceptor.ts` — extend to extract `message` from error response body
