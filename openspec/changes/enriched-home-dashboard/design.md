## Context

`GET /api/home/stats` currently returns a single `HomeStats` record with three fields: `folderCount`, `assetCount`, and `lastCatalogCompletedAt`. The `HomeComponent` renders these as three `MatCard` tiles and nothing else. The page is stateless — it loads once on navigation and has no interactive elements.

The backend already has all the data needed to power a richer dashboard:
- `AssetRepository` can count assets grouped by folder and sum file sizes.
- `FindDuplicatedAssetsService` already computes duplicate groups for the `/duplicates` route.
- `AssetRepository` can return the most recently cataloged assets ordered by `thumbnailCreationDateTime`.

> **Architecture note:** This design targets the post-hexagonal-architecture backend. Business logic lives in `application/usecase/home/GetHomeStatsUseCaseImpl`, repository queries are declared on `domain/port/out/AssetRepository` and implemented in `infrastructure/persistence/adapter/AssetRepositoryAdapter`, and projections are defined in `infrastructure/persistence/jpa/`.

No new tables, migrations, or external dependencies are needed.

## Goals / Non-Goals

**Goals:**
- Extend `HomeStats` with `totalFileSize`, `duplicateCount`, `topFolders`, and `recentAssets`.
- Rewrite `HomeComponent` with a multi-section layout: quick actions, stat cards, recent photos strip, top folders list.
- Navigate to the gallery pre-filtered to the asset's folder when a recent photo thumbnail is clicked.
- Show a visual warning badge on the Duplicates action when `duplicateCount > 0`.

**Non-Goals:**
- Real-time dashboard updates (polling or SSE) — a single load on navigation is sufficient.
- Charts or graph libraries — the top-folders bar is a pure CSS proportional bar, no third-party chart dependency.
- Personalization or user-specific dashboards — all data is library-wide.
- Caching the stats response — queries are cheap at gallery scale; no cache layer needed.

## Decisions

### 1. Extend `HomeStats` record rather than create a new endpoint

**Decision:** Add the four new fields directly to the existing `HomeStats` Java record and populate them in `GetHomeStatsUseCaseImpl`. The endpoint path and HTTP method remain unchanged.

**Rationale:** A single endpoint call on page load is the simplest contract. Adding a second endpoint would require the frontend to make multiple parallel requests and merge results, adding latency and error-handling complexity. The `HomeStats` record is only used by `HomeController` and `HomeComponent` — extending it has zero ripple effect.

**Alternatives considered:** *Separate endpoints per section* (e.g., `/api/home/recent-assets`, `/api/home/top-folders`) — more granular and independently cacheable, but premature optimization for a dashboard that loads once per navigation.

### 2. Duplicate count via a dedicated query, not full group computation

**Decision:** `getHomeStats()` runs a single `COUNT(DISTINCT asset.hash) WHERE hash IN (SELECT hash FROM assets GROUP BY hash HAVING COUNT(*) > 1)` query to get `duplicateCount`. It does not call `FindDuplicatedAssetsService.findDuplicatedAssets()` (which returns full group lists).

**Rationale:** The full duplicate computation loads all duplicate asset records into memory. For the dashboard we only need the count — a scalar subquery is orders of magnitude cheaper. `FindDuplicatedAssetsService` is still the right entry point for the `/duplicates` page.

### 3. Recent assets: last 12 by `thumbnailCreationDateTime`

**Decision:** `recentAssets` is a `List<AssetSummary>` (a new lightweight projection: `assetId`, `fileName`, `folderPath`, `thumbnailUrl`) of the 12 most recently cataloged assets across all folders, ordered by `thumbnailCreationDateTime DESC`.

**Rationale:** 12 fills a 4-column × 3-row grid at typical viewport widths. `thumbnailCreationDateTime` is the most reliable "when was this added to the library" timestamp — it is always set by the catalog process. `fileCreationDateTime` can be null or reflect the original camera date, which is not what "recently added" means.

**Alternatives considered:** *Per-folder recent assets* — requires knowing which folder the user cares about; not useful on the home page. *File modification date* — reflects disk edits, not catalog additions.

### 4. Top folders: top 5 by asset count, proportional CSS bar

**Decision:** `topFolders` is a `List<FolderStat>` (projection: `path`, `assetCount`) of the 5 folders with the most assets. The frontend renders the bar width as `(assetCount / maxAssetCount) * 100%` in pure CSS — no chart library.

**Rationale:** 5 folders is enough to show the shape of the library without overwhelming the page. Pure CSS bars keep the bundle size zero and match the existing Material design language. Adding a chart library (Chart.js, ngx-charts) for a single bar list is not justified.

### 5. Click-to-navigate on recent photo thumbnail

**Decision:** Clicking a recent photo thumbnail navigates to `/gallery` with a `?folder=<folderPath>` query parameter. `GalleryComponent` reads this parameter on `ngOnInit` and pre-selects the folder in `FolderNavComponent`.

**Rationale:** This is the most direct path from "I see a photo on the dashboard" to "I'm browsing that photo's folder". The `?folder` query param is a lightweight, bookmarkable way to pass the initial folder without adding a route segment or shared state service.

**Open question:** `GalleryComponent` does not currently read query params — it relies on `FolderNavComponent` emitting a `folderSelected` event. The implementation will need to read `ActivatedRoute.queryParams` on init and programmatically trigger the folder selection if a `folder` param is present.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `getHomeStats()` now runs 4 queries instead of 1; latency increases | All queries are indexed and scalar or paginated to 12/5 rows — total added latency is <50 ms at gallery scale |
| `duplicateCount` query is approximate (counts by hash, not by group) | The count is used only for the alert badge, not for the duplicates page itself; approximate is acceptable |
| `?folder` query param navigation requires changes to `GalleryComponent` | Scope is small: read one query param in `ngOnInit`, call existing `onFolderSelected()` — one method, no new state |
| `AssetSummary` projection requires a new JPQL or interface-based projection | Use a Spring Data interface projection `AssetSummary` in `infrastructure/persistence/jpa/` with `getAssetId()`, `getFileName()`, `getFolderPath()` — no new entity class needed |

## Migration Plan

- No Flyway migration needed — no schema changes.
- `HomeStats` record change is additive; any client that ignores unknown JSON fields is unaffected (the frontend is the only consumer).
- Rollback: revert `HomeStats` to three fields and restore the original `HomeComponent` template.
