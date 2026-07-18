## REMOVED Requirements

### Requirement: AssetExif is persisted in a MongoDB collection
**Reason**: `mongodb-exif-store` (#72) is being reverted — `gps-map-view` (#13), the feature that motivated this capability's `2dsphere` geospatial index, has been cancelled, and no other feature uses geospatial queries. `AssetExif` persistence moves back to PostgreSQL, restoring `raw-exif-jsonb` (#63).
**Migration**: `AssetExifRepository` is now implemented by a PostgreSQL/JPA adapter (see the `exif-metadata-panel` and `raw-exif-jsonb` delta specs in this change). Existing MongoDB `asset_exif` documents are not migrated; EXIF data for previously-catalogued assets is recovered by re-cataloging the asset's folder.

### Requirement: A unique index on assetId enforces one EXIF document per asset
**Reason**: The MongoDB `asset_exif` collection no longer exists; uniqueness is enforced by the restored PostgreSQL `asset_exif.asset_id` primary key instead.
**Migration**: No action required — a PostgreSQL primary key provides the same one-row-per-asset guarantee natively.

### Requirement: GPS coordinates are indexed for geospatial queries
**Reason**: This index was created solely to support `$near`/`$geoWithin` proximity queries for `gps-map-view` (#13), which has been cancelled. No feature has ever queried this index.
**Migration**: None. `gpsLatitude`/`gpsLongitude` remain available as plain columns on the restored PostgreSQL `asset_exif` table for any future feature that needs them; a geospatial index can be reintroduced independently if a future feature is proposed that actually requires proximity queries.

### Requirement: Asset deletion removes the corresponding EXIF document
**Reason**: This requirement existed only because MongoDB has no foreign-key cascade. The restored PostgreSQL `asset_exif` table uses `asset_id BIGINT PRIMARY KEY REFERENCES assets(asset_id) ON DELETE CASCADE`, so cleanup is automatic again.
**Migration**: The explicit `assetExifRepository.deleteByAssetId(assetId)` call added to the asset-purge use case for this requirement is removed; see the `exif-metadata-panel` delta spec in this change for the restored `ON DELETE CASCADE` behavior.

### Requirement: Existing PostgreSQL EXIF data is migrated to MongoDB exactly once
**Reason**: This requirement described the one-time PostgreSQL→MongoDB migration runner that shipped with #72. That runner has no purpose once `asset_exif` moves back to PostgreSQL, and this revert explicitly does not migrate data in either direction.
**Migration**: No replacement. The restored PostgreSQL `asset_exif` table starts empty; the migration runner component is deleted from the codebase.
