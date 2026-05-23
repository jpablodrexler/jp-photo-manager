## Why

Users frequently need to make minor photo adjustments (brightness, contrast, hue) without leaving the application or installing external editing software. Providing a basic non-destructive editor directly in the viewer eliminates the round-trip to an external tool for common corrections.

## What Changes

- The image viewer gains three `MatSlider` controls for brightness, contrast, and hue with CSS `filter` applied to the `<img>` for instant live preview
- Saving dispatches `POST /api/assets/{id}/edit` with the adjustment values
- The backend processes the image using Java2D (`RescaleOp` for brightness/contrast, RGB→HSB→RGB for hue) with no external dependency
- The edited file is saved as a new asset (non-destructive by default); an optional "replace original" flag covers the destructive case

## Capabilities

### New Capabilities

- `asset-image-editor`: The image viewer includes brightness, contrast, and hue sliders for live CSS preview. Saving the edit processes the image server-side with Java2D and stores the result as a new asset (or optionally replaces the original).

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/asset/EditAssetUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/EditAssetUseCaseImpl.java` — Java2D image processing
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — new `POST /api/assets/{id}/edit` endpoint
- `JPPhotoManagerWeb/backend/src/test/` — tests for Java2D processing
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/asset-editor/asset-editor.component.ts` — new standalone component with sliders
