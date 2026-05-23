## Context

The `search-and-filter` feature adds a `findByFolderWithFilters` JPQL query that uses `LOWER(a.fileName) LIKE LOWER(CONCAT('%', :search, '%'))` for keyword matching. This is case-insensitive but does not support multi-word queries, stemming, or cross-field search. PostgreSQL full-text search provides `tsvector` (indexed document representation) and `tsquery` (structured search query) with automatic stemming, stop word removal, and ranking via `ts_rank`.

The `asset_exif` table stores `cameraModel` and other EXIF fields. The `asset_tags` junction table stores tags as strings. The `description` column is added by `asset-description` (#37) in V16; full-text search depends on it (V17 must run after V16).

## Goals / Non-Goals

**Goals:**
- Add a `search_vector` generated column covering `file_name`, `description`, EXIF `camera_model`, and tag names
- Maintain `search_vector` via a PostgreSQL trigger (updated on `INSERT`/`UPDATE` to `assets` and tag changes)
- Use `search_vector @@ websearch_to_tsquery('english', :search)` in `findByFolderWithFilters`
- Rank results by `ts_rank` when a search query is present

**Non-Goals:**
- Changing the frontend search input (no UI change needed)
- Supporting multiple languages (English stemming dictionary only)
- Highlighting search terms in results

## Decisions

### 1. Generated `tsvector` column maintained by a trigger

**Decision:** Add a `search_vector TSVECTOR GENERATED ALWAYS AS (...) STORED` column (PostgreSQL 12+). If generated columns cannot reference other tables (they cannot in PostgreSQL), use a trigger instead: after insert/update on `assets`, compute the `tsvector` from `file_name` and `description`; a separate trigger on `asset_tags` updates `search_vector` on tag changes.

**Rationale:** A trigger-maintained column is the standard approach for multi-table `tsvector` that spans related tables (tags, EXIF). A generated column alone cannot reference `asset_exif` or `asset_tags`.

**Alternative considered:** Compute `to_tsvector()` in the JPQL query at search time. Rejected because it cannot use the `GIN` index, making full-table scans on large catalogs too slow.

### 2. `websearch_to_tsquery` for user input

**Decision:** Use `websearch_to_tsquery('english', :search)` instead of `to_tsquery()` to parse user input.

**Rationale:** `websearch_to_tsquery` accepts Google-like syntax (quoted phrases, `-` for exclusion, `OR`) and is forgiving of invalid query syntax — it never throws for ordinary user input, unlike `to_tsquery` which requires strict boolean syntax.

### 3. Fallback to `LIKE` for single-character queries

**Decision:** If the search term is fewer than 3 characters, fall back to the existing `LIKE` behavior. `to_tsquery` requires at least one lexeme; very short terms produce poor results.

**Rationale:** Users often type a single letter while composing a longer search. Falling back prevents empty result sets during typing.

### 4. V17 migration depends on V16

**Decision:** V17 migration adds `search_vector` and assumes the `description` column exists (added in V16 by `asset-description`). The tasks.md documents this dependency explicitly.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Trigger adds overhead to every `INSERT`/`UPDATE` on `assets` | Low | `tsvector` computation is fast; the GIN index update is the main cost, amortized across catalog operations |
| Existing assets have `search_vector = NULL` until re-cataloged | Medium | V17 migration includes a bulk `UPDATE assets SET search_vector = ...` backfill for existing rows |
| `ts_rank` ordering changes the existing sort order | Low | `ts_rank` is applied only when a search term is present; otherwise existing `ORDER BY` is unchanged |
