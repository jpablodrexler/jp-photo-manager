## Why

The EXIF panel currently displays 13 structured fields extracted during cataloging. Modern DSLRs and mirrorless cameras embed 80–300 EXIF fields across multiple IFD directories (IFD0, ExifIFD, GPS IFD, MakerNote). Users who want to see vendor-specific fields (Nikon colour modes, Canon lens correction, Sony face detection) cannot do so. Storing the full raw EXIF as JSONB and displaying it in a searchable collapsible panel exposes all available metadata without requiring new schema columns per field.

## What Changes

- A Flyway migration adds `raw_exif JSONB` to `asset_exif`
- During cataloging, all EXIF fields are collected into a `Map<String, String>` via Apache Commons Imaging and stored as JSONB
- The `ExifPanelComponent` gains a collapsible "All EXIF data" section with a real-time search filter
- Existing assets have `raw_exif = NULL`; re-cataloging any folder populates the column

## Capabilities

### New Capabilities

- `raw-exif-jsonb`: All raw EXIF fields from the image file are stored as JSONB in `asset_exif.raw_exif` and displayed in a searchable collapsible panel in the EXIF sidebar. Re-cataloging a folder populates the column for existing assets.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V26__add_raw_exif_jsonb.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/persistence/entity/AssetExifEntity.java` — add `rawExif Map<String, String>` with `@JdbcTypeCode(SqlTypes.JSON)`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/StorageServiceImpl.java` — collect all EXIF fields during extraction
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/dto/ExifMetadataDto.java` — add `rawExif` field
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/exif-panel/exif-panel.component.ts` — collapsible section with search filter
