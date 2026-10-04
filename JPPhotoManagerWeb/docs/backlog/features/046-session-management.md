# Feature 46 — session-management

The `refresh_tokens` table already stores `userId`, `tokenHash`, and `expiresAt`; add an optional `user_agent` column and expose `GET /api/auth/sessions` (list active sessions with device hint and last-used time), `DELETE /api/auth/sessions/{id}` (revoke one), and `DELETE /api/auth/sessions` (revoke all others); frontend `/profile/sessions` page lists sessions in a `MatTable` with a revoke button per row and a "sign out everywhere" action
