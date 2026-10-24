# Feature 56 — asset-image-editor

**As a** photo library owner, **I want** to adjust brightness, contrast and hue of a photo in the viewer, **so that** I can improve a photo without another application.

## Acceptance Criteria
- AC1: Given the viewer, when I move a slider, then the preview updates instantly without a backend call; the three adjustments combine.
- AC2: Given adjustments, when I save, then the edit is stored as a new asset next to the original.
- AC3: Given replace original is chosen, when I save, then the original file is replaced.
- AC4: Given all adjustments are zero, when I save, then the original asset is returned unchanged.

## Technical notes

Add brightness, contrast, and hue adjustment to the viewer; CSS `filter: brightness() contrast() hue-rotate()` applied to the `<img>` tag drives live preview via three `MatSlider` inputs with zero backend calls; saving dispatches `POST /api/assets/{id}/edit` with the adjustment values; the backend processes the image using Java2D (`java.awt.image`) with no external dependency: `RescaleOp` applies brightness and contrast in a single pass, hue is adjusted by converting RGB→HSB via `Color.RGBtoHSB`, rotating the H component, and converting back; the edited file is saved as a new asset alongside the original (non-destructive by default); an optional "replace original" flag covers the destructive case; no new Maven dependency, no Docker image change

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
