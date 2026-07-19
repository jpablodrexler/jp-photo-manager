## Why

Photos taken in portrait orientation on phones or cameras are stored in landscape orientation in the JPEG file, with the correct orientation encoded in EXIF. The `Asset` entity already stores the `imageRotation` field (e.g. `ROTATE_90`, `ROTATE_270`) extracted from EXIF during cataloging, but the frontend ignores it. As a result, portrait-orientation photos appear sideways in the gallery thumbnail grid and in the full-size viewer.

## What Changes

- Apply a CSS `transform: rotate()` to the `<img>` in `ThumbnailComponent` based on the `imageRotation` field on the `Asset` model
- Apply the same CSS rotation to the full-size image `<img>` in the viewer section of `GalleryComponent`
- No backend change and no new API call required

## Capabilities

### New Capabilities

_(none — this corrects existing behavior using already-stored data)_

### Modified Capabilities

- `gallery-viewer`: Thumbnails and the full-size viewer image are rotated correctly based on the `imageRotation` field already present on the `Asset` model.

## Impact

- `JPPhotoManagerWeb/frontend/src/app/shared/components/thumbnail/thumbnail.component.ts` — add computed rotation style binding
- `JPPhotoManagerWeb/frontend/src/app/shared/components/thumbnail/thumbnail.component.html` — add `[style.transform]` binding
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.html` — add `[style.transform]` binding to the full-size viewer image
