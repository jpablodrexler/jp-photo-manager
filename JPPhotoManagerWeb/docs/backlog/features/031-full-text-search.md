# Feature 31 — full-text-search

**As a** photo library owner, **I want** search that also covers tags, camera model and descriptions, ranked by relevance, **so that** I find a photo by what it contains or how it was taken, not only by its filename.

## Acceptance Criteria
- AC1: Given the migration, when it runs, then a search vector and a GIN index exist for assets.
- AC2: Given a new asset, when it is cataloged, then its search vector is populated automatically.
- AC3: Given a multi-word search, when it runs, then it matches across filename and tags.
- AC4: Given a camera model or a description term, when I search, then the matching assets are returned.
- AC5: Given a very short term, when I search, then a LIKE fallback is used.
- AC6: Given the results, when they are returned, then they are ranked by relevance.

## Technical notes

Extend search beyond filename-only `LIKE` matching to cover tags, EXIF camera model, and the `description` field (#37) using PostgreSQL native `tsvector`/`tsquery` with a `GIN` index; a generated `search_vector` column is maintained automatically by a trigger; the existing `findByFolderWithFilters` JPQL method gains a `search_vector @@ to_tsquery(...)` predicate; ranked results via `ts_rank`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
