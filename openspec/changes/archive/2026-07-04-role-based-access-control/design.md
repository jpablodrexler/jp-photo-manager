## Context

The `User` entity has a `role` field of type `Role` enum (currently only `ADMIN`). The JWT issued on login includes the role as an authority (`ROLE_ADMIN`). `SecurityConfig` already provides the JWT filter chain. `@EnableMethodSecurity` with `@PreAuthorize` annotations on service methods or controllers is the standard Spring Security approach for fine-grained access control.

The `VIEWER` role can: browse folders, view assets, download assets, view EXIF, rate assets, use search/filter, and view albums.
The `VIEWER` role cannot: catalog, sync, convert, soft-delete, restore, move, copy, bulk-download (too aggressive?), upload, administer users, or access the recycle bin write path.

## Goals / Non-Goals

**Goals:**
- Add `VIEWER` to the `Role` enum
- Enable `@EnableMethodSecurity` in `SecurityConfig`
- Annotate all write-path use cases with `@PreAuthorize("hasRole('ADMIN')")`
- Conditionally hide write-action UI for `VIEWER` users in the frontend

**Non-Goals:**
- A `MODERATOR` or other intermediate role
- Row-level security (asset ownership by user)
- Dynamic role assignment in the UI (user-admin form update only)

## Decisions

### 1. `@PreAuthorize` on use case implementations (not controllers)

**Decision:** Apply `@PreAuthorize` to use case implementations in `application/usecase/` rather than only to controller methods.

**Rationale:** Applying at the use case layer ensures enforcement even if a write path is later called from a non-HTTP context (e.g., a scheduled job calling a use case directly). Defense in depth.

**Alternative considered:** Controller-only `@PreAuthorize`. Easier but less defense-in-depth.

### 2. Frontend role check from JWT claims

**Decision:** `AuthService` exposes a `currentUserRole(): 'ADMIN' | 'VIEWER'` observable derived from the decoded JWT payload. UI components subscribe to this to show/hide write actions.

**Rationale:** The JWT is already stored in an HttpOnly cookie and cannot be read by JavaScript. Use the `/api/auth/me` endpoint (which returns user info including role) to expose the role to the Angular app.

**Alternative considered:** Decode the JWT in the Angular app. Rejected because the JWT is in an HttpOnly cookie and is inaccessible to JavaScript.

### 3. No schema migration

**Decision:** No Flyway migration is needed. The `role` column and existing seeding are sufficient. New `VIEWER` users are created via the existing user-admin form.

**Rationale:** The column already exists. Adding the `VIEWER` enum value is a code-only change.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Missing a write endpoint causes unauthorized access | Medium | Write a comprehensive list of all protected endpoints in the tasks; cover with integration tests |
| Frontend hiding is cosmetic — backend enforcement is the real gate | Low | `@PreAuthorize` on use cases is the real security boundary; frontend hiding is UX-only |
| Existing `ADMIN` users unaffected — no role migration needed | Low | Only new `VIEWER` users are restricted; existing accounts are all `ADMIN` |
