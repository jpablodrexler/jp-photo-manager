## 1. Frontend — Component state and logic

- [x] 1.1 Remove `pageIndex`, `totalPages`, `prevPage()`, and `nextPage()` from `GalleryComponent`; add `totalItems = 0`, `isLoading = false`, `allLoaded = false` properties
- [x] 1.2 Add `@ViewChild('scrollSentinel') private sentinel!: ElementRef<HTMLDivElement>` and a private `observer: IntersectionObserver | null = null` field
- [x] 1.3 Add `loadNextPage()` method: guard with `if (this.isLoading || this.allLoaded || !this.currentFolder) return;`; set `isLoading = true`; call `assetService.getAssets(this.currentFolder, this.pageIndex, this.sortCriteria)`; on success append items with `this.assets = [...this.assets, ...data.items]`; update `totalItems`, increment `pageIndex`; set `allLoaded = (this.pageIndex >= data.totalPages)`; set `isLoading = false`
- [x] 1.4 Add `setupSentinelObserver()` method: create `new IntersectionObserver((entries) => { if (entries[0].isIntersecting) this.loadNextPage(); }, { threshold: 0.1 })`; observe `this.sentinel.nativeElement`
- [x] 1.5 Add `disconnectObserver()` method: call `this.observer?.disconnect(); this.observer = null`
- [x] 1.6 Update `onFolderSelected(folderPath)`: set `this.currentFolder = folderPath`; reset `assets = []`, `pageIndex = 0`, `isLoading = false`, `allLoaded = false`, `selectedAssets.clear()`; call `disconnectObserver()`; then after a microtask (`Promise.resolve().then(...)`) call `setupSentinelObserver()` and `loadNextPage()` (deferred so the sentinel `@ViewChild` is in the DOM)
- [x] 1.7 Update `onSortChange()`: reset `assets = []`, `pageIndex = 0`, `isLoading = false`, `allLoaded = false`; call `disconnectObserver()`; deferred: `setupSentinelObserver()` and `loadNextPage()`
- [x] 1.8 Implement `ngOnDestroy()`: call `disconnectObserver()` to prevent memory leaks
- [x] 1.9 Add `ScrollingModule` from `@angular/cdk/scrolling` and `MatProgressBarModule` from `@angular/material/progress-bar` to the `imports` array of `GalleryComponent`

## 2. Frontend — Template

- [x] 2.1 Remove the entire `<!-- Pagination -->` `@if (totalPages > 1)` block (lines 64–74 in current HTML)
- [x] 2.2 Inside the `.thumbnail-grid-container` div, after the `.thumbnail-grid` closing tag, add:
  ```html
  @if (isLoading) {
    <div class="loading-row">
      <mat-progress-bar mode="indeterminate" />
    </div>
  }
  @if (allLoaded && assets.length > 0) {
    <div class="end-of-list">All photos loaded</div>
  }
  <div #scrollSentinel class="scroll-sentinel"></div>
  ```
- [x] 2.3 Update the status bar to show total count: replace the `{{ currentFolder }}` span with `{{ assets.length }} of {{ totalItems }} photos` when a folder is selected and `totalItems > 0`; retain the "X selected" and `statusMessage` spans

## 3. Frontend — Styles

- [x] 3.1 In `gallery.component.scss`, add `.scroll-sentinel { height: 1px; }` — invisible trigger element
- [x] 3.2 Add `.loading-row { padding: 16px; display: flex; justify-content: center; } mat-progress-bar { width: 200px; }` for the loading indicator
- [x] 3.3 Add `.end-of-list { text-align: center; padding: 16px; font-size: 12px; color: rgba(255,255,255,0.4); }` for the end-of-list label
- [x] 3.4 In `styles.scss`, remove the `.pagination-bar` rule block (lines 115–121) — no longer used anywhere

## 4. Frontend — Tests

- [x] 4.1 In `gallery.component.cy.ts`, remove any existing assertions about `.pagination-bar`, prev/next button disabled states, or `prevPage`/`nextPage` method calls
- [x] 4.2 Add test: given `assetService.getAssets` returns page 0 with `totalPages: 3`, assert `.pagination-bar` does not exist in the DOM
- [x] 4.3 Add test: given `getAssets` returns page 0 (`totalPages: 2`), simulate sentinel intersection (call `loadNextPage()` directly or use Cypress intercepts); assert `getAssets` is called a second time with `page: 1`; assert the assets array grows to the combined count
- [x] 4.4 Add test: changing folder resets `assets` to empty before loading; assert `getAssets` is called with `page: 0` and the previous items are gone
- [x] 4.5 Add test: changing sort criteria resets `assets` and calls `getAssets` with `page: 0`
- [x] 4.6 Add test: while `isLoading` is true, `mat-progress-bar` is visible in the DOM
- [x] 4.7 Add test: when `allLoaded` is true, `.end-of-list` element is visible
- [x] 4.8 Add test: when `allLoaded` is true and `loadNextPage()` is called again, `getAssets` is NOT called a second time (guard check)
- [x] 4.9 Run `npm test` and confirm all tests pass
