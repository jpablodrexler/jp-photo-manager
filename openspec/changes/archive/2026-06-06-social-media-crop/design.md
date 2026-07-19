## Context

The viewer component displays the full-size image. In crop mode, the `<img>` is replaced by a `<canvas>` on which the image is drawn and a semi-transparent overlay shows the crop box. The frontend translates display-space crop coordinates to original pixel coordinates using `asset.pixelWidth / asset.pixelHeight`. The backend uses `BufferedImage.getSubimage()` + `Graphics2D.drawImage()` — standard Java2D, no external dependency. The 12 presets are fixed constants in a TypeScript enum and a Java enum.

## Goals / Non-Goals

**Goals:**
- `SocialMediaCropComponent`: canvas element; format selector `MatSelect` with 12 presets; crop box with draggable corners locked to format aspect ratio; circle preview for profile formats
- On format change: snap crop box to maximum fit centered on the canvas
- Mouse event handlers: drag inside box → move; drag corner → resize (maintain ratio)
- On confirm: compute original pixel coordinates (`cropX = canvasCropX * (asset.pixelWidth / canvasWidth)`, same for Y/W/H); call `assetService.cropAsset(assetId, { formatKey, x, y, width, height })`
- `POST /api/assets/{id}/crop` body: `{ formatKey, x, y, width, height }` (original pixel coordinates); returns new `AssetResponse`
- Backend: `BufferedImage.getSubimage(x, y, width, height)` → `Graphics2D.drawImage(subimage, ...)` scaled to format target dimensions → saved as new asset in same folder with `_<formatKey>` suffix; thumbnail generated
- After backend returns, trigger browser download: `window.open('/api/assets/{newId}/image', '_blank')`

**Non-Goals:**
- Free-form (non-preset) crop ratios
- Rotation within the crop tool (handled by image rotation viewer)
- Editing the original in place (always saves as new asset)

## Decisions

### 1. Canvas-based crop box (no external library)

**Decision:** Implement the crop box entirely with `CanvasRenderingContext2D` and mouse event handlers in Angular. No external canvas crop library.

**Rationale:** Avoids an npm dependency. The requirements are fixed-ratio crop with 12 presets — simple enough to implement directly.

### 2. Circle clip path for profile formats

**Decision:** For profile formats, draw a `ctx.arc()` outline inside the crop box. The saved image is still rectangular; the platform applies the circle clip.

**Rationale:** The circle is a visual preview only. Most platforms render profile images as circles by applying CSS or native clipping, not by requiring a circular image file.

### 3. Backend scales to format target dimensions

**Decision:** After `getSubimage()`, scale to the format's target dimensions (e.g. 1080×1080 for `INSTAGRAM_POST`) using `Graphics2D.drawImage(subimage, 0, 0, targetW, targetH, null)`.

**Rationale:** Users expect the output to be the exact format dimensions, not just the correct aspect ratio at whatever crop size they drew.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Canvas event handling complex on touch devices | Low | The feature is primarily desktop-oriented; basic touch support can be added as a follow-up |
| JPEG re-encoding degrades quality on repeated crops | Low | Document that the crop always reads from the original asset file path, not the thumbnail |
