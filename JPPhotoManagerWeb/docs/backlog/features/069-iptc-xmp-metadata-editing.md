# Feature 69 — iptc-xmp-metadata-editing

**As a** photo library owner, **I want** to read and edit IPTC caption, copyright, creator, keywords, city and country in the viewer, **so that** the metadata travels with the file and I can search by it.

## Acceptance Criteria
- AC1: Given a JPEG, when I open the viewer panel, then caption, copyright, creator, keywords, city and country are shown below the EXIF panel.
- AC2: Given I edit caption or copyright, when I leave the field, then the change is saved automatically to the file and the asset row.
- AC3: Given keywords, when I save them, then they map to the asset's tags and are available in tag filters.
- AC4: Given the IPTC endpoint, when it is called, then it writes the segment back to the file on disk.

## Technical notes

Read and write IPTC/XMP fields (caption, copyright notice, creator, keywords, city, country) directly in the viewer panel alongside the existing EXIF display; Apache Commons Imaging (already a dependency) supports IPTC segment read and write in JPEG files via `JpegIptcRewriter`; a new `PATCH /api/assets/{id}/iptc` endpoint writes the updated IPTC segment back to the file on disk and refreshes the `assets` row; caption and copyright are stored in two new VARCHAR columns on `assets` (Flyway migration) so they are queryable without re-reading the file; keywords are mapped to the existing `asset_tags` table, making them immediately available in tag-based filters; the viewer panel shows caption and copyright as editable `MatFormField` inputs below the EXIF panel with auto-save on blur

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
