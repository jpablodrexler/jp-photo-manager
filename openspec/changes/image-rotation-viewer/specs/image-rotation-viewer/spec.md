# image-rotation-viewer

Thumbnails and the full-size viewer image are rotated based on the `imageRotation` field already stored on the `Asset` model, using CSS `transform: rotate()`. No backend change or additional API call is required.

---

## ADDED Requirements

### Requirement: Thumbnails display with correct orientation

`ThumbnailComponent` SHALL apply a CSS `transform: rotate(Ndeg)` to the `<img>` element based on the `asset.imageRotation` field.

#### Scenario: Portrait photo displayed upright in thumbnail grid

- **GIVEN** an asset with `imageRotation: "ROTATE_90"` is shown in the gallery grid
- **WHEN** `ThumbnailComponent` renders
- **THEN** the thumbnail `<img>` has `style.transform = "rotate(90deg)"` and the image appears upright

#### Scenario: Non-rotated photo has no rotation style

- **GIVEN** an asset with `imageRotation: "ROTATE_0"` or `imageRotation: null`
- **WHEN** `ThumbnailComponent` renders
- **THEN** the thumbnail `<img>` has `transform: rotate(0deg)` (no visible rotation)

### Requirement: Full-size viewer image displays with correct orientation

The full-size `<img>` in the gallery viewer SHALL apply the same `transform: rotate(Ndeg)` based on the currently viewed `Asset.imageRotation`.

#### Scenario: Full-size portrait image displayed upright in viewer

- **GIVEN** an asset with `imageRotation: "ROTATE_270"` is opened in the viewer
- **WHEN** the full-size image `<img>` renders
- **THEN** it has `style.transform = "rotate(270deg)"` and the image appears in the correct orientation

### Requirement: All four rotation values are supported

The rotation mapping SHALL cover `ROTATE_0` → 0°, `ROTATE_90` → 90°, `ROTATE_180` → 180°, `ROTATE_270` → 270°.

#### Scenario: All four rotation values map to correct degrees

- **GIVEN** assets with each of the four `imageRotation` values
- **WHEN** rendered in `ThumbnailComponent`
- **THEN** each `<img>` has `transform: rotate(N deg)` with the corresponding degree value
