# asset-image-editor

The image viewer includes brightness, contrast, and hue sliders for live CSS preview. Saving the edit processes the image server-side with Java2D and stores the result as a new asset (or optionally replaces the original).

---

## ADDED Requirements

### Requirement: Image viewer includes brightness, contrast, and hue adjustment sliders

The image viewer SHALL display three `MatSlider` controls (brightness, contrast, hue) that apply CSS `filter` to the displayed image for instant live preview without a network call.

#### Scenario: Brightness slider adjusts the preview image instantly

- **GIVEN** the image viewer is open for an asset
- **WHEN** the user moves the brightness slider to +50
- **THEN** the displayed image immediately reflects the CSS `filter: brightness(1.5)` change with no network request

#### Scenario: All three sliders combine in the CSS filter

- **GIVEN** brightness = +30, contrast = -20, hue = +45
- **WHEN** all three sliders have been adjusted
- **THEN** the CSS `filter` on the `<img>` element is `brightness(1.3) contrast(0.8) hue-rotate(45deg)`

### Requirement: Saving the edit produces a new asset via Java2D processing

`POST /api/assets/{id}/edit` SHALL apply brightness, contrast, and hue adjustments to the original image file using Java2D and save the result as a new asset in the same folder.

#### Scenario: Edit saved as a new asset

- **GIVEN** an asset `photo.jpg` in `/photos/vacation`
- **WHEN** `POST /api/assets/42/edit` is called with `{ "brightness": 30, "contrast": -20, "hue": 45 }`
- **THEN** the response is `200 OK` with the new asset's `AssetResponse`; a file `photo_edited.jpg` is created in `/photos/vacation` and cataloged as a new asset

#### Scenario: Edit replaces the original when replaceOriginal is true

- **GIVEN** an asset `photo.jpg`
- **WHEN** `POST /api/assets/42/edit` is called with `{ "brightness": 30, "replaceOriginal": true }`
- **THEN** the original `photo.jpg` is overwritten and the same asset record is updated; no new asset is created

### Requirement: Zero-adjustment edit is a no-op

When all adjustment values are zero, `POST /api/assets/{id}/edit` SHALL return the original asset unchanged (no file copy, no new asset).

#### Scenario: Zero adjustments return the original asset

- **GIVEN** an asset with `id = 42`
- **WHEN** `POST /api/assets/42/edit` is called with `{ "brightness": 0, "contrast": 0, "hue": 0 }`
- **THEN** the response is `200 OK` with the original asset's `AssetResponse` and no new file is created
