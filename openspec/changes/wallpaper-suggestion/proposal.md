## Why

Users frequently want to set a photo from their collection as a desktop wallpaper but must export or locate the file manually. A wallpaper suggestion endpoint automatically finds a high-resolution image matching the user's screen dimensions and aspect ratio, saving several manual steps.

## What Changes

- Add `aspect_ratio FLOAT` column to `assets` via Flyway migration V15; backfill from existing `pixel_width / pixel_height` data
- Populate `aspect_ratio` in the catalog service when creating or updating an asset
- Add `GetWallpaperSuggestionUseCase` port and `GetWallpaperSuggestionUseCaseImpl` implementation
- Add `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` endpoint
- Add a "Suggest wallpaper" toolbar action in `GalleryComponent` that calls the endpoint using `window.screen.width`/`height` and shows the result in a dialog with a download button

## Capabilities

### New Capabilities

- `wallpaper-suggestion`: A random non-deleted asset matching the user's screen resolution and aspect ratio is returned by a dedicated endpoint; the frontend renders it with a download button.

### Modified Capabilities

- `catalog-assets`: The catalog service populates the new `aspect_ratio` column during asset creation and update.

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V15__add_aspect_ratio.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/entity/AssetEntity.java` — add `aspectRatio` field
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/model/Asset.java` — add `aspectRatio` field
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/persistence/jpa/JpaAssetRepository.java` — add wallpaper suggestion query
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/asset/GetWallpaperSuggestionUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/GetWallpaperSuggestionUseCaseImpl.java` — new use case implementation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — new endpoint
- `JPPhotoManagerWeb/frontend/src/app/core/services/asset.service.ts` — new `getWallpaperSuggestion()` method
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.ts` — new toolbar action and suggestion dialog
