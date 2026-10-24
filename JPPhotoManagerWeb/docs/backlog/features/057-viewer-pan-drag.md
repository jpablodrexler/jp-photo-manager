# Feature 57 — viewer-pan-drag

**As a** photo library owner, **I want** to pan a zoomed image by dragging, **so that** I inspect details of a large photo without scrolling controls.

## Acceptance Criteria
- AC1: Given a zoomed image, when I drag with the mouse, then the image pans and the cursor shows grabbing.
- AC2: Given I drag, when the mouse leaves the container, then the drag ends.
- AC3: Given a touch device, when I drag with one finger, then the image pans.
- AC4: Given the zoom returns to 1× or the displayed asset changes, when it happens, then the pan resets.

## Technical notes

Add mouse and touch drag-to-pan to the zoomed image viewer; the existing `transform: scale(viewerZoom)` is extended to `scale(zoom) translate(panX, panY)`; `mousedown` sets a dragging flag and captures the starting cursor position, `mousemove` updates `panX`/`panY` while dragging, `mouseup` clears the flag; `touchstart`/`touchmove`/`touchend` mirror the same logic for mobile; `panX` and `panY` reset to zero whenever zoom returns to 1× or the displayed asset changes; pure frontend change — no backend endpoint, no schema change, no external dependency

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
