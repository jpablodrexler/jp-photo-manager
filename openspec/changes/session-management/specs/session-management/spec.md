# session-management

Users can view all active sessions (with device hint derived from User-Agent), revoke individual sessions, and sign out of all other sessions from the `/profile/sessions` page.

---

## ADDED Requirements

### Requirement: Active sessions are listed with device hints

`GET /api/auth/sessions` SHALL return all non-expired refresh tokens for the authenticated user, each with a device hint and last-used timestamp.

#### Scenario: User sees their active sessions

- **GIVEN** a user has logged in from a browser on macOS and from a mobile device
- **WHEN** `GET /api/auth/sessions` is called
- **THEN** the response includes two entries, each with `id`, `deviceHint` (e.g., "Chrome on macOS", "Mobile Safari"), `lastUsedAt`, and `current: true/false`

### Requirement: Individual sessions can be revoked

`DELETE /api/auth/sessions/{id}` SHALL delete the specified refresh token. The token must belong to the authenticated user.

#### Scenario: User revokes a single session

- **GIVEN** a user has two active sessions and revokes one by ID
- **WHEN** `DELETE /api/auth/sessions/{sessionId}` is called
- **THEN** the response is `204 No Content` and the revoked session no longer appears in `GET /api/auth/sessions`

#### Scenario: User cannot revoke another user's session

- **GIVEN** a session ID belonging to a different user
- **WHEN** `DELETE /api/auth/sessions/{sessionId}` is called
- **THEN** the response is `404 Not Found`

### Requirement: All other sessions can be revoked at once

`DELETE /api/auth/sessions` SHALL delete all refresh tokens for the authenticated user except the current one.

#### Scenario: User signs out everywhere else

- **GIVEN** a user has 3 active sessions (including the current one)
- **WHEN** `DELETE /api/auth/sessions` is called
- **THEN** the response is `204 No Content` and only the current session remains active; the other 2 sessions are revoked

### Requirement: SessionsComponent displays and manages sessions

The `/profile/sessions` frontend page SHALL display active sessions in a `MatTable` with device hint, last-used time, and a "Revoke" button per row, plus a "Sign out everywhere" button.

#### Scenario: User revokes a session from the sessions page

- **GIVEN** the sessions page is displayed with two rows
- **WHEN** the user clicks "Revoke" on a non-current session row
- **THEN** the row is removed from the table and a `MatSnackBar` confirms "Session revoked"
