## Context

The gallery currently renders assets in a flat paginated grid using `GET /api/assets`, with infinite scroll powered by an `IntersectionObserver`. Sort options include `FILE_CREATION_DATE_TIME` and `FILE_MODIFICATION_DATE_TIME`, but there is no visual grouping — a user who sorts by date sees thumbnails in order but has no way to tell where one day ends and another begins.

> **Architecture note:** This design targets the post-hexagonal-architecture backend. The new endpoint is implemented as `GetAssetsTimelineUseCase` / `GetAssetsTimelineUseCaseImpl`, the domain group model is `TimelineGroup` in `domain/model/`, the HTTP DTO is `TimelineGroupDto` in `infrastructure/web/dto/`, and filtering reuses `AssetFilter` from `application/dto/`.

The desired outcome is a **Timeline** view mode that groups assets by day (newest first) with month + day headings, while keeping all existing filters (search, date range, min rating, search presets) working identically.

## Goals / Non-Goals

**Goals:**
- New backend endpoint that returns assets pre-grouped by date.
- Toggle in the gallery toolbar to switch between Grid and Timeline view modes.
- Sticky month-header labels and day sub-headers in the timeline layout.
- Infinite scroll continues to work: each scroll loads the next batch of day groups.
- All existing gallery filters apply in timeline mode.

**Non-Goals:**
- Collapsible / expand-collapse per month or day (out of scope for v1).
- Year-level grouping or zooming (year → month → day drill-down).
- Persisting the selected view mode across sessions.
- Timeline view for Albums or Recycle Bin pages.

## Decisions

### 1. Server-side grouping via a dedicated endpoint

**Decision:** Add `GET /api/assets/timeline` that returns `PaginatedData<TimelineGroupDto>`, where each `TimelineGroupDto` carries a `localDate`, a formatted `label` string, and a `List<AssetDto>` of all assets for that day.

**Rationale:** The existing `/api/assets` endpoint returns a flat page of N items. With infinite scroll, pages can cut arbitrarily through a day's photos — grouping client-side would require buffering multiple pages before rendering a complete day, adding complexity and jitter. Server-side grouping ensures each response is self-contained per day, making the frontend purely a renderer.

**Alternatives considered:**
- *Client-side grouping*: simpler backend, but requires look-ahead buffering, complicates the IntersectionObserver logic, and causes layout reflows.
- *Single aggregate query returning all dates*: avoids pagination but is unbounded — a large library could return thousands of items in one call.

### 2. Pagination unit: days, not items

**Decision:** Each page of the timeline endpoint returns up to `timelinePageSize` days (default: 30), including all assets for each of those days. Page 0 is the most recent 30 days that contain at least one matching asset.

**Rationale:** Days are a natural pagination boundary for a timeline. Paginating by item count would split days across pages, requiring the front-end to merge partial day groups. Paginating by day count keeps groups intact and simplifies rendering.

**Trade-off:** A single day with an unusually large number of photos (e.g., 500+ from a bulk import) will be returned in full in one page. The existing individual-image thumbnail + lazy loading mitigates rendering cost.

### 3. Reuse existing filter parameters

**Decision:** `GET /api/assets/timeline` accepts the same query parameters as `GET /api/assets` (`folderPath`, `search`, `dateFrom`, `dateTo`, `minRating`), plus `page`. The `sort` parameter is omitted — timeline is always sorted by `FILE_CREATION_DATE_TIME` descending.

**Rationale:** Reusing the filter contract means the frontend can pass the same filter state to both endpoints with minimal branching. It also means search presets work without modification.

### 4. Frontend: `viewType` toggle, no URL state

**Decision:** `GalleryComponent` adds a `viewType: 'grid' | 'timeline'` field. The toggle is rendered as two icon-buttons in the toolbar. View type is not persisted to the URL or `localStorage`.

**Rationale:** Keeping state in-component avoids URL parameter proliferation. The view mode is a presentation preference, not a shareable filter, so session-level state is sufficient.

### 5. Timeline layout: CSS sticky headers

**Decision:** Month headers (`May 2025`) use `position: sticky; top: <toolbar-height>` so they remain visible while scrolling within a month. Day labels (`10` with day-of-week) are non-sticky inline headings above each day's thumbnail row.

**Rationale:** Pure CSS sticky avoids JavaScript scroll listeners. The Angular CDK virtual scroll is not used in timeline mode because variable-height groups (different number of photos per day) are poorly supported by fixed-size virtual scrolling.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| A day with 500+ photos causes a slow API response | Add a per-day item cap (e.g., 200) as a safety valve; surface the cap in the `TimelineGroupDto` with a `truncated: boolean` flag |
| Timeline endpoint duplicates much of the asset-filter logic | Reuse `AssetFilter` from `application/dto/` as the input to both `GetAssetsUseCase` and `GetAssetsTimelineUseCase`; shared Criteria API filtering lives in `AssetRepositoryAdapter` |
| Sticky headers break on mobile with the sidenav overlay | Apply sticky only when `sidenavOpen` is false on mobile; fall back to non-sticky on small viewports |
| No virtual scroll → large DOM on scroll | Limit `timelinePageSize` to 30 days; browser handles ~1500 thumbnail nodes well within that range |

## Migration Plan

- No database schema changes; no Flyway migration needed.
- The new endpoint is additive. Existing clients are unaffected.
- Feature is self-contained in the gallery component — no changes to routing or other features.

## Open Questions

- Should the timeline group label format be locale-aware (e.g., "10 May" vs "May 10")? For now, use Angular's `DatePipe` with the `'longDate'` format, which respects the browser locale.
- Should `dateFrom` / `dateTo` filter on `fileCreationDateTime` or `fileModificationDateTime` in timeline mode? Use `fileCreationDateTime` to match the group-by date, for consistency.
