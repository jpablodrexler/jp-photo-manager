# Feature 20 — storage-analytics

**As a** photo library owner, **I want** a storage analytics page with charts about my library, **so that** I understand where my space goes and how my library grows.

## Acceptance Criteria
- AC1: Given a populated library, when the analytics API is called, then it returns storage per folder (descending, more than 20 folders collapsed into "other"), file formats (lowercased, no extension as "unknown"), photos per month (months with none omitted, null dates excluded) and the rating distribution (0 for unrated).
- AC2: Given an empty catalog, when the API is called, then empty data is returned; deleted assets are excluded.
- AC3: Given an unauthenticated request, when it calls the API, then it is rejected; unauthenticated users are redirected to login.
- AC4: Given /analytics, when the data loads, then four charts render; a spinner shows while loading and an error message if the call fails.
- AC5: Given the navigation, when I am signed in, then an Analytics link is visible; when signed out, absent.

## Technical notes

`/analytics` route with `ngx-charts` visualizations: storage-per-folder treemap, file-format pie, photos-per-month histogram, rating distribution bar; backed by aggregate JPQL queries

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
