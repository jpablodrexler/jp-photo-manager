# Feature 3 — drag-and-drop-upload

**As a** photo library owner, **I want** to upload photos by dropping them on the gallery or using an Upload button, **so that** new photos are saved to the folder and indexed immediately.

## Acceptance Criteria
- AC1: Given a cataloged folder, when I drop or select valid image files, then they are saved to the folder and indexed; several files upload sequentially.
- AC2: Given a non-image file or a file with a spoofed content type, when I upload it, then it is rejected.
- AC3: Given an uncataloged folder, when I try to upload, then it is refused.
- AC4: Given an upload in progress, when it runs, then a progress bar advances, and a success or error indicator is shown at the end.
- AC5: Given I drag files over the gallery, when I enter or leave it, then a drop target is shown or hidden.
- AC6: Given a batch finishes, when the last file completes, then the grid refreshes.
- AC7: Given an unauthenticated request, when it tries to upload, then it is rejected.

## Technical notes

Upload image files directly from the browser by dropping them onto the gallery grid or clicking an Upload button; files are saved to the selected folder and indexed immediately

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
