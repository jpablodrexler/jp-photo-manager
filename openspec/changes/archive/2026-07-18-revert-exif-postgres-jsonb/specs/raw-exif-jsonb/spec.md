## MODIFIED Requirements

### Requirement: All EXIF fields are stored during cataloging

The catalog service SHALL collect all EXIF fields from all IFD directories and store them as a `Map<String, String>` in the `raw_exif JSONB` column of the PostgreSQL `asset_exif` table.

#### Scenario: Raw EXIF is populated for a new image
- **GIVEN** a JPEG image with 120 EXIF fields across IFD0, ExifIFD, and GPS IFD
- **WHEN** the image is cataloged
- **THEN** the PostgreSQL `asset_exif.raw_exif` column contains all 120 key-value pairs as a JSONB object

#### Scenario: Raw EXIF is null for assets not yet re-catalogued after the PostgreSQL revert
- **GIVEN** an asset that was catalogued while `asset_exif` was persisted in MongoDB (`mongodb-exif-store`, now reverted) and has not been re-catalogued since
- **WHEN** `GET /api/assets/{id}/exif` is called
- **THEN** `rawExif` is `null` in the response, because the restored PostgreSQL `asset_exif` table starts empty and this asset has no row yet (no error; the panel hides the "All EXIF data" section as it already does for any asset with `rawExif = null`)
