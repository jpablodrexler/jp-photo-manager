# Feature 11 — star-ratings

**As a** photo library owner, **I want** to rate each photo from 0 to 5 stars, filter and sort by rating, **so that** I can find my best photos quickly.

## Acceptance Criteria
- AC1: Given a newly cataloged asset, when I look at it, then its rating is 0 (unrated).
- AC2: Given an asset, when I set a rating from 0 to 5, then it is saved; a value outside 0–5 is rejected; 0 marks it as unrated.
- AC3: Given a minimum rating filter, when I apply it, then only assets at or above it are returned; absent, no filter applies; a minimum of 1 excludes only unrated assets.
- AC4: Given the sort option, when I sort by rating, then the highest-rated assets come first.
- AC5: Given thumbnails and the viewer, when they render, then they show the current rating; clicking a star rates, and clicking the active star clears it.
- AC6: Given the filter control, when I select minimum 3, then the grid is filtered; clicking the active filter star clears it.
- AC7: Given an asset is soft-deleted and restored, when it returns, then its rating is preserved.

## Technical notes

0–5 star rating per asset stored in the database; rating widget on thumbnails and in the viewer; filter by minimum rating; sort by rating descending

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
