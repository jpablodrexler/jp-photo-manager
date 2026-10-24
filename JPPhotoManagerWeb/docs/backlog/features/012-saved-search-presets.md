# Feature 12 — saved-search-presets

**As a** photo library owner, **I want** to save the current filters as a named preset and restore it with one click, **so that** I do not rebuild the same search every time.

## Acceptance Criteria
- AC1: Given active filters (search, date range, minimum rating), when I save a preset with a name, then it is stored for me; a preset may hold only some fields; an empty name is rejected.
- AC2: Given the presets list, when I open it, then I see only my own presets, or an empty list.
- AC3: Given a preset, when I select it, then all its filter fields, including the date range, are restored.
- AC4: Given the save dialog, when I cancel, then presets are unchanged.
- AC5: Given a preset, when I delete it from the dropdown, then it is removed; deleting another user's or a non-existent preset returns 404.
- AC6: Given an unauthenticated request, when it accesses presets, then it is rejected.

## Technical notes

Save the current filter state (search, date range, minimum rating) as a named preset scoped to the authenticated user; restore with one click from a dropdown in the filter toolbar

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
