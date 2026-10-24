# Feature 6 — bulk-download-zip

**As a** photo library owner, **I want** to download a selection of photos as one ZIP archive, **so that** I can copy or share many photos at once.

## Acceptance Criteria
- AC1: Given a valid selection, when I download it, then a ZIP containing the files is streamed; a single asset also works.
- AC2: Given duplicate filenames, when the ZIP is built, then they are disambiguated.
- AC3: Given one unreadable file, when the ZIP is built, then it is skipped; given all files are unreadable, then an empty ZIP is returned.
- AC4: Given a request over the configured asset limit, or an empty list, when I download, then it is rejected.
- AC5: Given the gallery, when I download the selected assets, then the download starts; on failure an error message is shown.
- AC6: Given an unauthenticated request, when it downloads, then it is rejected.

## Technical notes

Download a selection of assets as a ZIP archive via a streaming `ZipOutputStream`; configurable per-request size cap

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
