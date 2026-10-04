# Feature 30 — image-rotation-viewer

Apply the `imageRotation` field already stored on `Asset` as a CSS `transform: rotate()` in `ThumbnailComponent` and the full-size viewer `<img>`; photos taken in portrait orientation are currently displayed sideways because the raw file is served without orientation correction; no backend change and no new API call are required
