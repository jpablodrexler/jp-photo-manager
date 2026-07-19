## 1. Frontend — Component state and logic

- [x] 1.1 Remove `ThumbnailComponent` from `imports[]` in `GalleryComponent`; add `FileSizePipe` to `imports[]`
- [x] 1.2 Add `trackByAssetId(_index: number, asset: Asset): number` method returning `asset.assetId`
- [x] 1.3 Change `loadNextPage()` to `loadNextPage(continueLoading = false)`: guard with `if (this.isLoading || this.allLoaded || !this.currentFolder) return`; set `isLoading = true`; call `assetService.getAssets()`; on success append with `this.assets = [...this.assets, ...data.items]`; update `totalItems`, increment `pageIndex`; set `allLoaded = (this.pageIndex >= data.totalPages)`; set `isLoading = false`; if `continueLoading && !allLoaded` call `this.loadNextPage(true)` recursively
- [x] 1.4 Update `setupSentinelObserver()` IntersectionObserver callback to call `loadTimelinePage()` only (remove the grid `loadNextPage()` branch)
- [x] 1.5 Update `onFolderSelected()`: reset `assets`, `pageIndex`, `isLoading`, `allLoaded`, `selectedAssets`; in a `Promise.resolve().then()` microtask, if `viewType === 'timeline'` call `setupSentinelObserver()` + `loadTimelinePage()`; otherwise call `loadNextPage(true)`
- [x] 1.6 Update `setViewType()`: same deferred pattern — timeline calls `setupSentinelObserver()` + `loadTimelinePage()`; grid calls `loadNextPage(true)`
- [x] 1.7 Update `loadAssets()` and `onSortChange()`: call `loadNextPage(true)` (grid) or `loadTimelinePage()` (timeline) after reset

## 2. Frontend — Template

- [x] 2.1 Replace the `app-thumbnail` grid section with a `<cdk-virtual-scroll-viewport itemSize="80" class="asset-virtual-viewport">` inside a `.thumbnail-list-container` div
- [x] 2.2 Use `*cdkVirtualFor="let asset of assets; trackBy: trackByAssetId; let i = index"` on the row div
- [x] 2.3 Each row div: `class="asset-list-row"`, `[class.asset-list-row--selected]="isSelected(asset)"`, `(click)="toggleSelection(asset)"`, `(dblclick)="openViewer(i)"`
- [x] 2.4 Row content: `<img [src]="asset.thumbnailUrl" class="list-thumb">`, `.list-meta` with `.list-filename` and `.list-details` (fileSize pipe + date pipe), hover album button
- [x] 2.5 Change sentinel `@if` guard from `viewMode === 'thumbnails'` to `viewMode === 'thumbnails' && viewType === 'timeline'`

## 3. Frontend — Styles

- [x] 3.1 Remove `.thumbnail-grid-container` and `.asset-cell` blocks from `gallery.component.scss`
- [x] 3.2 Add `.thumbnail-list-container { display: flex; flex-direction: column; overflow: hidden; }` and `.asset-virtual-viewport { flex: 1; height: 100%; }`
- [x] 3.3 Add `.asset-list-row` (80 px height, flex row, hover/selected states), `.list-thumb` (106×64 px), `.list-meta` with nested `.list-filename` and `.list-details`, `.list-album-btn` (show on hover)
- [x] 3.4 In `styles.scss`, remove the `.thumbnail-grid` rule and its `@media (max-width: 767px)` override

## 4. Frontend — Tests

- [x] 4.1 Replace `cy.get('app-thumbnail').should('have.length', 2)` assertions with `cy.get('.asset-list-row').should('have.length', 2)` in all affected tests
- [x] 4.2 Update rating tests: replace DOM star-click interactions with direct `component.rateAsset(asset, star)` method calls (no star UI in the list view)
- [x] 4.3 Run `npm test` and confirm all tests pass
