# Feature 46 — session-management

**As a** signed-in user, **I want** to see and revoke my active sessions, **so that** I can sign out a lost device or any session I do not recognise.

## Acceptance Criteria
- AC1: Given I open /profile/sessions, when it loads, then my active sessions are listed with a device hint and last-used time.
- AC2: Given a session, when I revoke it, then it is invalidated.
- AC3: Given another user's session, when I try to revoke it, then it is refused.
- AC4: Given "sign out everywhere", when I confirm, then all my other sessions are revoked and the current one stays.

## Technical notes

The `refresh_tokens` table already stores `userId`, `tokenHash`, and `expiresAt`; add an optional `user_agent` column and expose `GET /api/auth/sessions` (list active sessions with device hint and last-used time), `DELETE /api/auth/sessions/{id}` (revoke one), and `DELETE /api/auth/sessions` (revoke all others); frontend `/profile/sessions` page lists sessions in a `MatTable` with a revoke button per row and a "sign out everywhere" action

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
