## 1. Database migration

- [ ] 1.1 Create `V22__add_user_agent_to_refresh_tokens.sql`: `ALTER TABLE refresh_tokens ADD COLUMN user_agent VARCHAR(512) NULL, ADD COLUMN last_used_at TIMESTAMP NULL`
- [ ] 1.2 Update token-issuing logic to populate `user_agent` and `last_used_at` on token creation; update `last_used_at` on each refresh

## 2. Domain — Use case interfaces and DTO

- [ ] 2.1 Create `domain/port/in/auth/GetActiveSessionsUseCase.java` returning `List<SessionInfo>`
- [ ] 2.2 Create `domain/model/SessionInfo.java` record: `long id`, `String deviceHint`, `Instant lastUsedAt`, `boolean current`
- [ ] 2.3 Create `domain/port/in/auth/RevokeSessionUseCase.java` with `void revokeOne(long sessionId, long currentUserId)` and `void revokeAllOthers(long currentSessionId, long currentUserId)`

## 3. Application — Use case implementations

- [ ] 3.1 Implement `GetActiveSessionsUseCaseImpl`: query non-expired tokens by userId; compute `deviceHint` with a helper that parses User-Agent (look for Chrome/Firefox/Safari/Mobile keywords); mark `current = true` for the token matching the current refresh token hash
- [ ] 3.2 Implement `RevokeSessionUseCaseImpl`: `revokeOne()` finds by id + userId (throw ResourceNotFoundException if not found); `revokeAllOthers()` deletes all tokens for userId where id ≠ currentSessionId

## 4. HTTP adapter

- [ ] 4.1 Add `GET /api/auth/sessions` to `AuthController` returning `List<SessionResponse>`
- [ ] 4.2 Add `DELETE /api/auth/sessions/{id}` returning `204 No Content`
- [ ] 4.3 Add `DELETE /api/auth/sessions` returning `204 No Content`
- [ ] 4.4 All endpoints require authentication (no extra role restriction — any authenticated user manages their own sessions)

## 5. Backend unit tests

- [ ] 5.1 Test that `GetActiveSessionsUseCaseImpl` returns only non-expired tokens for the current user
- [ ] 5.2 Test that `current: true` is set correctly for the matching session
- [ ] 5.3 Test that `revokeOne()` throws ResourceNotFoundException when the session does not belong to the user
- [ ] 5.4 Test that `revokeAllOthers()` deletes all tokens except the current one

## 6. Frontend — SessionsComponent

- [ ] 6.1 Create `features/profile/sessions/sessions.component.ts` as a standalone component
- [ ] 6.2 Add `getSessions(): Observable<SessionInfo[]>` and `revokeSession(id: number): Observable<void>` and `revokeAllOtherSessions(): Observable<void>` to `AuthService`
- [ ] 6.3 Render sessions in a `MatTable` with columns: Device, Last Used, Actions (Revoke button disabled for `current: true`)
- [ ] 6.4 Add "Sign out everywhere" `MatButton` above the table; prompt with `MatDialog` confirmation before calling `revokeAllOtherSessions()`
- [ ] 6.5 Register lazy route `/profile/sessions` in `app.routes.ts`

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
