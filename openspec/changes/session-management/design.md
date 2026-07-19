## Context

The `refresh_tokens` table already has `id`, `userId`, `tokenHash`, and `expiresAt`. The `user_agent` column is optional (nullable) and populated when the token is issued. Sessions are identified by their `refresh_tokens.id`. The current user's session is identified by matching the hash of the refresh token in the current HttpOnly cookie.

## Goals / Non-Goals

**Goals:**
- Flyway V22: `ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(512) NULL`
- Store `User-Agent` header in `refresh_tokens.user_agent` when issuing a new refresh token
- `GET /api/auth/sessions` returns non-expired tokens for the authenticated user: `[ { id, deviceHint, lastUsedAt, current: bool } ]`; `deviceHint` is a simplified parser of the User-Agent (e.g., "Chrome on macOS", "Mobile Firefox")
- `DELETE /api/auth/sessions/{id}` deletes a single token by ID (must belong to the current user)
- `DELETE /api/auth/sessions` deletes all refresh tokens for the user except the current one
- Frontend `SessionsComponent` renders a `MatTable` with `Device`, `Last Used`, and `Actions` columns

**Non-Goals:**
- Geo-IP location lookup for sessions
- Session activity timeline beyond `lastUsedAt`
- Force logout of other sessions on password change (a separate security hardening concern)

## Decisions

### 1. `lastUsedAt` updated on every token refresh

**Decision:** Add `lastUsedAt TIMESTAMP` to `refresh_tokens` (same migration as `user_agent`). Update it each time the token is used to refresh a JWT.

**Rationale:** Without `lastUsedAt`, all sessions show the creation time, making it impossible to identify inactive sessions for cleanup.

### 2. `deviceHint` computed server-side

**Decision:** Parse the `User-Agent` string into a short human-readable string server-side. A simple heuristic (look for `Chrome`, `Firefox`, `Safari`, `Mobile`) is sufficient; full UAP parsing is not required.

**Rationale:** Returning the raw User-Agent string would expose overly verbose technical data. A short hint ("Chrome on Windows") is more actionable for users.

### 3. Revocation by ID with ownership check

**Decision:** `DELETE /api/auth/sessions/{id}` verifies that `refresh_tokens.userId = currentUser.id` before deleting. Return 404 if the token does not belong to the user.

**Rationale:** Prevents users from revoking other users' sessions by guessing IDs.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| User revokes their own current session by accident | Low | The `current: true` flag in the list response lets the UI warn the user |
| `DELETE /api/auth/sessions` leaves the current session intact | Low | Documented behavior: "sign out everywhere else"; the user retains their own session |
