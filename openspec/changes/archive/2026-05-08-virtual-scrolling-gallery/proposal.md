## Why

The gallery's current pagination model forces users to click through pages of 100 items to browse a large folder. Every "Next" click issues a new `GET /api/assets` request, clears the currently visible thumbnails, and re-renders the grid — breaking visual continuity. For folders with thousands of photos this interaction pattern is frustrating: the user loses their position and cannot quickly scan a long sequence without repeatedly clicking a button.

Virtual scrolling replaces the page-flip model with a continuous scroll experience. Only the thumbnails currently in the viewport are rendered in the DOM, keeping memory and layout cost flat regardless of how many images have been loaded. As the user scrolls toward the end of what has already been fetched, the component silently appends the next page — no button click required. The backend API is unchanged; the frontend simply consumes the existing `GET /api/assets` endpoint page-by-page and accumulates the results.

## What Changes

- Remove the prev/next pagination buttons from `GalleryComponent` and the `prevPage()` / `nextPage()` methods that drive them.
- Add `ScrollingModule` (from `@angular/cdk/scrolling`) to `GalleryComponent`'s `imports` array.
- Replace the `.thumbnail-grid-container` `<div>` with `<cdk-virtual-scroll-viewport>` using a fixed `itemSize` matching the rendered height of each thumbnail row.
- Replace the `@for` loop over `assets` with `*cdkVirtualFor`, which renders only the DOM nodes for items currently in the viewport.
- Add an `(scrolledIndexChange)` handler (or a `scroll` host listener on the viewport) that detects when the user is within the last 20% of loaded items and calls a new `loadNextPage()` method.
- Add `loadNextPage()` to `GalleryComponent`: guard against concurrent in-flight requests and end-of-data; call `assetService.getAssets()` with the next `pageIndex`, then append the response items to the existing `assets` array rather than replacing it.
- Add `isLoading` and `allLoaded` boolean signals to drive a loading spinner and an "All photos loaded" end-of-list indicator.
- Reset `assets`, `pageIndex`, `isLoading`, and `allLoaded` on folder change or sort change, then immediately fetch page 0.
- Replace the "X / Y pages (Z items)" pagination text with a status line showing "X of Y photos".
- Add component SCSS for the virtual scroll viewport height, a per-row wrapper needed by `CdkVirtualScrollViewport`'s fixed-size strategy, a bottom spinner row, and an end-of-list label.

## Capabilities

### New Capabilities

- `virtual-scrolling-gallery`: The gallery thumbnail grid renders via `CdkVirtualScrollViewport` so only visible thumbnails are in the DOM. As the user scrolls down, additional pages are fetched from the backend and appended seamlessly with no pagination UI.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`gallery.component.ts`**: remove `prevPage`, `nextPage`, `totalPages` fields; add `isLoading`, `allLoaded`, `loadNextPage()`; modify `onFolderSelected` and `onSortChange` to reset and reload; modify `loadAssets` to append rather than replace; add `ScrollingModule` import.
- **`gallery.component.html`**: replace `.thumbnail-grid-container + @for` block with `<cdk-virtual-scroll-viewport>`; remove pagination bar; add status line, loading spinner row, and end-of-list indicator.
- **`gallery.component.scss`**: add `cdk-virtual-scroll-viewport` height rule; add `.virtual-row` wrapper styles; add `.gallery-status` bar styles; add spinner/end-of-list row styles; remove or repurpose `.thumbnail-grid-container`.
- **`gallery.component.cy.ts`**: update existing Cypress component tests to remove pagination assertions; add tests for scroll-triggered page load, reset-on-folder-change, reset-on-sort-change, loading spinner, and end-of-list indicator.
- **No backend changes** — `GET /api/assets` with `folderPath`, `page`, and `sort` parameters is used as-is.
- **No model changes** — `PaginatedData<Asset>` and `Asset` interfaces are unchanged.
- **No service changes** — `AssetService.getAssets()` is used as-is.
