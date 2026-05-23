## Context

The `FolderNavComponent` renders a flat CDK tree of `FlatNode` objects. The pinned section is a separate list rendered above the tree using the same `FlatNode` model but sourced from the bookmarks API. A `MatDivider` visually separates the two sections. Each tree node gets a pin `MatIconButton` that calls the bookmark add/remove endpoints.

## Goals / Non-Goals

**Goals:**
- Flyway V20: `CREATE TABLE folder_bookmarks (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), folder_path VARCHAR(1024) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE (user_id, folder_path))`
- `GET /api/folders/bookmarks` returns `[ { id, folderPath, createdAt } ]` for the authenticated user
- `POST /api/folders/bookmarks` body `{ "folderPath": "..." }` — idempotent (unique constraint)
- `DELETE /api/folders/bookmarks/{id}` — returns 204; only the bookmark owner may delete
- `FolderNavComponent`: on init, load bookmarks; render as a pinned list above the main tree with a `MatDivider`; pin icon per node toggles between filled (`bookmark`) and outlined (`bookmark_border`) icons
- Bookmarks section is limited to 10 items (configurable) to avoid cluttering the sidebar

**Non-Goals:**
- Reordering bookmarks (always sorted by `createdAt` ascending)
- Sharing bookmarks between users
- Bookmarking arbitrary paths (only paths that exist in the folder tree)

## Decisions

### 1. UNIQUE constraint on `(user_id, folder_path)`

**Decision:** Enforce uniqueness in the DB. `POST /api/folders/bookmarks` returns `200` if the bookmark already exists (idempotent from the client's perspective — returns the existing row).

**Rationale:** Prevents duplicate bookmarks without requiring the client to check first.

### 2. Pinned section rendered as a simple `@for` list, not a CDK tree

**Decision:** The bookmarked folders are rendered as a flat clickable list above the tree, not as a sub-tree hierarchy.

**Rationale:** Bookmarks are individual folder references, not hierarchical. A flat list is simpler to implement and easier to scan.

### 3. Limit of 10 bookmarks

**Decision:** `POST /api/folders/bookmarks` returns `400 Bad Request` if the user already has 10 bookmarks.

**Rationale:** Prevents the pinned section from growing too tall. The limit is configurable via `photomanager.bookmarks.max-per-user`.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Bookmarked folder path renamed or deleted | Low | Show a broken bookmark with a warning icon; user can remove it manually |
| Pinned section grows tall on small screens | Low | Max-10 limit; section is scrollable |
