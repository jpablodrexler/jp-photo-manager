## Context

The existing `assets` table already stores `file_size`, `file_name` (from which the extension can be derived), `file_creation_date_time`, and `rating` on every catalogued asset, and a `folders` table stores the folder path. `AssetRepository` already has `sumFileSize()` and `findTopFoldersByAssetCount()` used by the home dashboard, proving the query patterns are established and performant.

The frontend has Angular Material cards, a standard `HttpClient` service layer, and a lazy-routing pattern consistent across all features. It does not yet have a charting library — all current visualisations (the top-folders progress bars on the home page) are hand-built HTML. Adding a single focused analytics page justifies pulling in a dedicated charting library.

No new database tables or Flyway migrations are needed: all four aggregates read from existing schema columns.

## Goals / Non-Goals

**Goals:**

- Expose `GET /api/analytics` returning `{ folderStorage, formatDistribution, photosPerMonth, ratingDistribution }` built entirely from aggregate JPQL queries on the existing schema.
- Add `GetAnalyticsUseCase` following the established hexagonal port/adapter pattern.
- Render four `ngx-charts` charts in a new `/analytics` route: treemap (storage per folder), pie (format distribution), vertical bar (photos per month), vertical bar (rating distribution).
- Integrate `@swimlane/ngx-charts` as a production dependency via `npm install`.
- Add an "Analytics" navigation link in `AppComponent` visible only when authenticated.

**Non-Goals:**

- Real-time updates or SSE streaming for analytics data — a single `GET` on page load is sufficient.
- Per-user analytics scoping — the charts aggregate across all catalogued assets regardless of who catalogued them; this is consistent with how the gallery and home dashboard work.
- Drill-down navigation (e.g. clicking a folder slice to open the gallery filtered to that folder) — a nice future enhancement but out of scope.
- Export to CSV or image — out of scope.
- Flyway migration — no new tables or columns are required.

## Decisions

### 1. Single `GET /api/analytics` endpoint returning all four aggregates

**Decision:** A single endpoint assembles all four dataset arrays in one call and returns them as a composite `AnalyticsResponseDto`.

**Rationale:** All four queries are read-only aggregations against the same tables. Combining them avoids four round-trips from the frontend on page load, and none of the queries is expensive enough to justify lazy individual loading. The payload is small (bounded by the number of distinct folders, extensions, months, and rating levels).

**Alternative considered:** Four separate endpoints (`/api/analytics/folder-storage`, `/api/analytics/format-distribution`, etc.) — rejected because it multiplies HTTP overhead with no benefit at this scale.

### 2. No new tables or Flyway migration

**Decision:** All four aggregations are derived entirely from columns already present in `assets` and `folders`. File extension is extracted with a JPQL `FUNCTION('substring', …)` expression or native SQL `split_part` / `regexp_replace`; storage per folder uses `SUM(a.fileSize)` grouped by folder; photos per month uses `FUNCTION('to_char', a.fileCreationDateTime, 'YYYY-MM')`; rating distribution uses `GROUP BY a.rating`.

**Rationale:** Adding computed columns or materialised views for analytics on a dataset of this size would be over-engineering. Aggregate queries over a typical home photo library (tens of thousands of rows) complete in milliseconds. Keeping schema changes out of this change avoids a Flyway migration version bump and reduces risk.

**Alternative considered:** A separate `analytics_cache` table pre-computed on a schedule — rejected as premature optimisation.

### 3. `@swimlane/ngx-charts` as the charting library

**Decision:** Install `@swimlane/ngx-charts` and use its `TreeMapComponent`, `PieChartComponent`, and `BarVerticalComponent`.

**Rationale:** `ngx-charts` is an Angular-native D3-based library with zero additional peer dependencies beyond D3, ships tree-shakeable ES modules, and is the most widely adopted Angular charting library. Its chart components are standalone-compatible (each exported as an Angular standalone component). The treemap, pie, and bar chart primitives map directly to the four required visualisations.

**Alternative considered:** Apache ECharts (`ngx-echarts`) — broader chart variety but a heavier bundle (~500 KB gzipped vs ~150 KB for ngx-charts at tree-shaken scope). Chart.js (`ng2-charts`) — no treemap support out of the box.

### 4. `AnalyticsData` as a Lombok `@Value` (immutable) record-like class

**Decision:** Represent the analytics result as a Lombok `@Value @Builder` class in `domain/model/`:

```java
@Value
@Builder
public class AnalyticsData {
    List<FolderStorageEntry> folderStorage;
    List<FormatEntry> formatDistribution;
    List<MonthlyCountEntry> photosPerMonth;
    List<RatingEntry> ratingDistribution;
}
```

Each inner record (`FolderStorageEntry`, `FormatEntry`, `MonthlyCountEntry`, `RatingEntry`) is a Java `record`.

**Rationale:** The analytics response is assembled once per request and never mutated. Using `@Value` (immutable) makes the intent explicit and prevents accidental field mutation in tests or future code. Inner types as Java `record` are idiomatic Java 21 and self-documenting.

**Alternative considered:** A `@Data` mutable class — rejected because analytics data has no reason to be mutated after construction.

### 5. File extension extracted via JPQL `LOWER(SUBSTRING(a.fileName, LOCATE('.', a.fileName) + 1))`

**Decision:** Use a pure JPQL expression to derive the extension from `fileName` so the query remains database-portable without requiring a native SQL function.

**Rationale:** `LOCATE` and `SUBSTRING` are standard JPQL string functions supported by Hibernate. Using JPQL keeps the query provider-agnostic (no PostgreSQL-specific `split_part` needed) and consistent with the existing query style in `JpaAssetRepository`.

**Alternative considered:** Native SQL `split_part(file_name, '.', -1)` — more concise but ties the query to PostgreSQL; rejected in favour of portability.

## Risks / Trade-offs

**Extension extraction edge cases**
→ Files without an extension (no `.` in the filename) will produce an empty string or the full filename as the "extension" depending on `LOCATE` returning 0. The query should filter out `LOCATE('.', a.fileName) = 0` or map blanks to `"unknown"`. This is handled in the `GetAnalyticsUseCaseImpl` post-processing step.

**Performance on large libraries**
→ Aggregate queries over hundreds of thousands of rows may become slow without indexes. The `file_creation_date_time` and `rating` columns are not currently indexed. For a home photo library (typically < 100k assets) this is not a concern; the trade-off is acceptable at this stage.

**`ngx-charts` Angular 19 compatibility**
→ `@swimlane/ngx-charts` targets Angular 17+ with standalone component support. Verify the installed version's peer dependency range covers Angular 19 during implementation. If incompatible, `ngx-echarts` is the fallback.

**Treemap readability for many folders**
→ A treemap with hundreds of folders produces many tiny cells that are hard to read. Consider capping the treemap to the top 20 folders by storage in `GetAnalyticsUseCaseImpl` and grouping the remainder as "Other".
