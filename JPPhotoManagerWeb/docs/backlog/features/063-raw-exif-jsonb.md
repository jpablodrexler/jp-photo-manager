# Feature 63 — raw-exif-jsonb

**As a** photo library owner, **I want** every EXIF field of a photo available in the viewer, searchable by name, **so that** I can find manufacturer-specific details that the main panel does not show.

## Acceptance Criteria
- AC1: Given a new image, when it is cataloged, then all its EXIF fields are stored as raw key-value data.
- AC2: Given an image cataloged before the migration, when I open its panel, then the raw data is null and the "All EXIF data" section is hidden.
- AC3: Given raw data, when I open the EXIF panel, then an "All EXIF data" section lists every key-value pair.
- AC4: Given the section, when I type in its search input, then the list filters by key name in real time and clearing it shows all entries.
- AC5: Given a folder is re-cataloged, when it finishes, then raw data is populated for its assets.

## Technical notes

Add a `raw_exif JSONB` column to the existing `asset_exif` table (new Flyway migration); during cataloging, after extracting the 13 known fields, iterate all EXIF directories returned by Apache Commons Imaging (`JpegImageMetadata.getExif().getDirectories()` → `dir.getAllFields()`) and collect every `TiffField` into a `Map<String, String>` keyed by `field.getTagInfo().name` with value `field.getValueDescription()`; the map is serialized to JSONB using Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, String> rawExif` field in `AssetExifEntity` (no new Maven dependency — Hibernate 6 handles JSON natively via Jackson, already present); `AssetExif` domain model and `AssetExifEntityMapper` (MapStruct) gain the `rawExif` field; `ExifMetadataDto` exposes it as `Map<String, String> rawExif`; `ExifMetadata` TypeScript interface adds `rawExif: Record<string, string> | null`; in `ExifPanelComponent`, below the existing 13 structured fields, a collapsible `MatExpansionPanel` labeled "All EXIF data" renders every key-value pair from `rawExif` as a compact two-column list; a `MatFormField` search input above the list filters entries by key name in real time using a component-level computed signal so the full raw map is never re-fetched; a single image from a modern DSLR or mirrorless camera typically carries 80–300 EXIF fields across the IFD0, ExifIFD, GPS IFD, and MakerNote directories; the search filter is essential because MakerNote alone can add 100+ manufacturer-specific fields (Nikon colour modes, Canon lens correction data, Sony face detection coordinates, etc.); existing assets cataloged before this migration have `raw_exif = NULL` and the panel section is hidden when `rawExif` is null; re-cataloging any folder populates the column for all assets in that folder; new Flyway migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
