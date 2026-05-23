## Why

Assets currently have no user-editable text annotation. Users cannot add context, keywords, or notes to individual photos. A free-text `description` field stored alongside EXIF metadata lets users annotate images without altering the original files, enabling richer search and organisation.

## What Changes

- A Flyway migration adds a `description VARCHAR(2000)` column to the `asset_exif` table
- A new `PATCH /api/assets/{id}/description` endpoint accepts `{ "description": "..." }` and persists the value
- The `ExifPanelComponent` in the frontend gains an editable text area that saves on blur

## Capabilities

### New Capabilities

- `asset-description`: Users can add and edit a free-text description for each asset. The description is saved to the database and displayed in the EXIF panel.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V16__add_asset_description.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/port/in/asset/UpdateAssetDescriptionUseCase.java` — new use case interface
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/UpdateAssetDescriptionUseCaseImpl.java` — use case implementation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — new PATCH endpoint
- `JPPhotoManagerWeb/backend/src/test/` — tests for the use case and endpoint
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/exif-panel/exif-panel.component.ts` — add editable description field
