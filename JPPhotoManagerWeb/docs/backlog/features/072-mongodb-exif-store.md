# Feature 72 — mongodb-exif-store

**As a** photo library owner, **I want** EXIF metadata kept in a document store (this feature was later reverted), **so that** every EXIF field is stored natively and geospatial queries were possible.

## Acceptance Criteria
- AC1: Given a JPEG with EXIF, when it is cataloged for the first time, then its full metadata is saved in the collection; a PNG with none stores none.
- AC2: Given the file is re-cataloged, when it runs, then the existing document is updated.
- AC3: Given the asset is deleted, when it is purged, then its document is removed; a soft delete leaves it.
- AC4: Given the API, when EXIF is read, then it comes from the collection; with no document, an empty result.
- AC5: Given GPS coordinates, when they are present, then they are stored in a form that supports proximity queries; when absent, none.
- AC6: Given the first startup after deployment, when the migration runs, then existing data is migrated once; a later startup does nothing; an interrupted migration can restart.

## Technical notes

🔁 **Reverted by `revert-exif-postgres-jsonb` (#84, `docs/backlog/features-planned.md`)** — see note below; `asset_exif` is back on PostgreSQL and `raw-exif-jsonb` (#63) is the active implementation again. Replace the `asset_exif` PostgreSQL table (including the JSONB `raw_exif` column planned in #63) with a MongoDB `asset_exif` collection; the native document model stores all 80–300 EXIF fields per image without column sprawl or JSONB workarounds; MongoDB `$near`/`$geoWithin` operators turn the already-stored GPS coordinates into geospatial "photos within radius" queries that are impossible to express efficiently in SQL; only `AssetExifRepositoryImpl` changes — the `AssetExifRepository` port interface and `AssetExif` domain model are untouched; **mutually exclusive with `raw-exif-jsonb` (#63)**: choose this feature if geospatial queries or deep EXIF document nesting are priorities; choose #63 if keeping a single PostgreSQL dependency is preferred

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
