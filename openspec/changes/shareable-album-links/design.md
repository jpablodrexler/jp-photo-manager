## Context

The application already has virtual albums (V8 migration): each album is owned by an authenticated user and is accessible only to that user. The `AlbumEntity` / `JpaAlbumRepository` / `AlbumRepository` port chain is in place, and the hexagonal architecture (domain ports, application use cases, infrastructure adapters) is the enforced structure for all new features.

JWT authentication uses an HttpOnly cookie. `SecurityConfig` uses `.requestMatchers(...).permitAll()` for a specific list of paths before the `.requestMatchers("/api/**").authenticated()` catch-all. `GET /api/assets/{id}/thumbnail` and `GET /api/assets/{id}/image` are currently authenticated; the public album view must use only the already-public `/api/albums/shared/{token}` endpoint — it cannot reuse asset image endpoints without also exposing them publicly.

The frontend is Angular 19 with standalone components and lazy routes. `app.component.html` renders the top navigation bar unconditionally (or conditionally on `authService.isLoggedIn()`). The public shared-album page must render without this nav shell.

## Goals / Non-Goals

**Goals:**

- Allow an album owner to generate a sharable, opaque UUID token for any of their albums.
- Allow anyone with the token URL to view the album's assets in a read-only page without authentication.
- Support optional token expiry; expired tokens return a distinct `410 Gone` response.
- The public endpoint must be reachable without a JWT cookie.
- Keep the existing album privacy intact: authenticated endpoints for album CRUD remain unchanged.

**Non-Goals:**

- Revoking or listing active share tokens from the UI (tokens can be deleted directly via a future "manage shares" feature; for now, `ON DELETE CASCADE` on album deletion is sufficient cleanup).
- Password-protecting a shared album link.
- Per-asset permissions within a shared album.
- Exposing full-resolution images publicly (public view returns only the paginated asset metadata via the share token; thumbnail/image endpoints remain authenticated in this iteration).
- SSE progress updates over the public route.

## Decisions

### 1. Opaque UUID token stored in `shared_albums` — not a signed JWT

**Decision:** The share token is a randomly generated `UUID` stored as a row in the `shared_albums` table. The public endpoint looks up the row by token, checks `expires_at`, and then fetches the album's assets.

**Rationale:** A database-backed token allows instant revocation (delete the row) and lets the server enforce expiry without embedding claims in the token itself. A UUID v4 provides 122 bits of entropy, which is sufficient for a non-guessable URL token. A signed JWT would be self-contained but cannot be revoked before its claim expiry without maintaining a blocklist — adding the same persistence cost anyway.

**Alternative:** HMAC-signed opaque token (e.g. `Base64URL(albumId + expiresAt + HMAC)`). Rejected: more implementation complexity with no benefit over a DB row for this use case.

### 2. `GET /api/albums/shared/{token}` returns album metadata + paginated assets — no separate asset proxy

**Decision:** The single public endpoint returns the album's `name`, `description`, and a `PaginatedResult<AssetDto>` where each `AssetDto` contains the same `thumbnailUrl` and `imageUrl` fields as the authenticated gallery, but the underlying image-serving endpoints (`/api/assets/{id}/thumbnail` and `/api/assets/{id}/image`) remain authenticated.

**Rationale:** Keeping image endpoints authenticated prevents unauthenticated bulk access to all catalogued files — exposure is limited to assets in shared albums, per token. The `SharedAlbumComponent` renders thumbnails using `<img [src]="asset.thumbnailUrl">`. Because the Angular SPA is a same-origin client (or uses the proxy), the browser will send the `jwt` cookie automatically if the user happens to be logged in; if not, the images will return 401. For a fully public experience, a future `thumbnail-http-cache` change can open those endpoints with token-scoped authorization.

**Alternative:** Add a token-scoped asset proxy endpoint `GET /api/albums/shared/{token}/assets/{id}/thumbnail`. Deferred to a follow-up change; it adds significant scope for this iteration.

### 3. `SharedAlbumController` — separate controller from `AlbumController`

**Decision:** Two distinct `@RestController` classes: the existing `AlbumController` (all authenticated album CRUD under `/api/albums`) and a new `SharedAlbumController` (public `POST /api/albums/{id}/share` + public `GET /api/albums/shared/{token}`).

**Rationale:** `AlbumController` uses `SecurityContextHolder` to resolve the current user; adding a `permitAll()` method to the same controller risks accidentally removing auth from other methods if the request matcher ordering changes. A dedicated controller makes the public surface explicit and keeps `SecurityConfig` changes minimal.

**Alternative:** Add both endpoints to `AlbumController` with per-method `@PreAuthorize` annotations. Rejected: the project does not currently use method-level security; introducing it only for this case is inconsistent.

### 4. Public route `/s/:token` bypasses `app.component` navigation shell

**Decision:** The `/s/:token` route is registered as a top-level lazy route in `app.routes.ts` without `canActivate: [authGuard]`. `SharedAlbumComponent` is a full-page standalone component that does not nest inside the `app.component` shell. `app.component.html` wraps `<router-outlet>` inside a conditional block that shows the navigation bar only when `authService.isLoggedIn()` returns true, so unauthenticated users visiting `/s/:token` will see only the album content.

**Rationale:** The existing `app.component.html` already conditionally renders the nav bar based on authentication state (from the `auth` feature slice). No structural change to the shell is needed; the public route naturally gets a bare layout because the user is not authenticated.

**Alternative:** Use a secondary `RouterOutlet` with a layout component hierarchy. Rejected: over-engineering for a single public route.

### 5. `ShareAlbumUseCaseImpl` enforces album ownership before generating a token

**Decision:** `ShareAlbumUseCaseImpl.share(albumId, userId, expiresAt)` calls `AlbumRepository.findByIdAndUserId(albumId, userId)` first. If the album does not exist or does not belong to the calling user, it throws `AlbumNotFoundException`, which the `GlobalExceptionHandler` maps to `404 Not Found`.

**Rationale:** The use case must not allow a user to generate a public share token for another user's album. Reusing `AlbumRepository.findByIdAndUserId` (already defined for the existing album use cases) enforces this with zero additional DB queries and keeps the ownership check consistent across all album operations.

### 6. `expires_at` is nullable — tokens without expiry are perpetual

**Decision:** The `expires_at` column is `TIMESTAMPTZ NULL`. When `POST /api/albums/{id}/share` is called without an `expiresAt` field in the request body, the token is created with `expires_at = NULL` and never expires on its own. `GetSharedAlbumUseCaseImpl` only checks expiry when `expires_at IS NOT NULL`.

**Rationale:** Requiring an expiry forces owners to think about a time horizon for every share, adding friction for casual use. Perpetual tokens are fine for a personal photo manager; users who want expiry can supply it. A future "manage shares" page can list and delete tokens manually.

## Risks / Trade-offs

- **Authenticated image endpoints:** As noted in Decision 2, thumbnails and full images are still authenticated. A user who is not logged in visiting `/s/:token` will see broken image placeholders unless they are also logged in. This is an accepted limitation for the current iteration; it will be resolved by adding a token-scoped image proxy in a follow-up change.
- **No rate limiting on public endpoint:** `GET /api/albums/shared/{token}` is `permitAll()` and has no rate limiting. A valid token URL could be crawled to enumerate asset metadata. The `api-rate-limiting` improvement in the backlog should cover this before production.
- **Token not revocable from the UI:** In this iteration there is no "Revoke" button. Deleting the album cascades and removes share rows, but a standalone revoke action requires a future "manage shares" page.
