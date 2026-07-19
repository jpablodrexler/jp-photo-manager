## Why

The convert feature currently only converts PNG images to JPEG. Modern image formats WebP and AVIF offer significantly better compression at equivalent quality. Adding them as conversion targets allows users to optimize storage and web delivery bandwidth for their photo archives.

## What Changes

- `ConvertAssetsUseCase` is extended to support JPEG/PNG → WebP and JPEG/PNG → AVIF in addition to the existing PNG → JPEG
- WebP encoding uses `cwebp` via `ProcessBuilder`; AVIF encoding uses `avifenc` via `ProcessBuilder`
- The `convert_assets_directories_definitions` table gains a `target_format VARCHAR(10)` column (Flyway migration)
- The frontend `ConvertComponent` adds a "Target format" dropdown (JPEG / WebP / AVIF) to the directory pair configuration form

## Capabilities

### New Capabilities

_(none — extends existing `convert-assets` capability)_

### Modified Capabilities

- **`convert-assets`**: Now supports conversion to JPEG, WebP, and AVIF formats. The target format is configurable per directory pair. `cwebp` and `avifenc` must be installed on the host (or available in the Docker image).

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V23__add_target_format_to_convert_definitions.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/enums/ImageFormat.java` — new enum (`JPEG`, `WEBP`, `AVIF`)
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/ConvertAssetsServiceImpl.java` — branch on `targetFormat`
- `JPPhotoManagerWeb/backend/src/test/` — tests for WebP and AVIF conversion paths
- `JPPhotoManagerWeb/frontend/src/app/features/convert/convert.component.ts` — add target format dropdown to directory pair form
- `JPPhotoManagerWeb/docker-compose.yml` — ensure `cwebp` and `avifenc` are available in the backend container (add to `Dockerfile`)
