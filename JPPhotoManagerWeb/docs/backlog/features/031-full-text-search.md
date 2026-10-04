# Feature 31 — full-text-search

Extend search beyond filename-only `LIKE` matching to cover tags, EXIF camera model, and the `description` field (#37) using PostgreSQL native `tsvector`/`tsquery` with a `GIN` index; a generated `search_vector` column is maintained automatically by a trigger; the existing `findByFolderWithFilters` JPQL method gains a `search_vector @@ to_tsquery(...)` predicate; ranked results via `ts_rank`
