# social-media-crop

An interactive canvas-based crop tool in the viewer supports 12 social media format presets with locked aspect ratios. The cropped image is saved as a new asset and immediately available for download.

---

## ADDED Requirements

### Requirement: Crop tool opens with a format selector

The viewer toolbar SHALL include a scissors icon button that opens the `SocialMediaCropComponent` with a format selector `MatSelect` and a canvas showing the image with a crop overlay.

#### Scenario: Crop tool opens for an image asset

- **GIVEN** an image asset is open in the viewer
- **WHEN** the user clicks the scissors icon
- **THEN** the `SocialMediaCropComponent` opens showing the image on a canvas with a crop box overlaid, and a format selector defaulting to `INSTAGRAM_POST`

### Requirement: Crop box is locked to the selected format's aspect ratio

When a format is selected, the crop box SHALL snap to maximum fit and maintain the format's aspect ratio while being dragged or resized.

#### Scenario: Changing format snaps crop box to new ratio

- **GIVEN** the crop tool is open with `INSTAGRAM_POST` (1:1)
- **WHEN** the user selects `TWITTER_HEADER` (3:1)
- **THEN** the crop box snaps to maximum-fit centered on the canvas with a 3:1 aspect ratio

#### Scenario: Corner drag maintains aspect ratio

- **GIVEN** the crop box is at 400×400 (INSTAGRAM_POST ratio)
- **WHEN** the user drags a corner handle
- **THEN** the crop box resizes but always maintains 1:1 ratio

### Requirement: Profile formats display a circle preview

For profile image formats (Instagram Profile, Facebook Profile, LinkedIn Profile, Twitter/X Profile), the crop tool SHALL render a circle outline inside the crop box.

#### Scenario: Circle preview shown for profile formats

- **GIVEN** the user selects `INSTAGRAM_PROFILE`
- **WHEN** the crop overlay renders
- **THEN** a circle outline is drawn inside the square crop box indicating the platform's circular display

### Requirement: Confirming the crop saves a new asset and triggers download

Clicking "Save & Download" SHALL call `POST /api/assets/{id}/crop`, save the result as a new asset at the format's target dimensions, and trigger a browser download of the cropped image.

#### Scenario: Crop is saved and downloaded

- **GIVEN** the user has set the crop box for `INSTAGRAM_POST` (1080×1080)
- **WHEN** the user clicks "Save & Download"
- **THEN** `POST /api/assets/{id}/crop` is called; a new asset is created; the browser downloads `GET /api/assets/{newId}/image`
