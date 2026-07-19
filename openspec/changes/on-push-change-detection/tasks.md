## 1. Add OnPush to all components

- [ ] 1.1 Add `changeDetection: ChangeDetectionStrategy.OnPush` to `AppComponent`
- [ ] 1.2 Add `OnPush` to `GalleryComponent`
- [ ] 1.3 Add `OnPush` to `ThumbnailComponent`
- [ ] 1.4 Add `OnPush` to `FolderNavComponent`
- [ ] 1.5 Add `OnPush` to `SyncComponent`
- [ ] 1.6 Add `OnPush` to `ConvertComponent`
- [ ] 1.7 Add `OnPush` to `DuplicatesComponent`
- [ ] 1.8 Add `OnPush` to `RecycleBinComponent`
- [ ] 1.9 Add `OnPush` to `AlbumsComponent`
- [ ] 1.10 Add `OnPush` to `AlbumDetailComponent`
- [ ] 1.11 Add `OnPush` to `ExifPanelComponent`
- [ ] 1.12 Add `OnPush` to `SlideshowComponent`
- [ ] 1.13 Add `OnPush` to `TimelineViewComponent`
- [ ] 1.14 Add `OnPush` to all dialog components and any remaining feature components

## 2. GalleryComponent — immutable state

- [ ] 2.1 Replace any `this.assets.push(...)` or index mutations with `this.assets = [...data.items]`
- [ ] 2.2 Replace `this.selectedAssets.add(id)` with `this.selectedAssets = new Set([...this.selectedAssets, id])`
- [ ] 2.3 Replace `this.selectedAssets.delete(id)` with `this.selectedAssets = new Set([...this.selectedAssets].filter(x => x !== id))`
- [ ] 2.4 Replace `this.selectedAssets.clear()` with `this.selectedAssets = new Set()`
- [ ] 2.5 Inject `ChangeDetectorRef` and add `cdr.markForCheck()` in all `EventSource.addEventListener` callbacks

## 3. SyncComponent, ConvertComponent, DuplicatesComponent — SSE callbacks

- [ ] 3.1 In `SyncComponent`, inject `ChangeDetectorRef` and call `cdr.markForCheck()` after every SSE-driven state update
- [ ] 3.2 In `ConvertComponent`, inject `ChangeDetectorRef` and call `cdr.markForCheck()` after SSE state updates
- [ ] 3.3 In `DuplicatesComponent`, inject `ChangeDetectorRef` and call `cdr.markForCheck()` after any SSE or async updates

## 4. Review all manual subscriptions

- [ ] 4.1 Search all `.subscribe()` calls in component files; add `cdr.markForCheck()` in each `next` callback where the updated property is used in the template
- [ ] 4.2 For components already using `async` pipe, no change needed — `async` pipe calls `markForCheck()` automatically

## 5. Testing and Commit

- [ ] 5.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 5.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 5.3 Commit all changes (only after both test suites pass)
