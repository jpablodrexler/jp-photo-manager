## Context

`JwtUtil` issues JWT tokens with a 24-hour lifetime (`photomanager.jwt-expiry-hours`). `JwtAuthenticationFilter` validates the `jwt` HttpOnly cookie on every request. When the token expires, the filter lets the request through unauthenticated, Spring Security returns `401`, and `authInterceptor` immediately clears the session and redirects to `/login` — with no opportunity for silent renewal.

`AuthController.login()` uses `ResponseCookie.from("jwt", token)` with explicit `httpOnly`, `sameSite`, and `maxAge` settings — the same builder pattern can be applied to a second `refreshToken` cookie. The login response already returns `{ username, expiresAt }` to `localStorage`; `AuthService.isLoggedIn()` compares `expiresAt` to `Date.now()`.

`authInterceptor` is an `HttpInterceptorFn` (Angular 19 functional interceptor) that uses `inject()` to access `AuthService` and `Router`. It currently short-circuits to a redirect on any 401 that is not from `/api/auth/login`. The retry logic must be added here before the redirect.

The proposal's migration version (V8) conflicts with the virtual-albums migration (V8). The correct version for refresh tokens is **V9**.

## Goals / Non-Goals

**Goals:**

- Issue a long-lived (30-day) refresh token at login in a second HttpOnly cookie.
- Rotate the refresh token on every successful use (one-time-use tokens).
- Silently re-issue a new JWT (and new refresh token) without user interaction.
- Proactively refresh the JWT five minutes before its recorded `expiresAt` timestamp.
- Revoke all refresh tokens for a user on logout.
- Protect `/api/auth/refresh` from unauthenticated access (no JWT required, refresh cookie required).

**Non-Goals:**

- Refresh token families / breach detection (detecting stolen tokens from reuse of a revoked token).
- Per-device token management or token listing UI.
- Sliding expiry on the refresh token itself.
- PKCE or OAuth flows.

## Decisions

### 1. Refresh tokens stored in the database — not as signed JWTs

**Decision:** Refresh tokens are cryptographically random 256-bit values stored in a `refresh_tokens` table. Each row has `user_id` (FK), `token` (TEXT UNIQUE), `expires_at`, and `revoked` (BOOLEAN). `RefreshTokenServiceImpl` uses `SecureRandom` + `Base64.getUrlEncoder().encodeToString()` to generate tokens.

**Rationale:** Database-backed tokens support immediate revocation on logout without needing a blocklist or waiting for expiry. If a refresh token is compromised, calling `revokeAllForUser` terminates all sessions instantly. Signed JWTs as refresh tokens cannot be revoked without a blocklist, which negates the stateless advantage.

### 2. Rotate on every use — mark old as `revoked = true`, insert new

**Decision:** `RefreshTokenServiceImpl.validateAndRotate(String tokenValue)` loads the token, checks `revoked` and `expiresAt`, marks it `revoked = true`, inserts a new token row, and returns the new token value in one `@Transactional` method.

**Rationale:** Atomic rotation inside a transaction prevents a race condition where two concurrent refresh attempts both see the same valid token. Keeping the revoked row (rather than deleting it) preserves an audit trail and enables future replay-attack detection.

### 3. `refreshToken` cookie scoped to `Path=/api/auth/refresh`

**Decision:** The `refreshToken` cookie is issued with `Path=/api/auth/refresh` so the browser only sends it to that single endpoint, not to every `/api/**` request. The `jwt` cookie keeps `Path=/`.

**Rationale:** Limiting the cookie's path reduces its exposure surface. Even if XSS were somehow possible, a script cannot trigger the refresh cookie being sent to arbitrary endpoints.

### 4. `POST /api/auth/refresh` added to SecurityConfig permit-all list — no JWT required

**Decision:** `SecurityConfig` adds `/api/auth/refresh` to the `.requestMatchers(...).permitAll()` clause so the refresh endpoint is reachable when the JWT has expired. The endpoint validates only the `refreshToken` cookie.

**Rationale:** The endpoint's entire purpose is to accept a request with an expired JWT and issue a new one. Requiring a valid JWT would make the endpoint unreachable in the exact scenario it must serve.

### 5. Angular interceptor retries once after a successful refresh — no infinite loop

**Decision:** `authInterceptor` catches a 401. If the URL is not `/api/auth/login` or `/api/auth/refresh`, it calls `authService.refresh()`. On success it retries the original request once via `switchMap(() => next(req.clone()))`. If `refresh()` itself fails (returns 401 or error), it calls `clearSession()` and navigates to `/login`. The refresh URL exclusion prevents the interceptor from trying to refresh an already-failed refresh attempt.

**Rationale:** Excluding the refresh URL from retry prevents a stack-overflow-style loop. The single retry is sufficient because the fresh JWT is immediately valid.

### 6. Proactive refresh via `setTimeout` in `AuthService`

**Decision:** After every successful login or refresh, `AuthService.scheduleProactiveRefresh()` sets a `setTimeout` that fires five minutes before the `expiresAt` timestamp stored in `localStorage`. On fire, it calls `this.refresh()`. The timer is cleared and reset on every new login/refresh.

**Rationale:** Proactive refresh eliminates the case where the user is mid-action (uploading, browsing) when the JWT expires. The five-minute window is well within the 24-hour JWT lifetime and gives enough runway for the refresh request to complete before the token is rejected.

### 7. `RefreshTokenService.issueRefreshToken(String username)` — controller stays thin

**Decision:** `RefreshTokenServiceImpl.issueRefreshToken(String username)` injects `UserRepository` to resolve the `User` by username before creating the token row. `AuthController` passes the username (already known from the login request) and receives the raw token string back to set the cookie.

**Rationale:** This keeps `AuthController` free of repository dependencies. The service owns the full lifecycle of the token row, consistent with the existing pattern where `JwtUtil` owns JWT generation without the controller touching signing keys.

## Data Flow

```
POST /api/auth/login (username, password)
  → AuthController.login()
    → userService.authenticate(username, password)  → JWT string
    → refreshTokenService.issueRefreshToken(username) → refresh token string
    → Set-Cookie: jwt=<token>; HttpOnly; Path=/; SameSite=Strict; MaxAge=86400
    → Set-Cookie: refreshToken=<rt>; HttpOnly; Path=/api/auth/refresh; SameSite=Strict; MaxAge=2592000
    → 200 OK { username, expiresAt }

  AuthService.storeSession(username, expiresAt)
  AuthService.scheduleProactiveRefresh()  → setTimeout fires 5 min before expiresAt

--- JWT expires or setTimeout fires ---

POST /api/auth/refresh
  → AuthController.refresh()
    → reads refreshToken cookie from HttpServletRequest
    → refreshTokenService.validateAndRotate(tokenValue)
      → load token row, check revoked + expiresAt → 401 if invalid
      → mark revoked = true, insert new token row (atomic @Transactional)
      → return new token value
    → jwtTokenService.generateToken(username)
    → Set-Cookie: jwt=<newToken>; ...
    → Set-Cookie: refreshToken=<newRt>; ...
    → 200 OK { username, expiresAt }

  AuthService.storeSession() + scheduleProactiveRefresh()

--- Any request returns 401 (JWT expired, refresh not yet triggered) ---

authInterceptor catches 401
  → authService.refresh()         → POST /api/auth/refresh
    → on success: retry original request
    → on failure: clearSession() + navigate('/login')

POST /api/auth/logout
  → AuthController.logout()
    → reads refreshToken cookie
    → refreshTokenService.revokeAllForUser(username) → deletes all token rows for user
    → Set-Cookie: jwt=; MaxAge=0
    → Set-Cookie: refreshToken=; Path=/api/auth/refresh; MaxAge=0
    → 200 OK
```

## File Change List

**New files:**

- `backend/.../db/migration/V9__add_refresh_tokens.sql`
- `backend/.../domain/entity/RefreshToken.java`
- `backend/.../domain/repository/RefreshTokenRepository.java`
- `backend/.../domain/service/RefreshTokenService.java`
- `backend/.../infrastructure/service/RefreshTokenServiceImpl.java`
- `backend/.../test/.../AuthControllerRefreshTest.java` — `@WebMvcTest` for refresh and logout endpoints
- `backend/.../test/.../RefreshTokenServiceTest.java` — unit tests for `validateAndRotate` and revocation

**Modified files:**

- `backend/.../api/AuthController.java` — inject `RefreshTokenService`; update `login()` to issue refresh cookie; add `refresh()` handler; update `logout()` to revoke tokens and clear both cookies
- `backend/.../config/SecurityConfig.java` — add `/api/auth/refresh` to permit-all
- `backend/.../resources/application.yml` — add `photomanager.refresh-token-expiry-days: 30`
- `frontend/src/app/core/services/auth.service.ts` — add `refresh()`, `scheduleProactiveRefresh()`, timer management
- `frontend/src/app/core/interceptors/auth.interceptor.ts` — replace redirect-only 401 handling with refresh-then-retry logic
