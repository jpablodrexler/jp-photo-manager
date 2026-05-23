## Context

The backend stores the EXIF `Orientation` tag as a `ImageRotation` enum on the `Asset` entity. The TypeScript `Asset` model has an `imageRotation: string` field (e.g. `"ROTATE_0"`, `"ROTATE_90"`, `"ROTATE_180"`, `"ROTATE_270"`). The frontend never reads this field. CSS `transform: rotate(Ndeg)` applied to an `<img>` tag is the standard browser-native way to display EXIF-rotated images without modifying the file.

## Goals / Non-Goals

**Goals:**
- Map `imageRotation` enum values to CSS `rotate()` degrees in both the thumbnail and the viewer
- Apply the rotation via a style binding in the component template

**Non-Goals:**
- Rotating the actual JPEG file on disk (that would require backend processing and is a separate feature)
- Applying rotation to the thumbnail binary file (the `.bin` thumbnail on disk remains landscape; CSS fixes the display)
- Handling the `FLIP_*` mirror orientations (rare; defer to future enhancement)

## Decisions

### 1. CSS `transform: rotate()` — no backend change

**Decision:** Apply the rotation entirely in CSS using `[style.transform]` bindings. No new API call, no backend change.

**Rationale:** The `imageRotation` value is already on the `Asset` model returned by the existing gallery API. CSS rotation is the simplest possible fix with zero performance overhead.

**Alternative considered:** Serving the thumbnail already rotated (processing during cataloging). Rejected because it requires regenerating all existing thumbnails and increases cataloging time. The CSS approach is instantaneous and non-destructive.

### 2. Rotation mapping function in component (not a pipe)

**Decision:** Add a `getRotationStyle(imageRotation: string): string` method to both `ThumbnailComponent` and `GalleryComponent` returning `rotate(Ndeg)` strings.

**Rationale:** A method is simpler than a new pipe for a single-use transformation with four possible inputs. A shared pipe would be justified if more components needed it.

**Alternative considered:** A shared `ImageRotationPipe`. Acceptable but unnecessary for four fixed inputs; a method is sufficient.

### 3. `ROTATE_0` and null/undefined map to `rotate(0deg)` (identity)

**Decision:** `"ROTATE_0"`, `null`, and `undefined` all map to `rotate(0deg)` (no visual change).

**Rationale:** Most photos are not rotated. Using `rotate(0deg)` as the default prevents any conditional null check in the template.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| CSS rotation does not account for image dimensions (a portrait image rotated 90° overflows its container) | Medium | Add `max-width: 100%; max-height: 100%; object-fit: contain` to the `<img>` to prevent overflow; the thumbnail grid may need height adjustment |
| Thumbnails generated as landscape (200×150) look wrong when displayed rotated | Low | The `.bin` thumbnail file orientation is a known limitation; the viewer uses the full-size image which benefits fully from rotation |
