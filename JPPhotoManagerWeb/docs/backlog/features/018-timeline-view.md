# Feature 18 — timeline-view

**As a** photo library owner, **I want** a timeline view that groups photos by year and month, **so that** I browse my library chronologically.

## Acceptance Criteria
- AC1: Given the gallery, when I choose the timeline mode, then photos are grouped by year then month under sticky date headers.
- AC2: Given the timeline groups endpoint, when it is called, then it returns the number of assets per bucket.
- AC3: Given a bucket scrolls into view, when it intersects the viewport, then its assets load lazily.
- AC4: Given a bucket outside the viewport, when I scroll elsewhere, then its assets are not loaded.

## Technical notes

Third gallery view mode grouping assets by year then month with sticky date headers; `GET /api/assets/timeline-groups` returns counts per bucket; buckets load lazily via Intersection Observer

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
