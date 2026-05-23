## Why

Users must currently tag photos manually. Most photos already contain machine-readable metadata (year, camera make, GPS location) that could generate useful tags automatically. Auto-applying tags during cataloging reduces manual work and populates the tag system without any user effort, making photos immediately browsable by year, camera brand, or location.

## What Changes

- During the catalog operation, after EXIF metadata is extracted, an `AutoTaggingService` derives tags from: the year extracted from `dateTaken`, the normalised camera make (lowercase, e.g. `canon`, `sony`, `apple`), and — when `gps-map-view` geocoding is available — a reverse-geocoded city or country name
- Tags are written through the existing `asset_tags` table and tag infrastructure
- Auto-applied tags are indistinguishable from manual tags (users can remove them)
- No new schema is required

## Capabilities

### New Capabilities

- `auto-tagging`: During cataloging, tags are automatically derived from EXIF data (year, camera make) and optionally from reverse geocoding (city/country). Auto-tags are stored alongside manual tags and are removable by the user.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/service/AutoTaggingService.java` — new service
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CatalogAssetsUseCaseImpl.java` — call `AutoTaggingService` after EXIF extraction
- `JPPhotoManagerWeb/backend/src/test/` — tests for tag derivation rules
