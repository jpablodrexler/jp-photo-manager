# Feature 62 — social-media-crop

**As a** photo library owner, **I want** to crop a photo to a social-media format with a locked aspect ratio, **so that** the picture fits Instagram, Facebook, LinkedIn or Twitter without distortion.

## Acceptance Criteria
- AC1: Given an image asset in the viewer, when I press the scissors button, then the crop tool opens; it is not available for non-images.
- AC2: Given the 12 format presets, when I choose one, then the crop box snaps to its aspect ratio.
- AC3: Given the crop box, when I drag inside it, then it moves; when I drag a corner, then it resizes keeping the ratio.
- AC4: Given a profile format, when the crop box is shown, then a circle outline previews the platform's circular display.
- AC5: Given I confirm, when the crop is applied, then the backend crops and scales the image into a new asset with a new thumbnail and the browser downloads the result.

## Technical notes

Canvas-based interactive crop tool for 12 social media format presets; a scissors icon button added to the viewer toolbar toggles `showCropPanel` (same pattern as `showExifPanel`); the `<img>` is replaced by a `<canvas>` in crop mode — the image is drawn on the canvas and a semi-transparent overlay renders the draggable crop box with corner handles; the crop box aspect ratio is always locked to the selected format; dragging inside the box moves it, dragging a corner handle resizes it while keeping the ratio; for profile-image formats a `ctx.arc()` circle outline is drawn inside the crop box to preview the platform's circular display; the 12 presets are: `INSTAGRAM_POST` 1080×1080 (1:1), `INSTAGRAM_PORTRAIT` 1080×1350 (4:5), `INSTAGRAM_LANDSCAPE` 1080×566 (~1.91:1), `INSTAGRAM_STORY` 1080×1920 (9:16), `INSTAGRAM_PROFILE` 110×110 (1:1 circle), `FACEBOOK_POST` 1200×630 (~1.91:1), `FACEBOOK_PROFILE` 170×170 (1:1 circle), `LINKEDIN_POST` 1200×627 (~1.91:1), `LINKEDIN_PROFILE` 400×400 (1:1 circle), `TWITTER_POST` 1600×900 (16:9), `TWITTER_PROFILE` 400×400 (1:1 circle), `TWITTER_HEADER` 1500×500 (3:1); on confirm the frontend translates canvas-display coordinates to original image pixel coordinates and sends `POST /api/assets/{id}/crop`; the backend uses Java2D to crop, scale, and save the result as a new `Asset` with a freshly generated thumbnail; the frontend immediately triggers a browser download of the cropped image

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
