## 1. Database migration

- [x] 1.1 Create `V22__add_user_agent_to_refresh_tokens.sql`: `ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(512) NULL, ADD COLUMN last_used_at TIMESTAMP NULL` — **Note:** used `TIMESTAMPTZ` instead of `TIMESTAMP` for `last_used_at` to match the existing `expires_at`/`issued_at` columns on the same table. **Also renumbered to `V34__add_user_agent_to_refresh_tokens.sql`** (database-review finding): the highest migration already present on this branch is `V33__recreate_asset_exif.sql`; `V22` sorts *before* that on any database that has already applied `V33` (e.g. a persistent local/staging Postgres instance), and Flyway's default `outOfOrder=false` (see `application.yml`) means such a database would fail validation/refuse to apply this migration. `V34` is the correct next-sequential version.
- [x] 1.2 Update token-issuing logic to populate `user_agent` and `last_used_at` on token creation; update `last_used_at` on each refresh

## 2. Domain — Use case interfaces and DTO

- [x] 2.1 Create `domain/port/in/auth/GetActiveSessionsUseCase.java` returning `List<SessionInfo>`
- [x] 2.2 Create `domain/model/SessionInfo.java` record: `long id`, `String deviceHint`, `Instant lastUsedAt`, `boolean current`
- [x] 2.3 Create `domain/port/in/auth/RevokeSessionUseCase.java` — **Note:** signature adapted to the codebase's existing convention (`GetCurrentUserUseCaseImpl`, `LogoutUseCaseImpl`) of resolving the current user from `SecurityContextHolder` inside the use case rather than the controller passing a `userId`; `User.id` is a `UUID` in this codebase, not `long`. Final signature: `void revokeOne(long sessionId)` and `void revokeAllOthers(String currentRefreshTokenValue)` (the current session is identified by the raw refresh-token cookie value, the only thing the controller has — there is no client-visible "current session id" until the list is loaded).

## 3. Application — Use case implementations

- [x] 3.1 Implement `GetActiveSessionsUseCaseImpl`: query non-expired tokens by userId; compute `deviceHint` with a helper that parses User-Agent (look for Chrome/Firefox/Safari/Mobile keywords); mark `current = true` for the token matching the current refresh token hash — **Note:** "hash" in the task description doesn't match this codebase: refresh tokens are stored as the raw random value (see `RefreshTokenIssuer`), not a hash, so `current` is computed by direct value comparison against the `refreshToken` cookie, consistent with how `RefreshTokenRepository.findByToken` already works.
- [x] 3.2 Implement `RevokeSessionUseCaseImpl`: `revokeOne()` finds by id + userId (throws `SessionNotFoundException`, this codebase's `*NotFoundException` naming convention, mapped to 404 in `GlobalExceptionHandler` — no `ResourceNotFoundException` class exists in this codebase); `revokeAllOthers()` deletes all tokens for the user except the one matching the current refresh token value

## 4. HTTP adapter

- [x] 4.1 Add `GET /api/auth/sessions` to `AuthController` returning `List<SessionResponseDto>`
- [x] 4.2 Add `DELETE /api/auth/sessions/{id}` returning `204 No Content`
- [x] 4.3 Add `DELETE /api/auth/sessions` returning `204 No Content`
- [x] 4.4 All endpoints require authentication (no extra role restriction — any authenticated user manages their own sessions) — already covered by the existing `.requestMatchers("/api/**").authenticated()` catch-all in `SecurityConfig`; no change needed there.

## 5. Backend unit tests

- [x] 5.1 Test that `GetActiveSessionsUseCaseImpl` returns only non-expired tokens for the current user
- [x] 5.2 Test that `current: true` is set correctly for the matching session
- [x] 5.3 Test that `revokeOne()` throws `SessionNotFoundException` when the session does not belong to the user (this codebase's `*NotFoundException` convention; see note on 3.2)
- [x] 5.4 Test that `revokeAllOthers()` deletes all tokens except the current one

## 6. Frontend — SessionsComponent

- [x] 6.1 Create `features/profile/sessions/sessions.component.ts` as a standalone component
- [x] 6.2 Add `getSessions(): Observable<SessionInfo[]>` and `revokeSession(id: number): Observable<void>` and `revokeAllOtherSessions(): Observable<void>` to `AuthService`
- [x] 6.3 Render sessions in a `MatTable` with columns: Device, Last Used, Actions (Revoke button disabled for `current: true`)
- [x] 6.4 Add "Sign out everywhere" `MatButton` above the table; prompt with `MatDialog` confirmation before calling `revokeAllOtherSessions()`
- [x] 6.5 Register lazy route `/profile/sessions` in `app.routes.ts` — **Note:** also added a "Sessions" nav entry (mobile menu + desktop toolbar icon button) in `app.component.html` so the page is actually reachable from the UI, matching how every other routed feature has a nav entry.

## 7. Testing and Commit

- [x] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test` — 710 tests run, 0 failures, 0 errors, 2 skipped (pre-existing, unrelated to this change); count grew from 706 to 710 across the code-review, database-review, and security-review fix-and-recheck rounds (`mvn clean test` re-verified after each).
- [x] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test` — 375 tests across 43 spec files, all passing.
- [ ] 7.3 Commit all changes (only after both test suites pass) — **Note:** intentionally left undone. The orchestrating instructions for this session explicitly forbid any `git commit`/`git push` at any point in this workflow; committing is left to the user/caller after review.
