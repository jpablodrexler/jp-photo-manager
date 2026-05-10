# refresh-token

Specifies how the application issues, rotates, and revokes long-lived refresh tokens so authenticated users are silently re-issued a new JWT without being redirected to the login screen when their access token expires.

---

### Requirement: A refresh token cookie is issued at login alongside the JWT

The `POST /api/auth/login` endpoint SHALL set two HttpOnly cookies on success: the existing short-lived `jwt` cookie (24-hour lifetime, `Path=/`) and a new `refreshToken` cookie (30-day lifetime, `Path=/api/auth/refresh`, `SameSite=Strict`). The `refreshToken` value SHALL be a cryptographically random 256-bit token stored as a row in the `refresh_tokens` table. The login response body is unchanged.

#### Scenario: Successful login issues both cookies

- **GIVEN** valid credentials are submitted
- **WHEN** the client calls `POST /api/auth/login`
- **THEN** the response is `200 OK`; the `Set-Cookie` headers include both `jwt` (HttpOnly, Path=/, MaxAge=86400) and `refreshToken` (HttpOnly, Path=/api/auth/refresh, MaxAge=2592000); the `refresh_tokens` table has a new row for the user with `revoked = false` and `expires_at` approximately 30 days in the future

#### Scenario: Failed login does not issue a refresh token

- **GIVEN** invalid credentials are submitted
- **WHEN** the client calls `POST /api/auth/login`
- **THEN** the response is `401 Unauthorized`; no `refreshToken` cookie is set; no row is inserted into `refresh_tokens`

---

### Requirement: The refresh endpoint silently re-issues both tokens

`POST /api/auth/refresh` SHALL accept the `refreshToken` HttpOnly cookie, validate it against the database (not revoked, not expired), atomically mark it revoked and insert a new token row, generate a new JWT, and return both new cookies. The endpoint SHALL NOT require a valid `jwt` cookie — it must be reachable when the JWT has expired. The response body SHALL be the same `{ username, expiresAt }` shape as `POST /api/auth/login`.

#### Scenario: Valid refresh token re-issues both cookies

- **GIVEN** the user has a valid, non-revoked `refreshToken` cookie
- **WHEN** the client calls `POST /api/auth/refresh`
- **THEN** the response is `200 OK`; new `jwt` and `refreshToken` cookies are set with fresh lifetimes; the old refresh token row in the database has `revoked = true`; a new refresh token row exists for the user

#### Scenario: Expired refresh token is rejected

- **GIVEN** the user's `refreshToken` cookie references a row with `expires_at` in the past
- **WHEN** the client calls `POST /api/auth/refresh`
- **THEN** the response is `401 Unauthorized`; no new cookies are set; no new token row is inserted

#### Scenario: Revoked refresh token is rejected

- **GIVEN** a previously rotated refresh token (marked `revoked = true`) is submitted
- **WHEN** the client calls `POST /api/auth/refresh`
- **THEN** the response is `401 Unauthorized`; no new cookies are set

#### Scenario: Missing refresh token cookie is rejected

- **GIVEN** no `refreshToken` cookie is present in the request
- **WHEN** the client calls `POST /api/auth/refresh`
- **THEN** the response is `401 Unauthorized`

#### Scenario: Refresh endpoint accessible without a valid JWT

- **GIVEN** the user's `jwt` cookie is absent or expired
- **WHEN** the client calls `POST /api/auth/refresh` with a valid `refreshToken` cookie
- **THEN** the response is `200 OK` (Spring Security does not block the request based on the missing JWT)

---

### Requirement: The interceptor retries failed requests after a successful refresh

When an API call returns `401 Unauthorized` and the URL is not `/api/auth/login` or `/api/auth/refresh`, the `authInterceptor` SHALL call `POST /api/auth/refresh` before redirecting to the login screen. If the refresh succeeds, the interceptor SHALL retry the original request exactly once with the new JWT cookie. If the refresh fails, the interceptor SHALL clear the session and navigate to `/login`.

#### Scenario: Expired JWT triggers silent retry

- **GIVEN** the user's `jwt` cookie has just expired during an active session
- **WHEN** the frontend makes any authenticated API call and receives `401`
- **THEN** the interceptor calls `POST /api/auth/refresh`; on success the original request is retried transparently; the user sees no redirect and no error

#### Scenario: Refresh failure redirects to login

- **GIVEN** both the `jwt` and `refreshToken` cookies are invalid
- **WHEN** the frontend makes an authenticated API call and receives `401`, then `POST /api/auth/refresh` also returns `401`
- **THEN** the interceptor calls `authService.clearSession()` and navigates to `/login`

#### Scenario: Refresh endpoint itself is not retried

- **GIVEN** `POST /api/auth/refresh` returns `401`
- **WHEN** the interceptor processes the response
- **THEN** the interceptor does NOT call `POST /api/auth/refresh` again (no infinite loop); it proceeds directly to `clearSession()` + navigate

---

### Requirement: The frontend proactively refreshes the JWT before it expires

`AuthService.scheduleProactiveRefresh()` SHALL set a timer that fires five minutes before the `expiresAt` timestamp stored in `localStorage`. On fire, it SHALL call `POST /api/auth/refresh` silently. The timer SHALL be rescheduled after every successful login or refresh. The timer SHALL be cancelled when `clearSession()` is called.

#### Scenario: Proactive refresh fires before expiry

- **GIVEN** the user logged in and `expiresAt` is 24 hours from now
- **WHEN** the system clock reaches five minutes before `expiresAt`
- **THEN** `POST /api/auth/refresh` is called automatically; `expiresAt` in `localStorage` is updated; a new timer is scheduled for the new expiry

#### Scenario: Timer is cancelled on logout

- **GIVEN** a proactive refresh timer is pending
- **WHEN** the user calls `authService.clearSession()`
- **THEN** the pending timer is cancelled and no refresh request is issued after logout

---

### Requirement: Logout revokes all refresh tokens for the user

`POST /api/auth/logout` SHALL call `RefreshTokenService.revokeAllForUser(username)` to delete all `refresh_tokens` rows for the authenticated user, in addition to clearing both the `jwt` and `refreshToken` cookies.

#### Scenario: Logout invalidates refresh token

- **GIVEN** the user has one active refresh token in the database
- **WHEN** the client calls `POST /api/auth/logout`
- **THEN** the response is `200 OK`; both the `jwt` and `refreshToken` cookies are cleared (MaxAge=0); the `refresh_tokens` table has no rows for the user; a subsequent `POST /api/auth/refresh` with the old cookie value returns `401 Unauthorized`

---

### Requirement: Deleting a user cascades to their refresh tokens

The `refresh_tokens.user_id` foreign key SHALL have `ON DELETE CASCADE` referencing `users.id`. When a user is deleted via `DELETE /api/admin/users/{id}`, all their refresh token rows SHALL be automatically removed by the database.

#### Scenario: User deletion removes refresh tokens

- **GIVEN** a user has two active refresh token rows in `refresh_tokens`
- **WHEN** `DELETE /api/admin/users/{id}` removes the user
- **THEN** both refresh token rows are deleted by the cascade; no orphaned rows remain in `refresh_tokens`
