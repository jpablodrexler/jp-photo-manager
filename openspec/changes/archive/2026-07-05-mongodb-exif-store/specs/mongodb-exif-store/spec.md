## ADDED Requirements

### Requirement: AssetExif is persisted in a MongoDB collection

The system SHALL persist `AssetExif` documents in a MongoDB collection named `asset_exif`, replacing the previous PostgreSQL table of the same name. The `AssetExifRepository` domain port (`findByAssetId`, `save`, `deleteByAssetId`) SHALL be implemented by a MongoDB-backed adapter with no change to the port interface or the `AssetExif` domain model.

#### Scenario: Saving EXIF data during cataloging
- **GIVEN** an image file is being cataloged
- **WHEN** the catalog process calls `AssetExifRepository.save(assetExif)`
- **THEN** a document is upserted into the MongoDB `asset_exif` collection keyed by `assetId`, containing all extracted fields

#### Scenario: Reading EXIF data for the API
- **GIVEN** a catalogued asset with a populated MongoDB `asset_exif` document
- **WHEN** `AssetExifRepository.findByAssetId(assetId)` is called
- **THEN** the returned `AssetExif` domain object contains the same field values as previously returned by the PostgreSQL-backed adapter

#### Scenario: No EXIF document exists for an asset
- **GIVEN** an asset with no corresponding MongoDB `asset_exif` document
- **WHEN** `AssetExifRepository.findByAssetId(assetId)` is called
- **THEN** an empty `Optional` is returned, matching the previous PostgreSQL-backed behavior

### Requirement: A unique index on assetId enforces one EXIF document per asset

The MongoDB `asset_exif` collection SHALL have a unique index on the `assetId` field, created automatically at application startup if it does not already exist.

#### Scenario: Re-saving EXIF for the same asset updates the existing document
- **GIVEN** a MongoDB `asset_exif` document already exists for `assetId = 42`
- **WHEN** the catalog process re-processes the same file and calls `save()` again
- **THEN** the existing document for `assetId = 42` is updated in place, and no duplicate document is created

### Requirement: GPS coordinates are indexed for geospatial queries

When both `gpsLatitude` and `gpsLongitude` are non-null, the MongoDB document SHALL include a GeoJSON `Point` field named `location` in the form `{ type: "Point", coordinates: [longitude, latitude] }`. The `asset_exif` collection SHALL have a `2dsphere` index on `location`, created automatically at application startup if it does not already exist.

#### Scenario: GPS coordinates present
- **GIVEN** an asset with `gpsLatitude = 37.77` and `gpsLongitude = -122.42`
- **WHEN** the EXIF document is saved
- **THEN** the document's `location` field is `{ type: "Point", coordinates: [-122.42, 37.77] }`

#### Scenario: GPS coordinates absent
- **GIVEN** an asset with no GPS EXIF tags
- **WHEN** the EXIF document is saved
- **THEN** the document's `location` field is absent or `null`, and the document is excluded from `2dsphere` index queries

### Requirement: Asset deletion removes the corresponding EXIF document

Because MongoDB has no foreign-key cascade, the system SHALL explicitly call `AssetExifRepository.deleteByAssetId(assetId)` from the asset-deletion and asset-purge use cases whenever an `Asset` is permanently removed.

#### Scenario: Purging a soft-deleted asset removes its EXIF document
- **GIVEN** an `Asset` with a corresponding MongoDB `asset_exif` document
- **WHEN** the purge use case permanently deletes the `Asset`
- **THEN** the corresponding MongoDB `asset_exif` document is also deleted

#### Scenario: Soft-deleting an asset does not remove its EXIF document
- **GIVEN** an `Asset` with a corresponding MongoDB `asset_exif` document
- **WHEN** the asset is soft-deleted (marked `deleted_at`, not purged)
- **THEN** the corresponding MongoDB `asset_exif` document is retained, so the EXIF data is available if the asset is restored

### Requirement: Existing PostgreSQL EXIF data is migrated to MongoDB exactly once

On application startup, the system SHALL check for a completion marker document in a `migration_status` MongoDB collection; if absent, it SHALL copy all rows from the PostgreSQL `asset_exif` table into MongoDB in batches, keyed by `assetId` (an upsert, so re-copying a row already present is a harmless no-op), and then write the completion marker. If the marker is present, no data is copied.

#### Scenario: First startup after deploying MongoDB support
- **GIVEN** no completion marker exists in `migration_status` and the PostgreSQL `asset_exif` table contains 10,000 rows
- **WHEN** the application starts
- **THEN** the migration runner copies all 10,000 rows into MongoDB as documents keyed by `assetId`, then writes the completion marker

#### Scenario: Subsequent startup after migration has already run
- **GIVEN** the completion marker exists in `migration_status`
- **WHEN** the application restarts
- **THEN** the migration runner performs no data copy and the collection's document count is unchanged

#### Scenario: Migration is interrupted mid-run and restarted
- **GIVEN** the migration runner previously copied half of the PostgreSQL rows before the process was terminated, and no completion marker was written
- **WHEN** the application restarts and the runner executes again
- **THEN** the runner re-processes all batches, upserting rows already copied as harmless no-ops and copying the remaining rows, then writes the completion marker
