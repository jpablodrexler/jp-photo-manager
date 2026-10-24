# Feature 2 — virtual-scrolling-gallery

**As a** photo library owner, **I want** a gallery that renders only the visible rows of a list layout, **so that** large folders scroll smoothly and load page by page.

## Acceptance Criteria
- AC1: Given a folder, when it loads, then a fixed-height list shows each row with the thumbnail on the left and the filename, size and date on the right.
- AC2: Given more pages exist, when I scroll toward the end, then the next page loads automatically and pages accumulate; rapid scrolling does not trigger duplicate requests.
- AC3: Given all pages are loaded, when I scroll, then no further load is requested.
- AC4: Given a fetch is in progress, when the list renders, then a loading indicator is shown and hidden afterwards.
- AC5: Given I change the folder or the sort, when the change applies, then the list resets to the first page.
- AC6: Given the list, when it renders, then only the visible items are in the DOM and the status shows partial or full load.

## Technical notes

Replace the CSS grid with a fixed-height list layout; render only visible items in the DOM via `CdkVirtualScrollViewport` with `*cdkVirtualFor`; remove the unused `ScrollingModule` import and the IntersectionObserver sentinel; each list row shows the thumbnail on the left and filename, size, and date metadata on the right

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
