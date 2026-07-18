## MODIFIED Requirements

### Requirement: EXIF metadata is extracted and persisted during cataloging

The catalog process SHALL extract up to 13 EXIF fields from each image file and persist them in the PostgreSQL `asset_exif` table linked to the corresponding `Asset` row via `asset_id`. The fields are: `cameraMake`, `cameraModel`, `dateTaken`, `fNumber`, `exposureTime`, `isoSpeed`, `focalLength`, `flash`, `exposureProgram`, `whiteBalance`, `meteringMode`, `gpsLatitude`, `gpsLongitude`. Any field absent from the file's EXIF segment SHALL be stored as `null`.

#### Scenario: JPEG file with full EXIF cataloged for the first time
- **GIVEN** a JPEG image file with a complete EXIF APP1 segment exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a row is inserted into the PostgreSQL `asset_exif` table linked to the new `Asset` by `asset_id`, with at least `camera_make`, `camera_model`, and `date_taken` populated from the file's EXIF tags

#### Scenario: PNG file with no EXIF cataloged
- **GIVEN** a PNG image file (no EXIF segment) exists in a catalogued folder
- **WHEN** the catalog process runs and indexes the file
- **THEN** a row is inserted into the PostgreSQL `asset_exif` table linked to the new `Asset`, with all 13 fields set to `NULL`

#### Scenario: File is re-cataloged
- **GIVEN** an asset and its `asset_exif` row already exist in PostgreSQL from a previous catalog run
- **WHEN** the catalog process re-processes the same file
- **THEN** the existing `asset_exif` row is updated in place with freshly extracted values (no duplicate row is created, per the primary key on `asset_id`)

#### Scenario: Asset is deleted
- **GIVEN** an `Asset` row and its corresponding PostgreSQL `asset_exif` row exist
- **WHEN** the `Asset` row is permanently purged
- **THEN** the corresponding `asset_exif` row is also deleted automatically via the `ON DELETE CASCADE` foreign key — no explicit application-level delete call is required

#### Scenario: Asset is soft-deleted (not purged)
- **GIVEN** an `Asset` row and its corresponding PostgreSQL `asset_exif` row exist
- **WHEN** the `Asset` is soft-deleted (`deleted_at` set, not purged)
- **THEN** the corresponding `asset_exif` row is retained, unaffected by the soft-delete (the `Asset` row itself is not deleted, so `ON DELETE CASCADE` does not trigger)
