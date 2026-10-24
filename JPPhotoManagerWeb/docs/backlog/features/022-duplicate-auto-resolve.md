# Feature 22 — duplicate-auto-resolve

**As a** photo library owner, **I want** to clean up duplicates automatically with a chosen policy, **so that** I free space without resolving each pair by hand.

## Acceptance Criteria
- AC1: Given the duplicates page, when I open "Clean up automatically", then I can choose keep oldest, keep newest, keep highest resolution or keep preferred folder.
- AC2: Given a policy, when I run a dry run, then the count is returned without deleting; I see the preview count before confirming.
- AC3: Given keep oldest, keep newest or keep highest resolution, when applied, then the matching asset is kept in each group and the others soft-deleted.
- AC4: Given keep preferred folder with no match in a group, when applied, then it falls back to keep oldest.
- AC5: Given I cancel, when the dialog closes, then no change is made.

## Technical notes

"Clean up automatically" dialog on the duplicates page with policies (keep oldest, keep newest, keep highest resolution, keep preferred folder); delegates to existing soft-delete path

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
