## Why

Users often receive photo collections as zip archives or store them in tar.gz files. Currently these files are ignored by the catalog. Additionally, the bulk-download endpoint only produces zip files. Two related capabilities share the same archive-reading infrastructure: (1) virtual folders let users browse inside archives without extracting them; (2) a download format selector lets users choose between zip and tar.gz for bulk downloads.

## What Changes

- `org.apache.commons:commons-compress` is added for tar.gz read/write (zip uses `java.util.zip`)
- Zip and tar.gz files appear as expandable virtual folder nodes in the tree using a `!` path separator (e.g. `/photos/album.zip!/summer/`)
- The catalog service extracts images from archives to a temp location, generates thumbnails, and stores assets with the virtual path
- `GET /api/assets/download` gains a `format` query parameter (`zip` / `tar.gz`)
- No Flyway migration required

## Capabilities

### New Capabilities

- `archive-support`: Zip and tar.gz files appear as virtual folders in the gallery. Images inside archives are cataloged using a `!` path separator. Bulk downloads support both zip and tar.gz output formats.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `org.apache.commons:commons-compress`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/ArchiveService.java` — archive reading and writing
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CatalogAssetsUseCaseImpl.java` — detect and expand archives
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java` — extend download endpoint with format parameter
- `JPPhotoManagerWeb/backend/src/test/` — tests for virtual folder cataloging and tar.gz download
- `JPPhotoManagerWeb/frontend/src/app/features/folder-nav/folder-nav.component.ts` — render virtual folder nodes
