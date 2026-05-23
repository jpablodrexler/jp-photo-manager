## Why

Users frequently need to crop photos to specific aspect ratios for social media profiles, posts, and headers. Currently this requires external tools. A built-in crop tool with 12 preset social media formats eliminates this round-trip and produces ready-to-upload images directly from the gallery.

## What Changes

- A scissors icon button in the viewer toolbar opens a `<canvas>`-based interactive crop overlay
- 12 preset social media formats (Instagram post/portrait/landscape/story/profile, Facebook post/profile, LinkedIn post/profile, Twitter post/profile/header) with locked aspect ratios
- Dragging moves the crop box; corner handles resize it while maintaining the ratio
- `POST /api/assets/{id}/crop` accepts the final coordinates; the backend uses Java2D to extract and scale the region; the result is saved as a new asset
- No Flyway migration required (no new Maven dependency, no new npm package)

## Capabilities

### New Capabilities

- `social-media-crop`: An interactive canvas-based crop tool in the viewer supports 12 social media format presets with locked aspect ratios. The cropped image is saved as a new asset and immediately available for download.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/asset/CropAssetUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CropAssetUseCaseImpl.java` — Java2D crop and scale
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — new PATCH/POST endpoint
- `JPPhotoManagerWeb/backend/src/test/` — tests for crop coordinate calculation
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/social-media-crop/social-media-crop.component.ts` — new canvas-based crop component
