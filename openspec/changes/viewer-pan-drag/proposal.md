## Why

The image viewer supports zooming in, but when zoomed in the image is clipped to the viewport with no way to pan to a different area. Users cannot examine a specific region of a zoomed photo without zooming back out and re-zooming. Drag-to-pan makes the zoomed viewer fully functional on both desktop (mouse) and mobile (touch).

## What Changes

- The existing `transform: scale(viewerZoom)` is extended to `scale(zoom) translate(panX, panY)`
- `mousedown` / `mousemove` / `mouseup` event handlers implement drag-to-pan on desktop
- `touchstart` / `touchmove` / `touchend` mirror the same logic for mobile
- `panX` and `panY` reset to zero when zoom returns to 1× or the displayed asset changes
- Pure frontend change — no backend endpoint, no schema change, no external dependency

## Capabilities

### New Capabilities

_(none — extends existing viewer capability)_

### Modified Capabilities

- **`image-viewer`** (gallery feature): The zoomed image viewer supports drag-to-pan using mouse and touch events. Pan resets automatically when zoom returns to 1× or the asset changes.

## Impact

- `JPPhotoManagerWeb/frontend/src/app/features/gallery/` — update viewer component with pan state and event handlers (mouse and touch)
