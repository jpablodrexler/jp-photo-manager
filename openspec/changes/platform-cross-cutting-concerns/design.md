## Context

The application is a single-tenant Spring Boot REST API consumed by an Angular SPA. It currently has no authentication layer, uses plain-text Logback output, and returns unformatted exception stack traces on errors. The goal is to add all three production-readiness concerns in a single change without introducing external services.

## Goals / Non-Goals

**Goals:**
- JSON logs to file, human-readable logs to console, using only the existing Logback infrastructure.
- Consistent JSON error envelopes from every endpoint.
- Username/password authentication with JWT; all `/api/**` endpoints secured except register and login.
- Angular frontend enforces auth state; unauthenticated requests are intercepted and redirected.

**Non-Goals:**
- OAuth2, SSO, or social login.
- Role-based access control (all authenticated users have equal access).
- Refresh tokens (single expiring JWT is sufficient for now).
- Log shipping or aggregation configuration.
- Rate limiting on auth endpoints.

## Decisions

### D1 — Logback + logstash-logback-encoder for structured logging

Spring Boot ships Logback by default. Adding `logstash-logback-encoder` (net.logstash.logback) is the standard, zero-configuration way to emit JSON from Logback without swapping the logging framework. A `logback-spring.xml` overrides the Boot defaults and defines:
- A `RollingFileAppender` using `LogstashEncoder` writing to `~/.photomanager/logs/photomanager.log`, rolling daily and keeping 30 days.
- A `ConsoleAppender` using the standard `PatternLayoutEncoder` for human-readable development output.

The `logging.file.name` property in `application.yml` is removed since `logback-spring.xml` owns the file path directly.

### D2 — `@RestControllerAdvice` for global exception handling

Spring MVC's `@RestControllerAdvice` + `@ExceptionHandler` is the canonical approach. A single `GlobalExceptionHandler` class in the `api/` package handles:
- `EntityNotFoundException` (Jakarta Persistence) → 404
- `IllegalArgumentException` → 400
- `Exception` (catch-all) → 500

Error body DTO:
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "..." }
```

The `timestamp` field uses ISO-8601 format. No stack traces are exposed in the response body. The catch-all handler logs the full exception at ERROR level before responding.

### D3 — Spring Security + JJWT for authentication

**Spring Security** is the standard for Spring Boot auth. Configuration uses the lambda DSL (`SecurityFilterChain` bean, no `WebSecurityConfigurerAdapter`).

**JJWT** (io.jsonwebtoken) is a pure-Java JWT library with no external service dependency. The HS256 algorithm with a configurable secret is sufficient for a single-service deployment.

**JWT lifecycle:**
- `POST /api/auth/login` validates credentials, returns `{ "token": "<jwt>" }` with a 24-hour expiry (configurable via `photomanager.jwt-expiry-hours`).
- A `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) extracts the `Authorization: Bearer <token>` header, validates the JWT, and sets the `SecurityContext`.
- The `SecurityFilterChain` permits `/api/auth/**` and requires authentication for all other `/api/**` paths.

**Password storage:** BCrypt via Spring Security's `BCryptPasswordEncoder`. No plain-text passwords are ever stored.

**User entity:** `id` (UUID, generated), `username` (varchar 50, unique, not null), `password_hash` (varchar 255, not null), `created_at` (timestamptz, defaulting to `now()`).

### D6 — Secrets managed via git-ignored local override file

Any secret that the application needs (currently: `photomanager.jwt-secret`) SHALL be:

1. **Declared empty** in the committed `application.yml`:
   ```yaml
   photomanager:
     jwt-secret: ""        # override in application-local.yml
     jwt-expiry-hours: 24
   ```

2. **Overridden at runtime** in `application-local.yml`, which is already git-ignored (`JPPhotoManagerWeb/backend/src/main/resources/application-local.yml` is in `.gitignore`) and already imported by `application.yml` via `spring.config.import: optional:classpath:application-local.yml`.

3. **Documented** in `JPPhotoManagerWeb/README.md` with:
   - How to create the file
   - How to generate a cryptographically-safe value (`openssl rand -base64 32`)
   - An example snippet

4. **Validated at startup**: `JwtUtil` SHALL throw `IllegalStateException` on application startup if `photomanager.jwt-secret` is blank, so misconfigured deployments fail fast rather than issuing unsigned tokens.

An `application-local.yml.example` file SHALL be committed to the repository as a template administrators can copy and fill in.

### D4 — Angular auth with localStorage JWT and HTTP interceptor

**Storage:** JWT stored in `localStorage` under the key `photomanager_token`. This is acceptable for a trusted-device scenario. No refresh token is stored.

**`AuthService`:** Wraps `POST /api/auth/login` and `POST /api/auth/register`. Exposes `isLoggedIn()` (checks token presence and expiry), `getToken()`, and `logout()` (clears localStorage).

**Route guard (`AuthGuard`):** Implements `CanActivateFn`. Checks `AuthService.isLoggedIn()`; redirects to `/login` if false.

**HTTP interceptor:** Implements `HttpInterceptorFn`. Reads the token from `AuthService` and adds `Authorization: Bearer <token>` to every outgoing request.

**Routes:** `/login` and `/register` are public (no guard). All existing routes (`/gallery`, `/sync`, `/convert`, `/duplicates`) are wrapped with `canActivate: [AuthGuard]`.

### D7 — Home dashboard as the authenticated landing page

After login the user is always sent to `/home` (not `/gallery`). The home route is guarded by `AuthGuard`.

**What the page shows:**
- Application name ("JP Photo Manager") as a heading
- Number of catalogued **folders** (`folderRepository.count()`)
- Number of catalogued **assets** (`assetRepository.count()`)
- **Last catalog completed** time (`catalog_run_state.last_completed_at`, nullable — shown as "Never" when null)

**Backend:** A new `GET /api/home/stats` endpoint returns a `HomeStats` DTO with `folderCount`, `assetCount`, `lastCatalogCompletedAt` (nullable `Instant`). The implementation lives in `PhotoManagerFacade.getHomeStats()`, which queries `folderRepository.count()`, `assetRepository.count()`, and `catalogRunStateRepository.findById(1)` for `lastCompletedAt`. The controller is a thin `HomeController` in `api/`.

**Frontend:** A standalone `HomeComponent` at `src/app/features/home/`. A `HomeService` in `core/services/` wraps the HTTP call. Stats are displayed as Angular Material cards. When `lastCatalogCompletedAt` is null the template shows "Never".

**Default route:** `{ path: '', redirectTo: 'home', pathMatch: 'full' }` — the root path redirects to `/home`. After login, the redirect target is `returnUrl || '/home'` instead of `/gallery`.

### D5 — Flyway migration V4 for the users table

The `users` table is added in `V4__add_users.sql`. No seed data. Usernames are case-insensitive at the application layer (normalized to lowercase before storage and lookup).

## Risks / Trade-offs

- **JWT is stateless — no revocation:** Logging out only clears the client token. If a token is stolen, it remains valid until expiry. Mitigation: keep expiry short (24h default). A token blocklist can be added later if needed.
- **HS256 shared secret:** Secret must be kept out of version control. It is read from `photomanager.jwt-secret` in `application.yml`, which should be overridden via environment variable in production.
- **localStorage XSS exposure:** Tokens in localStorage are accessible to any same-origin JavaScript. Mitigation: Angular's DomSanitizer and Content-Security-Policy headers (not in scope here) are the standard defence.
- **BCrypt cost factor 12:** Higher than the default (10). Adds ~500ms to login/register. Acceptable for a low-traffic personal app.
- **`logback-spring.xml` overrides all Boot logging config:** The `logging.*` properties in `application.yml` have no effect once a `logback-spring.xml` is present. Any future logging tuning must go in that file.
