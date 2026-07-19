## Why

`mongodb-exif-store` (#72) moved `AssetExif` persistence to MongoDB for two reasons: (1) avoiding JSONB "column sprawl" for the 80-300 raw EXIF fields, and (2) a `2dsphere` geospatial index to support future `$near`/`$geoWithin` "photos near here" queries for the planned `gps-map-view` (#13) and any other future proximity-search feature. `gps-map-view` has just been cancelled and removed from the feature backlog entirely, and no other planned or implemented feature uses geospatial queries — a codebase-wide search confirms `$near`/`$geoWithin`/`geoNear` are never called anywhere; the `2dsphere` index has existed but has never been queried. The remaining justification (a native document model instead of a flat table) was already delivered more cheaply by `raw-exif-jsonb` (#63)'s `raw_exif JSONB` column, which #72 replaced. Keeping `asset_exif` in MongoDB today buys nothing beyond what JSONB already provided, while paying an ongoing cost: no cross-store transactionality between `assets` (PostgreSQL) and `asset_exif` (MongoDB), and an explicit `deleteByAssetId` call standing in for what used to be a simple `ON DELETE CASCADE` foreign key.

This change reverts `asset_exif` persistence back to PostgreSQL with a `raw_exif JSONB` column, restoring the `raw-exif-jsonb` (#63) design. Per explicit instruction, no PostgreSQL-to-MongoDB-to-PostgreSQL data migration is performed — the new table starts empty; existing assets recover their EXIF data the next time their folder is re-cataloged, the same graceful-degradation pattern already used elsewhere in this codebase.

## What Changes

- Recreate the PostgreSQL `asset_exif` table via a single new Flyway migration combining the original `V7__add_asset_exif.sql` schema and `V26__add_raw_exif_jsonb.sql`'s `raw_exif JSONB` column into one `CREATE TABLE` (no dual-write, no verification deploy, no migration runner — this is a fresh, empty table, not a data copy).
- Recreate `AssetExifEntity` (JPA `@Entity`), `JpaAssetExifRepository` (Spring Data JPA), and `AssetExifEntityMapper` (MapStruct), matching the pre-#72 implementation.
- Rewrite `AssetExifRepositoryImpl` to implement the `AssetExifRepository` domain port against JPA instead of MongoDB — the port interface and the `AssetExif` domain model are unchanged.
- **BREAKING**: Delete `AssetExifDocument`, `MongoAssetExifRepository`, and `AssetExifDocumentMapper` — the MongoDB-backed adapter is fully removed. Any MongoDB `asset_exif` data left over from #72 becomes orphaned and unreachable through the application (not migrated back, per explicit instruction).
- Update `MongoIndexInitializer` to remove the `asset_exif`-specific index setup (unique `assetId` index, `2dsphere` `location` index); the `asset_audit_log` index setup for `mongodb-audit-log` (#73) is untouched.
- Remove the explicit `assetExifRepository.deleteByAssetId(assetId)` cascade-delete call from the asset-purge use case — the restored PostgreSQL foreign key's `ON DELETE CASCADE` handles this automatically again.
- Rewrite the `AssetExifRepositoryImpl` unit test against the JPA-backed adapter.
- MongoDB itself, `spring-boot-starter-data-mongodb`, and the `MONGO_URI` configuration are all retained unchanged — MongoDB remains in use for `asset_audit_log` (#73, already implemented) and is not being removed from the stack.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `mongodb-exif-store`: this capability is removed in its entirety — MongoDB-backed `AssetExif` persistence, its indexes, and its one-time PostgreSQL→MongoDB migration runner no longer exist. Expressed as a `REMOVED Requirements` delta.
- `exif-metadata-panel`: persistence wording reverts from "MongoDB `asset_exif` collection" to "PostgreSQL `asset_exif` table"; the "Asset is deleted" scenario reverts from an explicit `deleteByAssetId` MongoDB call to PostgreSQL `ON DELETE CASCADE`; document/upsert language reverts to row/INSERT-UPDATE-via-JPA language.
- `raw-exif-jsonb`: the "All EXIF fields are stored during cataloging" requirement reverts from "MongoDB `asset_exif` document's `rawExif` field... native BSON sub-document" to "PostgreSQL `asset_exif.raw_exif` JSONB column... `Map<String, String>`"; the migration-era null-handling scenario is restated for the fresh empty table instead of the MongoDB migration.

## Impact

- **Backend**: new Flyway migration recreating `asset_exif` in PostgreSQL; new `infrastructure/persistence/entity/AssetExifEntity.java`, `infrastructure/persistence/jpa/JpaAssetExifRepository.java`, `infrastructure/persistence/mapper/AssetExifEntityMapper.java`; rewritten `infrastructure/persistence/adapter/AssetExifRepositoryImpl.java`; removed `infrastructure/persistence/document/AssetExifDocument.java`, `infrastructure/persistence/mongo/MongoAssetExifRepository.java`, `infrastructure/persistence/mapper/AssetExifDocumentMapper.java`; updated `infrastructure/config/MongoIndexInitializer.java`; removed cascade-delete call from the asset-purge use case; rewritten unit test.
- **Domain**: no change — `AssetExif` model and `AssetExifRepository` port are untouched.
- **Frontend**: no change — the EXIF panel and raw-EXIF search UI consume the same REST response shape.
- **Infrastructure**: no change to `docker-compose.yml`/Kubernetes manifests — MongoDB stays deployed for `asset_audit_log`.
- **Documentation**: `openspec/features.md`, `openspec/features-implemented.md`, `JPPhotoManagerWeb/README.md`, and `CLAUDE.md`/`JPPhotoManagerWeb/CLAUDE.md` updated to reflect `asset_exif` living in PostgreSQL again.
- **Data**: existing MongoDB `asset_exif` documents are not migrated and become unreachable; EXIF data for previously-catalogued assets returns as `null` until the asset's folder is re-catalogued.
