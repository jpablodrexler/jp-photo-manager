# Feature 14 — smart-albums

**As a** photo library owner, **I want** smart albums whose content is filled by a saved filter, **so that** an album stays up to date as my catalog changes.

## Acceptance Criteria
- AC1: Given an existing database, when the migration runs, then it succeeds and existing albums remain static.
- AC2: Given the create form, when I turn on "Make smart" and give filter criteria (for example a minimum rating or several criteria), then a smart album is created; an empty filter is rejected; without the toggle a static album is created.
- AC3: Given an album, when I convert it to smart, revert it to static, or edit its criteria, then the mode and criteria change.
- AC4: Given a smart album, when I browse it, then it returns the assets matching its filter across all folders with correct pagination, excluding soft-deleted assets.
- AC5: Given the album list, when it renders, then smart albums expose their filter and a badge, and their count is dynamic and updates as the catalog changes.
- AC6: Given a smart album, when I try to add or remove assets, then it is rejected; it appears disabled in the "Add to album" dialog.
- AC7: Given a static album, when I open it, then it behaves as before with a remove button.
- AC8: Given an unauthenticated request, when it creates a smart album, then it is rejected.

## Technical notes

Extend albums with an optional `filter_json` column (same shape as saved search presets) so albums can be dynamically populated at query time; UI toggle between static and dynamic mode

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
