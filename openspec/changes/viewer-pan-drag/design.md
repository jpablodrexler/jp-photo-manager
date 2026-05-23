## Context

The viewer component applies `transform: scale(viewerZoom)` to the `<img>` element. Panning is implemented by adding `translate(panX px, panY px)` to the same transform. The combined transform string is `scale(zoom) translate(panX, panY)` with `transform-origin: center center`.

## Goals / Non-Goals

**Goals:**
- Component-level state: `panX = 0`, `panY = 0`, `isDragging = false`, `dragStartX = 0`, `dragStartY = 0`
- Mouse events bound to the viewer container (not the `<img>`) to avoid browser default drag behavior:
  - `(mousedown)` → set `isDragging = true`, record `dragStartX/Y`
  - `(mousemove)` → if `isDragging`, update `panX += event.movementX / viewerZoom`, `panY += event.movementY / viewerZoom`
  - `(mouseup)` / `(mouseleave)` → set `isDragging = false`
- Touch events:
  - `(touchstart)` → record `dragStartX/Y` from `touches[0]`
  - `(touchmove)` → compute delta from last touch position, update `panX`/`panY`
  - `(touchend)` → no action
- Reset `panX = panY = 0` when `viewerZoom` returns to 1 or the current asset changes
- CSS: `user-select: none; cursor: grab` on the viewer container; `cursor: grabbing` when `isDragging`
- Bind the combined transform: `[style.transform]="'scale(' + viewerZoom + ') translate(' + panX + 'px, ' + panY + 'px)'"`

**Non-Goals:**
- Pinch-to-zoom on mobile (the existing zoom slider handles zoom separately)
- Momentum/inertia scrolling after drag release
- Bounds clamping (panning past the image edge is allowed; the image clips to the container)

## Decisions

### 1. `event.movementX/Y` for mouse delta

**Decision:** Use `event.movementX` and `event.movementY` (relative pointer movement since last event) instead of computing delta from `clientX/Y`.

**Rationale:** `movementX/Y` is available in all modern browsers and avoids storing the previous position for mouse events. Touch events require explicit delta computation since `Touch` objects have no `movement*` property.

### 2. Divide mouse delta by zoom level

**Decision:** `panX += event.movementX / viewerZoom`.

**Rationale:** When zoomed in, the image is scaled up, so a pixel of cursor movement corresponds to a smaller pan offset in image coordinates. Dividing by zoom makes the drag feel 1:1 with the image content regardless of zoom level.

### 3. No bounds clamping

**Decision:** `panX` and `panY` are unclamped — users can drag the image fully out of the viewport.

**Rationale:** Clamping requires knowing the image's rendered dimensions relative to the container, which adds complexity. The simple reset-on-zoom behavior recovers from any out-of-bounds state.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Browser default drag behavior (ghost image on drag) | Medium | Call `event.preventDefault()` in `mousedown` and add `draggable="false"` to the `<img>` |
| Touch scrolling conflicts with pan on mobile | Low | Call `event.preventDefault()` in `touchmove` when `isDragging` |
