# raw-exif-jsonb

## Purpose

All raw EXIF fields from the image file are stored as JSONB in `asset_exif.raw_exif` and displayed in a searchable collapsible panel in the EXIF sidebar.

---

## Requirements

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

### Requirement: ExifPanelComponent displays all raw EXIF fields in a collapsible section

When `rawExif` is non-null, the EXIF panel SHALL display a collapsible `MatExpansionPanel` labeled "All EXIF data" below the 13 structured fields.

#### Scenario: Raw EXIF section is visible when data is present

- **GIVEN** an asset with non-null `rawExif`
- **WHEN** the EXIF panel is open
- **THEN** a collapsible "All EXIF data" section is visible below the structured fields

#### Scenario: Raw EXIF section is hidden when rawExif is null

- **GIVEN** an asset with `rawExif = null`
- **WHEN** the EXIF panel is open
- **THEN** no "All EXIF data" section is rendered

### Requirement: Raw EXIF fields can be searched by key name

A search input above the raw EXIF list SHALL filter entries by key name in real time without an additional network request.

#### Scenario: Typing in the search input filters the raw EXIF list

- **GIVEN** the "All EXIF data" panel is expanded and contains 120 entries
- **WHEN** the user types "gps" in the search input
- **THEN** only entries whose key name contains "gps" (case-insensitive) are displayed

#### Scenario: Clearing the search shows all entries

- **GIVEN** the search input contains "gps" and the list is filtered
- **WHEN** the user clears the search input
- **THEN** all 120 entries are shown again
