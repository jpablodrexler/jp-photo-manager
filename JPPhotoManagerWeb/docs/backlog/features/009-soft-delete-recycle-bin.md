# Feature 9 — soft-delete-recycle-bin

**As a** photo library owner, **I want** removed photos to go to a recycle bin before being deleted for good, **so that** a mistaken removal can be undone.

## Acceptance Criteria
- AC1: Given "Remove from catalog", when I remove one or several assets, then they are marked deleted, not erased, and can be restored.
- AC2: Given a hard delete, when it applies, then the asset does not appear in the recycle bin.
- AC3: Given the recycle bin, when I open it, then it lists soft-deleted assets or an empty state.
- AC4: Given an asset in the bin, when I restore it, then it returns to the gallery.
- AC5: Given assets in the bin, when I purge a selection or empty the bin, then they are permanently deleted, including when the file is already missing from disk.
- AC6: Given soft-deleted assets, when duplicates are computed, then they are excluded.
- AC7: Given the retention period, when the scheduled job runs, then expired items are purged and non-expired items are kept.
- AC8: Given an unauthenticated request, when it opens the bin, then it is rejected.

## Technical notes

"Remove from catalog" sets `deleted_at` instead of deleting; recycle-bin page lists, restores, and purges soft-deleted assets; scheduled auto-purge after configurable retention period

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
