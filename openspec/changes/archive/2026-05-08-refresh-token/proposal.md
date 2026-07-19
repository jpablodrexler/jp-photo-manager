## Why

JWT tokens issued by `JwtUtil` expire after 24 hours with no renewal path. When an active user's token expires mid-session — during a long catalog run, while browsing a large folder, or simply between morning and evening — the `JwtAuthenticationFilter` rejects their next request, the `AuthInterceptor` catches the 401, and the browser is immediately redirected to `/login`. All in-flight state (current folder, open viewer, active upload) is lost. Because the `jwt` cookie is `HttpOnly` the frontend cannot read the token's expiry claim and proactively warn the user.

A long-lived refresh token stored in a second `HttpOnly` cookie solves this without weakening the short-lived JWT's security properties. The JWT remains 24 hours (kept short to limit blast radius on token compromise); the refresh token lasts 30 days and is rotated on every use to prevent replay attacks. An active user is silently re-issued a new JWT without being redirected to the login screen.

## What Changes

- Add a `refresh_tokens` PostgreSQL table (Flyway `V8__add_refresh_tokens.sql`) with per-token revocation support.
- Add a `RefreshToken` JPA entity and `RefreshTokenRepository` (`findByToken`, `deleteByUser_Id`) in the domain layer.
- Add a `RefreshTokenService` interface in `domain/service/` with `issueRefreshToken(User)`, `validateAndRotate(String)`, and `revokeAllForUser(User)`.
- Implement `RefreshTokenServiceImpl` in `infrastructure/service/` generating cryptographically random tokens, enforcing expiry and revocation, and rotating tokens atomically.
- Update `POST /api/auth/login` to issue a second `refreshToken` HttpOnly cookie (30-day, `SameSite=Strict`, `Path=/api/auth/refresh`) alongside the existing `jwt` cookie.
- Add `POST /api/auth/refresh` in `AuthController`: reads the `refreshToken` cookie, calls `validateAndRotate()`, and re-issues both cookies with new values.
- Update `POST /api/auth/logout` to call `revokeAllForUser()` and clear both the `jwt` and `refreshToken` cookies.
- Update `AuthInterceptor` to intercept 401 responses, attempt `POST /api/auth/refresh`, and retry the original request on success; redirect to `/login` only on refresh failure.
- Update `AuthService` to proactively call `POST /api/auth/refresh` five minutes before the stored `expiresAt` timestamp elapses.

## Capabilities

### New Capabilities

- `refresh-token`: Issue, rotate, and revoke long-lived refresh tokens so authenticated users are silently re-issued a new JWT without being redirected to the login screen when their access token expires.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`V8__add_refresh_tokens.sql`** *(new)*: creates the `refresh_tokens` table (`token_id BIGSERIAL PK`, `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`, `token TEXT NOT NULL UNIQUE`, `issued_at TIMESTAMP NOT NULL DEFAULT NOW()`, `expires_at TIMESTAMP NOT NULL`, `revoked BOOLEAN NOT NULL DEFAULT FALSE`).
- **`RefreshToken.java`** *(new)*: JPA `@Entity` in `domain/entity/` with `@ManyToOne(fetch = LAZY)` link to `User`, plus `token`, `issuedAt`, `expiresAt`, and `revoked` fields.
- **`RefreshTokenRepository.java`** *(new)*: Spring Data JPA interface in `domain/repository/` with `Optional<RefreshToken> findByToken(String token)` and `void deleteByUser_Id(UUID userId)`.
- **`RefreshTokenService.java`** *(new)*: interface in `domain/service/` declaring `issueRefreshToken(User)`, `validateAndRotate(String)`, `revokeAllForUser(User)`.
- **`RefreshTokenServiceImpl.java`** *(new)*: implementation in `infrastructure/service/` using `SecureRandom` + Base64, 30-day expiry, atomic rotate-on-use (mark revoked, insert new).
- **`AuthController.java`**: updated `login()` to set `refreshToken` cookie; updated `logout()` to revoke and clear `refreshToken` cookie; new `refresh()` handler for `POST /api/auth/refresh`.
- **`SecurityConfig.java`**: add `POST /api/auth/refresh` to the permit-all list so the endpoint is reachable without a valid JWT cookie.
- **`auth.service.ts`**: new `refresh()` method calling `POST /api/auth/refresh`; new `scheduleProactiveRefresh()` that sets a `setTimeout` for five minutes before `expiresAt`; `login()` and the new `refresh()` both call `scheduleProactiveRefresh()` after storing the session.
- **`auth.interceptor.ts`**: changed 401 handling from "redirect immediately" to "call `authService.refresh()` → retry once → redirect on failure"; guard against infinite retry loops with a `_retry` flag on the cloned request.
- **`auth.model.ts`** *(new or update)*: ensure `AuthSession` interface includes `username: string` and `expiresAt: string` (ISO-8601).
- **No breaking API changes** — existing `POST /api/auth/login` and `POST /api/auth/logout` contracts are preserved; only new cookies are added.
