# Feature 1 — exif-metadata-panel

**As a** photo library owner, **I want** to see the EXIF metadata of a photo in a collapsible panel in the viewer, **so that** I know the camera, exposure, location and date a photo was taken with.

## Acceptance Criteria
- AC1: Given a file with EXIF data, when it is cataloged for the first time, then its full metadata is stored; a file with no EXIF is cataloged without a metadata row.
- AC2: Given a file that is cataloged again, when it is re-cataloged, then its metadata is refreshed; given the asset is deleted, then its metadata is removed.
- AC3: Given a malformed tag, when a file is cataloged, then the other tags are still stored.
- AC4: Given an asset with EXIF data, when the metadata API is called, then it returns the camera, exposure, GPS and date-taken fields; GPS is returned when present and absent otherwise.
- AC5: Given an asset with no metadata row (cataloged before this feature) or an unknown asset id, when the API is called, then it answers with no data or not found respectively; an unauthenticated request is rejected.
- AC6: Given the viewer, when I open the EXIF panel for an asset with data, then the fields are shown; for an asset without data, an empty state; closing it hides it, and leaving the viewer hides it.
- AC7: Given I toggle the panel twice for the same asset, when it reopens, then it shows the same data.

## Technical notes

Store full EXIF metadata during cataloging; display a collapsible panel in the viewer showing camera, exposure, GPS, and date-taken fields

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
