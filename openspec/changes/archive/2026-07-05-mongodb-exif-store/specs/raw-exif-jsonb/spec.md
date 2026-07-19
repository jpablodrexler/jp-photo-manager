## MODIFIED Requirements

### Requirement: All EXIF fields are stored during cataloging

The catalog service SHALL collect all EXIF fields from all IFD directories and store them as a `Map<String, String>` in the `rawExif` field of the MongoDB `asset_exif` document (previously the `asset_exif.raw_exif` PostgreSQL `JSONB` column).

#### Scenario: Raw EXIF is populated for a new image
- **GIVEN** a JPEG image with 120 EXIF fields across IFD0, ExifIFD, and GPS IFD
- **WHEN** the image is cataloged
- **THEN** the MongoDB `asset_exif` document's `rawExif` field contains all 120 key-value pairs as a native BSON sub-document

#### Scenario: Raw EXIF is null for images cataloged before the migration
- **GIVEN** an image cataloged before the MongoDB migration was applied, whose `asset_exif` document was copied over from the previous PostgreSQL row
- **WHEN** `GET /api/assets/{id}/exif` is called
- **THEN** `rawExif` is `null` in the response if it was `null` in the original PostgreSQL row (no error, panel is simply hidden)
