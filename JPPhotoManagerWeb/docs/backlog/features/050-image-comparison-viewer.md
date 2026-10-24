# Feature 50 — image-comparison-viewer

**As a** photo library owner, **I want** to compare two photos side by side, **so that** I can pick the better shot or confirm a duplicate.

## Acceptance Criteria
- AC1: Given exactly two selected assets, when I look at the toolbar, then a Compare button is enabled; with one, or three or more, it is not shown.
- AC2: Given I press Compare, when the view opens, then both images show side by side with filename, size, dimensions and rating beneath each.
- AC3: Given the zoom slider, when I move it, then both images scale equally.
- AC4: Given I close the view, when it closes, then I return to the gallery.

## Technical notes

Select exactly two assets in the gallery → "Compare" action opens a split-screen view showing both images side by side at matched zoom levels, with filename, size, dimensions, and rating displayed beneath each panel; most useful as a companion to the duplicates workflow but available from any multi-selection; no new backend endpoint — both images are served by the existing `GET /api/assets/{id}/image`; new `ComparisonViewerComponent` in `features/gallery/`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
