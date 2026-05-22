## Why

The home page currently shows three static stat cards (folder count, asset count, last catalog time) and nothing else — it is the least informative screen in the app. Users land there after login and immediately navigate away because it offers no actionable information. Transforming it into a real dashboard gives users an at-a-glance view of their library health and direct paths to the most common actions.

## What Changes

- **Extend `GET /api/home/stats`** to return additional fields: `totalFileSize`, `duplicateCount`, `topFolders` (top 5 by asset count), and `recentAssets` (last 12 cataloged assets with thumbnail URLs).
- **Rewrite `HomeComponent`** with a richer multi-section layout:
  - **Quick Actions row** — buttons linking to Gallery, Catalog, Sync, and Duplicates; the Duplicates button shows a warning badge when `duplicateCount > 0`.
  - **Library summary cards** — the existing three stat cards plus a new "Total size" card.
  - **Recent Photos strip** — a scrollable row of the last 12 thumbnail cards; clicking one navigates to the gallery at that asset's folder.
  - **Top Folders list** — a ranked list of the 5 folders with the most assets, each showing a proportional bar and asset count.
- No changes to any other page or route.

## Capabilities

### New Capabilities
- `home-dashboard`: The enriched home page — recent photos strip, quick-action buttons, duplicate alert, total file size stat, and top-folders breakdown.

### Modified Capabilities

## Impact

- **Backend:** `HomeStats` record gains four new fields; `PhotoManagerFacadeImpl.getHomeStats()` runs additional queries (recent assets, duplicate count, folder totals, total file size); no new endpoints or Flyway migrations needed.
- **Frontend:** `HomeStats` model updated; `HomeComponent` template and styles substantially rewritten; `ThumbnailComponent` reused for the recent-photos strip; `RouterLink` used for quick-action navigation.
- **No breaking changes** — the existing three fields remain on `HomeStats`; the endpoint path and method are unchanged.
