# Feature 84 — revert-exif-postgres-jsonb

**As a** maintainer of the project, **I want** EXIF persistence moved back from MongoDB to PostgreSQL, **so that** the project keeps a single database dependency now that the geospatial feature was cancelled.

## Acceptance Criteria
- AC1: Given the new migration, when it runs, then an empty asset_exif table with the raw EXIF column exists.
- AC2: Given a JPEG with EXIF or a PNG with none, when it is cataloged, then metadata is stored or not as before; a re-cataloged file refreshes it.
- AC3: Given an asset is deleted, when the deletion applies, then its EXIF rows are removed by the cascade foreign key; a soft delete keeps them.
- AC4: Given an asset cataloged before the revert, when it is read, then raw EXIF is null until its folder is re-cataloged.
- AC5: Given the audit log, when the revert ships, then MongoDB stays in use for it.

## Technical notes

Revert `mongodb-exif-store` (#72): move `AssetExif` persistence back from MongoDB to PostgreSQL, restoring the `raw-exif-jsonb` (#63) `raw_exif JSONB` column design that #72 had superseded. Triggered by the cancellation of `gps-map-view` (#13) — #72's `2dsphere` geospatial index existed solely to support #13's proximity queries, and no other feature used it. A new Flyway migration (`V33__recreate_asset_exif.sql`) recreates an empty `asset_exif` PostgreSQL table (original `V7` schema + `V26`'s `raw_exif` column combined); `AssetExifRepositoryImpl` moved back to JPA (`AssetExifEntity`, `JpaAssetExifRepository`, `AssetExifEntityMapper`); the explicit `deleteByAssetId` cascade-delete call was removed from the purge/delete use cases in favor of the restored `ON DELETE CASCADE` foreign key. Existing MongoDB `asset_exif` data was **not** migrated back (explicit instruction) — EXIF data for assets catalogued while #72 was in effect returns once the asset's folder is re-catalogued. MongoDB itself is untouched and remains in use for `mongodb-audit-log` (#73)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
