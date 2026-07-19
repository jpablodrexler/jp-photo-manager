# shareable-album-links

Specifies how authenticated album owners generate opaque share tokens that allow anyone with the link to view an album's assets without authentication, and how the public Angular route renders that album in a minimal read-only layout.

---

## ADDED Requirements

### Requirement: An authenticated album owner can generate a share token for their album

The system SHALL expose `POST /api/albums/{id}/share` requiring a valid JWT cookie. The endpoint SHALL accept an optional `expiresAt` timestamp in the request body. It SHALL resolve the album by `albumId` scoped to the authenticated user; if the album does not exist or belongs to another user the response SHALL be `404 Not Found`. On success it SHALL generate a `UUID` token, persist a row in the `shared_albums` table with `album_id`, `token`, `created_at = NOW()`, and `expires_at` (null when not supplied), and return `201 Created` with a `ShareAlbumResponse` containing the `token` UUID and a `url` string of the form `/s/{token}`.

#### Scenario: Owner generates a share token without expiry

- **GIVEN** the authenticated user owns album with `albumId = 7`
- **WHEN** the client calls `POST /api/albums/7/share` with an empty body `{}`
- **THEN** the response is `201 Created` with a `ShareAlbumResponse` containing a non-null `token` UUID and a `url` matching `/s/{token}`; a row exists in `shared_albums` with `album_id = 7`, `expires_at = NULL`

#### Scenario: Owner generates a share token with an expiry date

- **GIVEN** the authenticated user owns album with `albumId = 7`
- **WHEN** the client calls `POST /api/albums/7/share` with body `{ "expiresAt": "2027-01-01T00:00:00Z" }`
- **THEN** the response is `201 Created`; the `shared_albums` row has `expires_at = 2027-01-01T00:00:00Z`

#### Scenario: Owner attempts to share another user's album

- **GIVEN** album with `albumId = 99` belongs to a different user
- **WHEN** the authenticated user calls `POST /api/albums/99/share`
- **THEN** the response is `404 Not Found`; no row is inserted into `shared_albums`

#### Scenario: Unauthenticated request to share endpoint

- **GIVEN** no valid JWT cookie is present
- **WHEN** a client calls `POST /api/albums/7/share`
- **THEN** the response is `401 Unauthorized`

---

### Requirement: Anyone with a valid, non-expired token can retrieve the album's assets

The system SHALL expose `GET /api/albums/shared/{token}` as a `permitAll()` endpoint (no authentication required). It SHALL look up the `shared_albums` row by `token` UUID. If no matching row exists the response SHALL be `404 Not Found`. If a matching row exists and `expires_at IS NOT NULL` and `expires_at` is in the past the response SHALL be `410 Gone`. Otherwise the response SHALL be `200 OK` with a `SharedAlbumDto` containing the album `name`, `description`, and a paginated `assets` list of `AssetDto` items (page size 50). An optional `page` query parameter (default `0`) SHALL select the page.

#### Scenario: Valid token retrieves album assets

- **GIVEN** a `shared_albums` row exists with `token = "a1b2c3d4-..."` linking to album "Vacation 2025" which contains 10 assets, and `expires_at = NULL`
- **WHEN** an unauthenticated client calls `GET /api/albums/shared/a1b2c3d4-...`
- **THEN** the response is `200 OK` with `SharedAlbumDto` containing `name = "Vacation 2025"`, `assets.items` with 10 `AssetDto` objects, and `assets.totalItems = 10`

#### Scenario: Valid token with pagination

- **GIVEN** a share token links to an album with 120 assets and `expires_at = NULL`
- **WHEN** an unauthenticated client calls `GET /api/albums/shared/{token}?page=1`
- **THEN** the response is `200 OK` with `assets.items` containing 50 `AssetDto` objects (items 51–100), `assets.pageIndex = 1`, `assets.totalPages = 3`

#### Scenario: Unknown token returns 404

- **GIVEN** no `shared_albums` row exists for token `"00000000-0000-0000-0000-000000000000"`
- **WHEN** a client calls `GET /api/albums/shared/00000000-0000-0000-0000-000000000000`
- **THEN** the response is `404 Not Found`

#### Scenario: Expired token returns 410

- **GIVEN** a `shared_albums` row exists with `expires_at = "2020-01-01T00:00:00Z"` (in the past)
- **WHEN** a client calls `GET /api/albums/shared/{token}`
- **THEN** the response is `410 Gone`

#### Scenario: Token for a deleted album is gone

- **GIVEN** a share token was created for album 5, and album 5 has since been deleted (cascade removed the `shared_albums` row)
- **WHEN** a client calls `GET /api/albums/shared/{token}`
- **THEN** the response is `404 Not Found`

---

### Requirement: The `shared_albums` table stores share tokens with optional expiry

The schema SHALL include a `shared_albums` table with columns `id BIGSERIAL PRIMARY KEY`, `album_id BIGINT NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE`, `token UUID NOT NULL UNIQUE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, and `expires_at TIMESTAMPTZ NULL`. A unique index SHALL exist on `token`. Deleting an album SHALL cascade-delete all associated share rows automatically.

#### Scenario: Album deletion removes share tokens

- **GIVEN** album 7 has two share tokens in `shared_albums`
- **WHEN** `DELETE /api/albums/7` is called by the album owner
- **THEN** the album and both `shared_albums` rows are deleted; calling `GET /api/albums/shared/{token}` for either token returns `404 Not Found`

---

### Requirement: The Angular frontend provides a public `/s/:token` route for viewing a shared album

The Angular router SHALL include a lazy route `{ path: 's/:token', loadComponent: ... }` with no `canActivate` guard. The `SharedAlbumComponent` SHALL call `GET /api/albums/shared/{token}` on initialisation, render the album name and a `ThumbnailComponent` grid of assets, and show a pagination control when `totalPages > 1`. When the response is `410 Gone` the component SHALL display a "This link has expired" message. When the response is `404 Not Found` the component SHALL display a "Album not found" message. The navigation bar SHALL NOT be visible (the component renders outside the authenticated shell).

#### Scenario: Visitor opens a valid shared album link

- **GIVEN** a share token `abc` exists and the visitor is not authenticated
- **WHEN** the visitor navigates to `/s/abc`
- **THEN** `SharedAlbumComponent` loads; the album name is displayed in the page heading; a grid of `ThumbnailComponent` cards appears; no top navigation bar is visible

#### Scenario: Visitor opens an expired share link

- **GIVEN** a share token `exp` exists but has expired (API returns `410 Gone`)
- **WHEN** the visitor navigates to `/s/exp`
- **THEN** `SharedAlbumComponent` loads and displays "This link has expired"; no thumbnail grid is shown

#### Scenario: Visitor opens an unknown share link

- **GIVEN** token `unk` does not exist (API returns `404 Not Found`)
- **WHEN** the visitor navigates to `/s/unk`
- **THEN** `SharedAlbumComponent` displays "Album not found"

#### Scenario: Visitor pages through a large shared album

- **GIVEN** a share token links to an album with 120 assets (`totalPages = 3`)
- **WHEN** the visitor is on page 1 and clicks "Next page"
- **THEN** `SharedAlbumComponent` calls `GET /api/albums/shared/{token}?page=1`; the grid updates to show the next 50 assets

---

### Requirement: Album owners can share an album directly from the album detail page

The `AlbumDetailComponent` toolbar SHALL include a "Share" button. Clicking it SHALL call `POST /api/albums/{id}/share` and, on success, copy the returned `url` to the system clipboard and show a snack bar confirming "Link copied to clipboard". If the browser clipboard API is unavailable the component SHALL display the URL in a dialog so the user can copy it manually.

#### Scenario: Owner shares album and copies the link

- **GIVEN** the owner is viewing the album detail page for album 7
- **WHEN** the owner clicks "Share"
- **THEN** `POST /api/albums/7/share` is called; the returned `url` is written to the clipboard; a snack bar shows "Link copied to clipboard"

#### Scenario: Clipboard API is unavailable

- **GIVEN** `navigator.clipboard` is not available in the browser
- **WHEN** the owner clicks "Share" and the response is received
- **THEN** a `MatDialog` opens displaying the full share URL so the owner can copy it manually
