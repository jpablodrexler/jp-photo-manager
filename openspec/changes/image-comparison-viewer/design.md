## Context

The `GalleryComponent` maintains `selectedAssets: Set<number>`. When exactly two asset IDs are in the set, a "Compare" toolbar button becomes enabled. The `ComparisonViewerComponent` receives the two `Asset` objects as inputs (or route params) and displays them side by side. Both images are loaded via the existing `GET /api/assets/{id}/image` endpoint (same as the single-asset viewer).

## Goals / Non-Goals

**Goals:**
- "Compare" button in the gallery toolbar: `@if (selectedAssets.size === 2)`
- `ComparisonViewerComponent` receives two `Asset` objects; displays them in a 50/50 split layout
- Each panel: full-height image with `object-fit: contain`; metadata row below (filename, file size via `FileSizePipe`, dimensions from EXIF, star rating if available)
- Zoom synchronization: a single `<mat-slider>` controls both panels' CSS `transform: scale()`
- "Close" button returns the user to the gallery

**Non-Goals:**
- Pixel-diff highlighting between the two images
- Drag-to-compare slider (moving vertical divider — out of scope)
- More than two images at once
- Persisting comparison state across navigation

## Decisions

### 1. `ComparisonViewerComponent` rendered inline in `GalleryComponent` (not a modal)

**Decision:** Show the comparison viewer as a full-page overlay within the gallery feature, not as a `MatDialog`. The gallery toolbar remains visible with only the "Close" button active.

**Rationale:** Full-screen images benefit from all available viewport space. A modal constrains height. The overlay approach is consistent with how the existing single-image viewer works.

### 2. Two-panel CSS Grid layout

**Decision:** Use a CSS Grid with `grid-template-columns: 1fr 1fr` for the two panels. Each panel is a `flex-column` with the image taking `flex: 1` and the metadata row fixed height.

**Rationale:** Grid ensures the panels always share 50% width regardless of screen size. No JavaScript layout calculation needed.

### 3. Zoom via shared CSS `transform: scale()`

**Decision:** A `<mat-slider min="0.5" max="3" step="0.1">` sets a `zoomLevel` property. Both panels apply `[style.transform]="'scale(' + zoomLevel + ')'"` with `transform-origin: top center`.

**Rationale:** CSS transform is GPU-accelerated and does not require reloading the image. `transform-origin: top center` keeps the top of the image aligned, which is natural for tall images.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Images with very different aspect ratios look unbalanced | Low | `object-fit: contain` keeps both images fully visible at any ratio |
| EXIF dimensions not loaded when viewer opens | Low | Load asset EXIF on component init if not already available; show `—` until loaded |
