# Feature 7 — search-and-filter

**As a** photo library owner, **I want** to filter the gallery by filename and creation date, **so that** I find a photo without scrolling through the folder.

## Acceptance Criteria
- AC1: Given a keyword, when I search, then assets whose filename contains it are returned, case-insensitively.
- AC2: Given a date-from and/or date-to, when I filter, then only assets in that range are returned; assets with no creation date are excluded.
- AC3: Given search and dates together, when I filter, then they combine; with no filters, all assets are returned as before.
- AC4: Given filtered results, when I look at the total, then it reflects the filtered count across pages.
- AC5: Given the search box, when I type, then the request is debounced by 400 ms; a date change filters immediately.
- AC6: Given any filter change, when it applies, then the list returns to page 0; changing the folder clears the filters; the clear button resets them all.

## Technical notes

Filter gallery assets by filename keyword, date-from, and date-to via a `findByFolderWithFilters` JPQL query; 400 ms debounced search input

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
