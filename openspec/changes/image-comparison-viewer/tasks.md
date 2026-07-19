## 1. GalleryComponent — Compare button

- [ ] 1.1 Add a "Compare" `MatButton` to the gallery toolbar that is shown `@if (selectedAssets.size === 2)`
- [ ] 1.2 Bind `(click)` to `openComparisonViewer()` which sets `showComparisonViewer = true` and stores the two selected `Asset` objects

## 2. ComparisonViewerComponent

- [ ] 2.1 Create `features/gallery/comparison-viewer/comparison-viewer.component.ts` as a standalone component
- [ ] 2.2 Declare `@Input() assetA!: Asset` and `@Input() assetB!: Asset`
- [ ] 2.3 Load EXIF for each asset on `ngOnInit` if `dimensions` are not already in the `Asset` model; call `assetService.getAssetExif(assetId)`
- [ ] 2.4 Implement `zoomLevel = 1` property; bind to a `<mat-slider min="0.5" max="3" step="0.1">`
- [ ] 2.5 Template: two-column CSS Grid; each column has `<img>` with `[src]` bound to `/api/assets/{id}/image`, `[style.transform]="'scale(' + zoomLevel + ')'"`, and a metadata row below
- [ ] 2.6 Add "Close" `MatButton` that emits `(closed)` output event; `GalleryComponent` handles it by setting `showComparisonViewer = false`

## 3. ComparisonViewerComponent styles

- [ ] 3.1 Create `comparison-viewer.component.scss`:
  - `.comparison-layout` — `display: grid; grid-template-columns: 1fr 1fr; height: 100%; gap: 8px`
  - `.panel` — `display: flex; flex-direction: column; overflow: hidden`
  - `.panel img` — `flex: 1; object-fit: contain; width: 100%; transition: transform 0.2s`
  - `.metadata-row` — `padding: 8px; font-size: 12px; display: flex; gap: 16px`

## 4. GalleryComponent integration

- [ ] 4.1 Import `ComparisonViewerComponent` in `GalleryComponent`
- [ ] 4.2 Add `showComparisonViewer = false` and `comparisonAssets: [Asset, Asset] | null = null` properties
- [ ] 4.3 Render `<app-comparison-viewer @if (showComparisonViewer) [assetA]="comparisonAssets![0]" [assetB]="comparisonAssets![1]" (closed)="showComparisonViewer = false">` as a full-page overlay above the gallery grid

## 5. Frontend tests

- [ ] 5.1 Add a Cypress component test for `ComparisonViewerComponent` verifying both images render and the zoom slider exists
- [ ] 5.2 Add a Cypress component test for `GalleryComponent` verifying the Compare button is visible only when exactly 2 assets are selected

## 6. Testing and Commit

- [ ] 6.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 6.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 6.3 Commit all changes (only after both test suites pass)
