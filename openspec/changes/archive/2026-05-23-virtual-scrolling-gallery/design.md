## Context

The gallery previously used an IntersectionObserver sentinel to trigger page loads as the user scrolled to the bottom of a CSS grid (`repeat(auto-fill, minmax(200px, 1fr))`). Assets were accumulated in a flat `assets` array and all loaded thumbnails were kept in the DOM. The `ScrollingModule` from `@angular/cdk/scrolling` was imported but unused.

The `ThumbnailComponent` card (used previously) rendered a 150px image with a ~30px info row, giving each card a variable rendered height (~200 px) that depended on the grid's auto-fill column calculation — incompatible with CDK's `FixedSizeVirtualScrollStrategy`.

`@angular/cdk/scrolling` (`ScrollingModule`) was already in the project's transitive dependencies as a peer of Angular Material 19 — no `package.json` change was needed.

## Goals / Non-Goals

**Goals:**

- Replace the CSS grid with a fixed-height list layout so each row has a known, constant height.
- Render only visible rows in the DOM via `CdkVirtualScrollViewport` + `*cdkVirtualFor`.
- Remove the IntersectionObserver sentinel from the grid view.
- Each row shows: thumbnail on the left (106×64 px), filename + size + date on the right.
- Auto-fetch all pages on folder/sort change without user scrolling (the viewport may be hidden or 0-height in some contexts).
- Keep the backend API (`GET /api/assets`) and `AssetService.getAssets()` signature unchanged.
- Retain the timeline view's IntersectionObserver sentinel (timeline uses variable-height buckets, incompatible with CDK fixed-size scroll).

**Non-Goals:**

- Keeping the responsive grid layout.
- Changing the backend pagination API.
- Server-side cursor pagination.

## Decisions

### 1. CDK virtual scroll with a fixed-height list — not a responsive CSS grid

**Decision:** Replace the grid with a single-column list where every row is exactly 80 px tall. Use `<cdk-virtual-scroll-viewport itemSize="80">` with `*cdkVirtualFor` to DOM-virtualise the list.

**Rationale:** `CdkVirtualScrollViewport` requires all items to share the same fixed height (`itemSize`). A single-column list with an 80 px row (thumbnail + metadata) satisfies this constraint trivially. The responsive grid (`repeat(auto-fill, minmax(200px, 1fr))`) has variable row heights dependent on viewport width and cannot be virtualised without grouping items into row arrays of a dynamically calculated size — a significantly more complex approach.

**Alternative considered:** Keep the CSS grid and IntersectionObserver for infinite scroll. This was what the previous design implemented. Rejected because it does not virtualise the DOM — all loaded nodes remain in memory — and leaves `ScrollingModule` imported but idle.

**Alternative considered:** Fixed-column-count grid (e.g., always 5 columns) with CDK. Rejected because it removes responsive behaviour and still requires a resize observer to recalculate row groups on window resize.

### 2. Auto-fetch all pages on folder/sort change (`loadNextPage(continueLoading = true)`)

**Decision:** `loadNextPage(continueLoading = false)` is the base signature. When called with `continueLoading = true`, each successful page response immediately triggers the next page until `allLoaded`. `onFolderSelected`, `setViewType`, `loadAssets`, and `onSortChange` all call `loadNextPage(true)` to auto-populate the list on state changes.

**Rationale:** The CDK virtual scroll viewport may have zero rendered height when the component first initialises (e.g. in Cypress component tests or before CSS layout settles). In a zero-height viewport the scroll sentinel would never enter view, so relying solely on scroll events to trigger subsequent pages would leave the list empty after page 0. Auto-fetching all pages on folder/sort change ensures correctness regardless of viewport size. The default `continueLoading = false` preserves the existing sentinel-triggered behaviour for future scroll-based loading if needed.

### 3. Retain IntersectionObserver sentinel only for the timeline view

**Decision:** The `#scrollSentinel` div and its `IntersectionObserver` observer are kept in the component but are only rendered and observed when `viewType === 'timeline'`. The grid view uses auto-fetch instead.

**Rationale:** The timeline view groups assets into year/month buckets with variable-height bucket headers. It cannot use CDK fixed-size virtual scroll. The existing sentinel pattern is the correct infinite-scroll mechanism for the timeline. Removing it entirely would break timeline loading.

### 4. Replace `ThumbnailComponent` with inline list row

**Decision:** The `ThumbnailComponent` import is removed. Each row is rendered inline in the `*cdkVirtualFor` template with an `<img class="list-thumb">`, a `.list-meta` div (filename + size/date), and a show-on-hover "Add to album" icon button.

**Rationale:** `ThumbnailComponent` rendered a card-style layout designed for the grid. The new list row is structurally different (horizontal rather than vertical, no card chrome) and shares no meaningful structure with the old card. Inlining avoids an unnecessary component wrapper and keeps CDK's item tracking in direct contact with the DOM nodes it virtualises.

### 5. Guard against concurrent and redundant fetches

**Decision:** `loadNextPage()` returns immediately if `isLoading || allLoaded || !currentFolder`. The `allLoaded` boolean is set when `pageIndex >= data.totalPages` after each successful response.

**Rationale:** `continueLoading = true` calls `loadNextPage` recursively. Without the guard, parallel responses could race and produce duplicate pages. The guard ensures at most one in-flight request at any time.

### 6. Selection tracking and status bar unchanged

**Decision:** `selectedAssets: Set<number>` and the status bar ("X of Y photos", "X selected", `statusMessage`) are unchanged.

**Rationale:** These are display concerns orthogonal to the virtualisation change. No redesign was needed.

## Data Flow

```
User opens folder / changes sort / changes view type
  → onFolderSelected() / onSortChange() / loadAssets()
    → reset: assets=[], pageIndex=0, isLoading=false, allLoaded=false
    → (if timeline) setupSentinelObserver() + loadTimelinePage()
    → (if grid)     loadNextPage(true)
      → isLoading = true
      → assetService.getAssets(folder, pageIndex, sort)  [GET /api/assets]
        → PaginatedData<Asset> response
          → assets = [...assets, ...data.items]
          → totalItems = data.totalItems
          → pageIndex++
          → allLoaded = (pageIndex >= data.totalPages)
          → isLoading = false
          → if (continueLoading && !allLoaded) loadNextPage(true)  ← recursive

CDK virtual scroll renders only visible rows (+ buffer)
  → DOM contains ~10–15 row elements regardless of total asset count
```

## File Change List

**New files:**

_(none)_

**Modified files:**

- `frontend/src/app/features/gallery/gallery.component.ts`
  - Remove `ThumbnailComponent` from imports; add `FileSizePipe`
  - Add `trackByAssetId(_index, asset)` method
  - Change `loadNextPage(continueLoading = false)` — recursive auto-fetch when `continueLoading = true`
  - `setupSentinelObserver()` callback calls `loadTimelinePage()` only (grid branch removed)
  - `onFolderSelected`, `setViewType`, `loadAssets`, `onSortChange` use `loadNextPage(true)` for grid; timeline path unchanged

- `frontend/src/app/features/gallery/gallery.component.html`
  - Grid section replaced: `<cdk-virtual-scroll-viewport itemSize="80">` with `*cdkVirtualFor` rows
  - Each row: `<img class="list-thumb">`, `.list-meta` (filename + `fileSize | fileSize` + `fileCreationDateTime | date`), hover album button
  - Sentinel `@if` changed from `viewMode === 'thumbnails'` to `viewMode === 'thumbnails' && viewType === 'timeline'`

- `frontend/src/app/features/gallery/gallery.component.scss`
  - Removed: `.thumbnail-grid-container`, `.asset-cell`, old `.scroll-sentinel`
  - Added: `.thumbnail-list-container`, `.asset-virtual-viewport`, `.asset-list-row`, `.list-thumb`, `.list-meta`, `.list-album-btn`, `.scroll-sentinel`, `.loading-row`, `.end-of-list`, `.empty-state`

- `frontend/src/styles.scss`
  - Removed: `.thumbnail-grid` rule and its `@media (max-width: 767px)` override

- `frontend/src/app/features/gallery/gallery.component.cy.ts`
  - `cy.get('app-thumbnail')` assertions replaced with `cy.get('.asset-list-row')`
  - Rating tests changed from DOM star-click interactions to direct `component.rateAsset(asset, star)` method calls (no star UI in list view)
