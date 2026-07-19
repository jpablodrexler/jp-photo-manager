## Why

The web application currently lacks essential production-readiness features: logs are plain text making them hard to parse with log aggregators; unhandled exceptions return inconsistent error responses; and there is no access control, allowing anyone with network access to read and modify the photo library. These three concerns — observability, error handling, and authentication — need to be addressed together to make the application suitable for shared or networked deployments.

## What Changes

- **Structured JSON logging:** Replace plain-text file logging with JSON-formatted logs via `logstash-logback-encoder`. A `logback-spring.xml` configures a rolling JSON file appender and a plain-text console appender. Log location remains `~/.photomanager/logs/photomanager.log`. README updated with log format and location documentation.
- **Global exception handler:** A `@RestControllerAdvice GlobalExceptionHandler` maps `EntityNotFoundException` → 404, `IllegalArgumentException` → 400, and all other `Exception` → 500. Every error response body is a consistent JSON object with `timestamp`, `status`, `error`, and `message` fields.
- **User authentication:** A `users` table stores credentials (BCrypt-hashed passwords). Spring Security secures all `/api/**` endpoints with JWT bearer token validation. New `POST /api/auth/register` and `POST /api/auth/login` endpoints are public. The Angular frontend gains `LoginComponent`, `RegisterComponent`, an `AuthService`, a route guard, and an HTTP interceptor.

## Capabilities

### New Capabilities

- `structured-logging`: JSON-formatted log output to file with plain-text console output.
- `global-exception-handling`: Consistent JSON error responses for all unhandled exceptions.
- `user-authentication`: Full login and registration flow securing all API endpoints with JWT.

### Modified Capabilities

## Impact

- **Backend — new files:** `GlobalExceptionHandler`; `User` entity; `UserRepository`; `UserService`; `AuthController`; `SecurityConfig`; `JwtUtil`; `logback-spring.xml`; Flyway migration `V4__add_users.sql`.
- **Backend — modified files:** `pom.xml` (add `logstash-logback-encoder`, `spring-boot-starter-security`, `jjwt` dependencies); `application.yml` (add JWT secret and expiry config); `AppConfig` (remove logging config if present).
- **Frontend — new files:** `features/auth/login/login.component.ts/html/scss`; `features/auth/register/register.component.ts/html/scss`; `core/services/auth.service.ts`; `core/guards/auth.guard.ts`; `core/interceptors/auth.interceptor.ts`.
- **Frontend — modified files:** `app.routes.ts` (add `/login`, `/register` routes and guard); `app.config.ts` (register interceptor).
- **Tests:** Unit tests for `GlobalExceptionHandler`, `UserService`, `AuthController`; Cypress component tests for `LoginComponent` and `RegisterComponent`.
- **README:** New sections for structured logging and authentication.
