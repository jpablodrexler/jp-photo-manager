## Why

Assets can only be organized by folder, album, and star rating. Folders mirror the filesystem and cannot be changed freely; albums require deliberate curation. There is no lightweight way to label a photo with a concept (e.g., "vacation", "family", "to-print") that cuts across folder boundaries and composes naturally with the existing search and filter system.

## What Changes

- Add a `Tag` entity and an `AssetTag` join table to the database (Flyway migration).
- Expose CRUD tag endpoints: add a tag to an asset, remove a tag from an asset, list all known tags (for autocomplete).
- Extend `GET /api/assets` to accept an optional `tags` filter parameter (comma-separated tag names).
- Display editable tag chips in the gallery EXIF / info panel for the currently viewed or hovered asset.
- Add a **Tags** filter chip/dropdown to the gallery toolbar that narrows results to assets matching all selected tags.
- Support bulk tagging: when one or more assets are selected in the gallery, a "Tag selected" action opens a dialog to add or remove tags across all selected assets at once.
- Tag names are case-insensitive and normalized to lowercase on write.

## Capabilities

### New Capabilities
- `asset-tagging`: Core tag management — creating tags, assigning/removing tags to/from individual or multiple assets, listing all tags for autocomplete, and bulk tagging from the gallery selection.

### Modified Capabilities
- `search-and-filter`: The `GET /api/assets` endpoint gains an optional `tags` filter parameter, and the gallery filter toolbar gains a tag selector. The existing filter requirements are unchanged; this adds a new composable filter dimension.

## Impact

- **Backend:** New `Tag` entity + `AssetTag` join table; Flyway migration; new endpoints `POST /api/assets/{id}/tags`, `DELETE /api/assets/{id}/tags/{tag}`, `GET /api/tags`; `GET /api/assets` updated to accept `tags` query param; `PhotoManagerFacade` gains corresponding methods.
- **Frontend:** `Tag` model in `core/models/`; `TagService` in `core/services/`; tag chip editor in the EXIF panel and gallery info panel; tag filter in the gallery toolbar; bulk-tag dialog component.
- **Database:** One new table (`tags`) and one join table (`asset_tags`); no changes to existing columns.
- **No breaking changes** to any existing endpoint signature or frontend component contract.
