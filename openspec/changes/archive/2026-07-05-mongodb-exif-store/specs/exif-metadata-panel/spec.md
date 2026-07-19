## MODIFIED Requirements

### Requirement: EXIF metadata is extracted and persisted during cataloging

The catalog process SHALL extract up to 13 EXIF fields from each image file and persist them in the MongoDB `asset_exif` collection linked to the corresponding `Asset` row via `assetId`. The fields are: `cameraMake`, `cameraModel`, `dateTaken`, `fNumber`, `exposureTime`, `isoSpeed`, `focalLength`, `flash`, `exposureProgram`, `whiteBalance`, `meteringMode`, `gpsLatitude`, `gpsLongitude`. Any field absent from the file's EXIF segment SHALL be stored as `null`.

#### Scenario: JPEG file with full EXIF cataloged for the first time
- **GIVEN** a JPEG image file with a complete EXIF APP1 segment exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a document is upserted into the MongoDB `asset_exif` collection linked to the new `Asset` by `assetId`, with at least `cameraMake`, `cameraModel`, and `dateTaken` populated from the file's EXIF tags

#### Scenario: PNG file with no EXIF cataloged
- **GIVEN** a PNG image file (no EXIF segment) exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a document is upserted into the MongoDB `asset_exif` collection linked to the new `Asset`, with all 13 fields set to `null`

#### Scenario: File is re-cataloged
- **GIVEN** an asset and its `asset_exif` document already exist in MongoDB from a previous catalog run
- **WHEN** the catalog process re-processes the same file
- **THEN** the existing `asset_exif` document is updated in place with freshly extracted values (no duplicate document is created, per the unique index on `assetId`)

#### Scenario: Asset is deleted
- **GIVEN** an `Asset` row and its corresponding MongoDB `asset_exif` document exist
- **WHEN** the `Asset` row is permanently purged
- **THEN** the corresponding `asset_exif` document is also deleted from MongoDB via an explicit `AssetExifRepository.deleteByAssetId(assetId)` call from the purge use case — MongoDB does not cascade this deletion automatically the way the previous PostgreSQL foreign key did

#### Scenario: Asset is soft-deleted (not purged)
- **GIVEN** an `Asset` row and its corresponding MongoDB `asset_exif` document exist
- **WHEN** the `Asset` is soft-deleted (`deleted_at` set, not purged)
- **THEN** the corresponding `asset_exif` document is retained in MongoDB, unaffected by the soft-delete
