# Feature 60 — archive-support

**As a** photo library owner, **I want** zip and tar.gz archives browsable as folders, and bulk download in either format, **so that** I can catalog photos still inside archives and choose how to download.

## Acceptance Criteria
- AC1: Given a zip or tar.gz file, when a folder is cataloged, then it appears as an expandable node using the ! path separator and its images are cataloged with thumbnails.
- AC2: Given a virtual folder, when the tree renders, then it shows an archive icon.
- AC3: Given the download endpoint, when format is tar.gz, then a tar.gz archive is streamed; when format is missing, then zip is used.
- AC4: Given the change, when it ships, then no migration is needed.

## Technical notes

Two related capabilities sharing the same archive-reading infrastructure (`org.apache.commons:commons-compress` for tar.gz; `java.util.zip` built-in for zip): (1) **virtual folders** — zip and tar.gz files appear as expandable nodes in the folder navigation tree using a `!` path separator convention (e.g. `/photos/album.zip!/summer/`); the catalog service extracts images to a temp location, generates thumbnails, and stores assets with the virtual path; (2) **download formats** — the bulk-download endpoint (`GET /api/assets/download`) gains a `format` query parameter (`zip` / `tar.gz`) so users can choose the archive type; the existing `ZipOutputStream` path is joined by a `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`; no Flyway migration required for either capability

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
