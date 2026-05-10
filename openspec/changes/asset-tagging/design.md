## Context

Assets currently carry three forms of user-assigned metadata: folder path (implicit from disk location), album membership (many-to-many via `album_assets`), and star rating (integer 0–5 on the `Asset` entity). Tags are the missing lightweight labeling primitive — free-form keywords that can span folders and compose with the existing search and filter system.

The existing filter pipeline (`GET /api/assets` → Criteria API in `AssetRepository`) already composes multiple optional predicates. Tags integrate as one more optional predicate, minimizing changes to the query layer.

## Goals / Non-Goals

**Goals:**
- A `Tag` entity normalized to lowercase, and an `AssetTag` join table.
- CRUD tag endpoints: assign, remove, list-all (for autocomplete).
- Tags as an additional optional filter on `GET /api/assets`.
- Tag chip editor in the gallery EXIF panel and info overlay.
- Tag filter in the gallery toolbar (multi-select).
- Bulk tagging for selected assets in the gallery.

**Non-Goals:**
- Tag hierarchies or parent/child relationships.
- Tag color coding or icons.
- Tag-based search presets (search presets already save filter state; they will automatically persist selected tags once the filter param exists).
- Tag usage counts or sorting tags by frequency (v1 keeps it simple).

## Decisions

### 1. Data model: normalized `tags` table + `asset_tags` join table

**Decision:**
```
tags           asset_tags
──────────     ────────────────
tag_id  PK     asset_id  FK → assets
name    UNIQUE tag_id    FK → tags
               PK (asset_id, tag_id)
```
Tags are stored once in the `tags` table and referenced by many assets via `asset_tags`. Names are lowercased at the application layer before persistence.

**Rationale:** Normalizing tag names avoids duplication and makes rename-a-tag feasible in the future. The join table PK prevents duplicate assignments without an extra unique index.

**Alternatives considered:**
- *Denormalized array on `Asset`*: Simpler schema, but PostgreSQL array columns complicate the Criteria API filter and make cross-asset tag queries (e.g., "list all tags") expensive.
- *JSON column on `Asset`*: Same drawbacks as the array approach; also bypasses JPA type safety.

### 2. Tag normalization: lowercase on write, not stored case-insensitively

**Decision:** The service layer calls `tag.toLowerCase(Locale.ROOT)` before any persistence or lookup. The database column uses a standard `VARCHAR`; no `CITEXT` or expression index is added.

**Rationale:** Keeps the schema portable and migration simple. Tag names like "Vacation" and "vacation" are treated as the same tag. Displaying stored tags to the user always shows lowercase, which is the convention.

### 3. Filter semantics: AND across selected tags

**Decision:** `GET /api/assets?tags=vacation,family` returns assets that have **all** listed tags (intersection, not union).

**Rationale:** AND semantics produce smaller, more targeted result sets — the typical intent when combining tags. OR semantics would require a different query structure (subquery or `HAVING COUNT(DISTINCT tag) >= 1`). AND is implemented cleanly with a `GROUP BY asset_id HAVING COUNT(DISTINCT tag_id) = ?tagCount` subquery joining the Criteria query.

**Alternatives considered:** *OR semantics* — easier SQL (`IN` clause), but less useful: selecting "vacation" OR "family" returns everything tagged with either, which is rarely the user's intent.

### 4. API surface: three endpoints, no dedicated tag controller

**Decision:** Tag management is handled via three endpoints added to `AssetController`:
- `POST /api/assets/{id}/tags` — body `{ "name": "vacation" }`; creates tag if new, assigns to asset.
- `DELETE /api/assets/{id}/tags/{tagName}` — removes assignment; deletes orphan tag.
- `GET /api/tags?q=vac` — returns all tags whose name contains the query string (for autocomplete); no auth restriction beyond the standard `authGuard`.

**Rationale:** Tags are asset metadata, so keeping them under `/api/assets` matches the existing structure (e.g., `/api/assets/{id}/rating`). A dedicated `TagController` would be premature given the limited surface area.

### 5. Orphan tag cleanup: delete on last removal

**Decision:** When the last assignment to a tag is removed, the tag row is deleted from `tags`. No background cleanup job.

**Rationale:** Orphan tags pollute autocomplete with stale labels. Eager deletion keeps autocomplete clean. The implementation is a single `DELETE FROM tags WHERE tag_id = ? AND NOT EXISTS (SELECT 1 FROM asset_tags WHERE tag_id = ?)` after removing the join row.

### 6. Frontend: MatChipsInput for tag editing

**Decision:** Use Angular Material's `MatChipsModule` with `MatChipInput` for the tag editor in the EXIF panel and bulk-tag dialog. The chips input provides keyboard-driven add (Enter / comma), backspace-to-remove, and integrates with `MatAutocomplete` for suggestions.

**Rationale:** Consistent with the Material design system already used throughout the app. Autocomplete wires naturally to `GET /api/tags?q=...` with a debounce.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| AND filter with many tags returns an empty result silently | The filter count badge in the toolbar makes active tags visible; users can remove one at a time |
| Large number of tags slows autocomplete `GET /api/tags?q=` | Index on `tags.name`; query is bounded by the `q` prefix filter and a `LIMIT 20` |
| Orphan-delete races under concurrent removes | Wrap the assignment removal + orphan check in a single `@Transactional` method; PostgreSQL row locking makes this safe |
| Bulk tag dialog applying to 200+ selected assets | Batch insert via `saveAll()` on the join table; single transaction, fast enough for gallery-scale libraries |
| Tag names with special characters (slashes, spaces) in URL path segment for DELETE | Encode tag name in the DELETE path; alternatively, accept tag name as a query param to avoid encoding issues |

## Migration Plan

1. Add Flyway migration `V<next>__create_tags_tables.sql` creating `tags` and `asset_tags`.
2. No data migration needed — no existing tag data exists.
3. New endpoints are additive; existing clients are unaffected.
4. Rollback: drop `asset_tags`, drop `tags`, remove Flyway migration entry (dev only).

## Open Questions

- Should the `DELETE /api/assets/{id}/tags/{tagName}` path encode tag names, or accept them as query params? Decision: use a query param `?name=vacation` to avoid percent-encoding edge cases in path segments.
- Should tag autocomplete be accessible without authentication (public)? No — tags reveal user-assigned labels; keep behind `authGuard`.
