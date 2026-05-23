## Why

Users currently have no visibility into or control over their active sessions. A stolen refresh token grants persistent access with no way for the user to revoke it. Exposing active sessions (with device hints) and allowing targeted or bulk revocation gives users the tools to respond to a suspected account compromise.

## What Changes

- A Flyway migration adds an optional `user_agent` VARCHAR column to `refresh_tokens`
- `GET /api/auth/sessions` returns all active sessions with device hint and last-used time
- `DELETE /api/auth/sessions/{id}` revokes a single session
- `DELETE /api/auth/sessions` revokes all sessions except the current one ("sign out everywhere")
- A new `/profile/sessions` frontend page lists sessions in a `MatTable` with per-row revoke and a "sign out everywhere" button

## Capabilities

### New Capabilities

- `session-management`: Users can view all active sessions (with device hint derived from User-Agent), revoke individual sessions, and sign out of all other sessions from the `/profile/sessions` page.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V22__add_user_agent_to_refresh_tokens.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/auth/GetActiveSessionsUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/auth/RevokeSessionUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AuthController.java` — new session endpoints
- `JPPhotoManagerWeb/backend/src/test/` — tests for session listing and revocation
- `JPPhotoManagerWeb/frontend/src/app/features/profile/sessions/sessions.component.ts` — new standalone component
- `JPPhotoManagerWeb/frontend/src/app/app.routes.ts` — add `/profile/sessions` route
