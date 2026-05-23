## Why

When deduplicating photos or comparing different versions of the same shot, users currently have to open images one at a time. A side-by-side comparison view for exactly two selected assets lets users immediately spot differences without switching between tabs or dialogs.

## What Changes

- When exactly two assets are selected in the gallery, a "Compare" action appears in the toolbar
- Clicking "Compare" opens a new `ComparisonViewerComponent` that renders both images side by side
- Each panel shows the image with matched zoom levels, plus filename, file size, dimensions, and rating beneath
- No new backend endpoint — both images are served by the existing `GET /api/assets/{id}/image`

## Capabilities

### New Capabilities

- `image-comparison-viewer`: Selecting exactly two assets in the gallery enables a "Compare" toolbar action that opens a split-screen `ComparisonViewerComponent` showing both images side by side with metadata beneath each panel.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/frontend/src/app/features/gallery/comparison-viewer/comparison-viewer.component.ts` — new standalone component
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.ts` — add "Compare" toolbar button visible when `selectedAssets.size === 2`
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/comparison-viewer/comparison-viewer.component.scss` — split-screen layout styles
