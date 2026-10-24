# Feature 21 — video-file-support

**As a** photo library owner, **I want** video files supported in the catalog and the viewer, **so that** my clips live next to my photos.

## Acceptance Criteria
- AC1: Given .mp4, .mov and .mkv files (extension match is case-insensitive), when a folder is cataloged, then they are indexed; unsupported extensions are not.
- AC2: Given a video, when it is cataloged, then a thumbnail is generated with FFmpeg; if FFmpeg fails for a file or is not installed, then the catalog continues without a thumbnail.
- AC3: Given a video asset, when lookups run, then image rotation and EXIF lookups behave safely and the list response carries an isVideo flag.
- AC4: Given the gallery, when a video asset renders, then it shows a play-overlay icon; an image shows none.
- AC5: Given the viewer, when I open a video, then it plays in a video element; navigating between images and videos works.
- AC6: Given the container image, when it runs, then FFmpeg is executable inside it.

## Technical notes

Thumbnail generation via FFmpeg (`ProcessBuilder`) for `.mp4`/`.mov`/`.mkv`; catalog service accepts video MIME types; frontend shows play-overlay icon and `<video>` tag in the viewer

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
