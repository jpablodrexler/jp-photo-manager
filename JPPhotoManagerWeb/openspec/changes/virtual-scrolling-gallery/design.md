## Context

The gallery currently uses a page-based model: `GalleryComponent` holds a `pageIndex` integer and calls `assetService.getAssets(folder, pageIndex, sort)`, which maps to `GET /api/assets?folderPath=&page=&sort=`. The backend returns a `PaginatedData<AssetDto>` object with `items`, `pageIndex`, `totalPages`, and `totalItems`. The frontend replaces `assets` with each page's `items` and shows prev/next buttons. The existing CSS grid in `styles.scss` uses `repeat(auto-fill, minmax(200px, 1fr))` — a responsive layout whose column count varies with viewport width.

The `ThumbnailComponent` is a standalone card with a 150px image and ~30px info row, giving each card a rendered height of roughly 200px (including gap).

`@angular/cdk/scrolling` (`ScrollingModule`) is available as a peer dependency of Angular Material 19, already in the project's transitive dependencies — no `package.json` change is needed.

## Goals / Non-Goals

**Goals:**

- Replace the prev/next pagination buttons with continuous-scroll page loading.
- Show a loading spinner while additional pages are being fetched.
- Show an end-of-list indicator when all pages have been loaded.
- Reset the accumulated asset list on folder change or sort change and fetch page 0 fresh.
- Keep the backend API (`GET /api/assets`) unchanged.
- Keep the `AssetService.getAssets()` method signature unchanged.
- Show total count as "X of Y photos" in the status area.

**Non-Goals:**

- Removing already-loaded DOM nodes as the user scrolls (full virtual DOM removal).
- Changing the responsive CSS grid layout (`repeat(auto-fill, minmax(200px, 1fr))`).
- Infinite back-scrolling to previous pages (loaded pages stay in DOM).
- Server-side cursor pagination (the existing offset-based page API is retained).

## Decisions

### 1. IntersectionObserver sentinel — not `CdkVirtualScrollViewport`

**Decision:** Detect scroll-to-bottom via a sentinel `<div>` at the end of the thumbnail grid observed by `IntersectionObserver`. When the sentinel enters the viewport, the next page is fetched and appended to `assets`.

**Rationale:** `CdkVirtualScrollViewport` is designed for single-column lists where every item has a known, fixed height. The existing grid uses `repeat(auto-fill, minmax(200px, 1fr))` — the number of columns changes with viewport width, so row heights are not fixed and the column count is not statically known. Adapting CDK virtual scroll to a responsive grid requires grouping assets into row arrays of a dynamic size and re-grouping on every resize event. This is significantly more complex than the IntersectionObserver approach and provides no meaningful benefit for the expected folder sizes (hundreds to low thousands of images). An `IntersectionObserver` with a bottom sentinel is the standard infinite-scroll pattern, is supported in all modern browsers with no polyfill, and integrates cleanly with the existing CSS grid.

**Alternative considered:** `CdkVirtualScrollViewport` with a fixed column count (e.g., always 5 columns, ignoring viewport width). Rejected because it removes the responsive grid behavior, requiring a separate SCSS rewrite, and still needs resize handling to recalculate rows.

**Alternative considered:** Scroll event listener on `.thumbnail-grid-container` comparing `scrollTop + clientHeight` to `scrollHeight`. Rejected in favour of `IntersectionObserver` because the observer fires asynchronously off the main thread and does not require debouncing — it is more performant and simpler to implement than a scroll event handler.

### 2. Accumulate pages in a flat `assets` array

**Decision:** Each successful `getAssets()` response appends its `items` to the existing `assets` array (spread or `push`). Pages already rendered remain in the DOM as the user scrolls down.

**Rationale:** For typical photo library folders (up to a few thousand images), keeping all loaded thumbnails in the DOM is not a performance concern — the browser handles CSS grid layout efficiently at this scale. Full DOM virtualisation (removing off-screen nodes) adds significant complexity for negligible gain at these sizes. If a folder contains tens of thousands of images, only the first few pages will be visible at any one time anyway, since the user reaches the end-of-list indicator and stops scrolling.

### 3. Guard against concurrent and redundant fetches

**Decision:** `GalleryComponent` maintains an `isLoading` boolean and an `allLoaded` boolean. `loadNextPage()` returns immediately if either is `true` or if `currentFolder` is empty. This prevents duplicate requests when the sentinel fires multiple times in rapid succession.

**Rationale:** `IntersectionObserver` can fire multiple times as the user scrolls quickly. Without a guard, multiple in-flight requests could arrive out of order and append duplicate items.

### 4. Reset on folder or sort change — re-observe sentinel

**Decision:** When `onFolderSelected()` or `onSortChange()` is called: set `assets = []`, `pageIndex = 0`, `allLoaded = false`, `isLoading = false`, disconnect and re-observe the sentinel, then call `loadNextPage()`.

**Rationale:** The sentinel may already be intersecting when the user switches folders (e.g., switching from a large folder to a small one where the sentinel is immediately in view). Re-observing after the reset ensures the observer fires correctly for the new folder.

### 5. Pagination bar replaced by status line in existing status bar

**Decision:** Remove the `<div class="pagination-bar">` block and its `prevPage()` / `nextPage()` methods and `totalPages` field entirely. Render total count in the existing `.status-bar` as "X of Y photos" (where X = `assets.length`, Y = `totalItems`). An `isLoading` indicator (small `mat-progress-bar` below the grid) and an `allLoaded` end-of-list label are added inside the thumbnail-grid container.

**Rationale:** The status bar already exists and is the right place for summary information. Removing the pagination bar simplifies the template and eliminates the visual context switch that currently happens on each page flip.

## Data Flow

```
User opens folder / changes sort
  → onFolderSelected() / onSortChange()
    → reset: assets=[], pageIndex=0, isLoading=false, allLoaded=false
    → disconnect + re-observe sentinel
    → loadNextPage()
      → isLoading = true
      → assetService.getAssets(folder, pageIndex, sort)  [GET /api/assets]
        → PaginatedData<Asset> response
          → assets = [...assets, ...data.items]
          → totalItems = data.totalItems
          → pageIndex++
          → allLoaded = (pageIndex >= data.totalPages)
          → isLoading = false

IntersectionObserver fires (sentinel enters viewport)
  → loadNextPage()           [guard: returns if isLoading || allLoaded]
    → [same fetch flow as above]

User scrolls up
  → no action (loaded items remain in DOM)
```

## File Change List

**New files:**

_(none)_

**Modified files:**

- `frontend/src/app/features/gallery/gallery.component.ts` — remove `prevPage`, `nextPage`, `totalPages`; add `totalItems`, `isLoading`, `allLoaded`, `sentinel` (`ElementRef`); add `loadNextPage()`, `setupSentinelObserver()`, `disconnectObserver()`; update `onFolderSelected` and `onSortChange` to reset state and re-observe; update `loadAssets` to append rather than replace `assets`; add `ScrollingModule` to `imports`
- `frontend/src/app/features/gallery/gallery.component.html` — remove `.pagination-bar` block; add sentinel `<div #scrollSentinel>` at the end of the grid; add `<mat-progress-bar>` loading row; add "All photos loaded" end-of-list label; update status bar to show "X of Y photos"
- `frontend/src/app/features/gallery/gallery.component.scss` — remove `.pagination-bar` rule (already in `styles.scss`); add `.scroll-sentinel`, `.loading-row`, `.end-of-list` rules
- `frontend/src/styles.scss` — remove `.pagination-bar` global rule (no longer used)
- `frontend/src/app/features/gallery/gallery.component.cy.ts` — remove pagination button assertions; add tests for sentinel-triggered page load, reset-on-folder-change, reset-on-sort-change, loading bar visibility, end-of-list indicator
