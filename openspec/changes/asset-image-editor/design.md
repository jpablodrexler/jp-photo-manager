## Context

The existing image viewer renders the full-size image via `GET /api/assets/{id}/image`. Java2D (`java.awt.image`, `java.awt.image.RescaleOp`, `java.awt.Color`) is part of the Java standard library and available in all JRE distributions. No additional Maven dependency or Docker image change is required.

## Goals / Non-Goals

**Goals:**
- Frontend: three `MatSlider` controls (brightness: -100 to +100, contrast: -100 to +100, hue: -180 to +180); sliders drive CSS `filter: brightness() contrast() hue-rotate()` for zero-latency live preview
- `POST /api/assets/{id}/edit` accepts `{ brightness, contrast, hue, replaceOriginal?: boolean }` and returns the new asset's `AssetResponse`
- Backend processing:
  1. Brightness + contrast: `RescaleOp(scaleFactor, offset, null).filter(src, dst)` — `scaleFactor = 1 + contrast/100`, `offset = brightness * 2.55`
  2. Hue: convert each pixel RGB→HSB, rotate H by `hue/360`, convert back
- Edited image is saved to the same folder as the original with `_edited` suffix; a new catalog entry is created
- `replaceOriginal = true` overwrites the original file and updates the existing asset record instead

**Non-Goals:**
- Crop, rotate, or resize (separate concerns)
- RAW format editing (Java2D handles JPEG and PNG only)
- Undo history beyond reset-to-zero sliders in the frontend

## Decisions

### 1. CSS filter for live preview, Java2D for server-side save

**Decision:** The frontend sliders apply CSS `filter` instantly to the displayed `<img>` element. Saving sends the numeric values to the backend, which applies equivalent Java2D operations to the original image data.

**Rationale:** CSS filter gives sub-frame preview latency with zero network calls. Java2D ensures the saved file matches the preview without JPEG re-encoding artifacts from stacking CSS filters.

### 2. Non-destructive by default (new asset)

**Decision:** The default behavior saves the edited image as a new file (`photo_edited.jpg`) and creates a new asset record. `replaceOriginal = true` is the opt-in destructive path.

**Rationale:** Non-destructive editing is the industry default for photo management tools. The original is always recoverable unless the user explicitly chose to replace it.

### 3. Single-pass Java2D: `RescaleOp` for brightness and contrast

**Decision:** Apply brightness and contrast in a single `RescaleOp` pass: `scaleFactor = 1 + contrast/100`, `offset = brightness * 2.55`. Hue is applied in a second pass (pixel-by-pixel HSB rotation).

**Rationale:** `RescaleOp` is GPU-accelerable by Java2D on some platforms and is the standard way to apply linear image transforms. Hue rotation requires HSB space and cannot be done with `RescaleOp`.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| JPEG re-encoding degrades image quality | Low | Apply edits to the original unmodified source image, not the thumbnail |
| Pixel-by-pixel hue rotation is slow for large images | Medium | Apply asynchronously (`@Async`); SSE not needed since result is returned in the response body after completion |
