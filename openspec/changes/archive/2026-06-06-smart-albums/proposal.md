## Why

Virtual albums (improvement 8) let users curate static, hand-picked photo sets. They work well for fixed collections such as "Wedding photos" or "Client delivery", but they require constant manual curation: every new photo that matches a user's mental criteria must be added one by one. Photographers frequently think in terms of ongoing categories — "all 5-star shots from 2024", "everything tagged 'holiday' captured after January 2023", "recently added files named 'RAW_'"— that are dynamic by nature. Without smart albums, a user who wants to track such a category must either maintain a static album manually (tedious) or apply the same filter every session from the gallery toolbar (repetitive). Smart albums solve this by storing the filter criteria alongside the album record. When the album is opened, its contents are computed on the fly by running the existing `findFiltered` query logic across all catalogued folders, so the album always reflects the current state of the catalog without any user effort.

## What Changes

- Add a Flyway migration `V14__add_smart_albums.sql` that adds a `filter_json JSONB` nullable column to the `albums` table and creates a partial index `ix_albums_filter_json_not_null` for efficient listing of smart albums.
- Extend the `AlbumEntity`, `Album` domain model, `AlbumData` application DTO, `AlbumSummaryDto`, `AlbumDto`, `CreateAlbumRequest`, and `UpdateAlbumRequest` HTTP DTOs with an optional `filterJson` field carrying the same filter shape already used by `FilterPreset` (`search`, `dateFrom`, `dateTo`, `minRating`).
- Extend `AlbumRepository` (port) and `AlbumRepositoryImpl` (adapter) with a new `findSmartAlbumAssets(Long albumId, AssetFilter filter, int page, int pageSize)` method that delegates to `AssetRepository.findFiltered` with `folderId = null` (all folders).
- Extend `GetAlbumUseCaseImpl.executeAssets` to branch on whether the album's `filterJson` is non-null: if it is, reconstruct an `AssetFilter` from the stored JSON and call `assetRepository.findFiltered`; otherwise use the existing `albumRepository.findAssetsByAlbumId` path against the static join table.
- Extend `AlbumRepositoryImpl.countAssets` with a parallel branch that counts via `AssetRepository.findFiltered` when the album is a smart album.
- Extend `CreateAlbumUseCaseImpl` and `UpdateAlbumUseCaseImpl` to accept and persist the optional `filterJson` field; validation ensures that when `filterJson` is present at least one filter criterion is non-null.
- Add `GetSmartAlbumAssetsUseCase` and its implementation as a thin use case that wraps the filter-based query so the controller can call it independently.
- Update the `AlbumController` `GET /api/albums/{id}` endpoint to pass `filterJson` from the album to the appropriate use case.
- Add an Angular `AlbumFilterJson` TypeScript interface to `album.model.ts` and extend `AlbumSummary` and `Album` with an optional `filterJson` field.
- Extend `AlbumService` (Angular) with no additional HTTP methods — existing `createAlbum`, `updateAlbum`, and `getAlbum` already carry `filterJson` through.
- Extend `AlbumsComponent` to show a "smart" badge on albums that have `filterJson` set.
- Extend `AlbumDetailComponent` to show "Smart album — populated dynamically" hint text when the album is a smart album; hide the "Remove from album" button per asset (assets cannot be manually removed from a smart album).
- Extend the create-album form in `AlbumsComponent` to include an optional "Make smart" toggle that reveals the same filter fields used in the gallery filter toolbar (`search`, `dateFrom`, `dateTo`, `minRating`), wired to `filterJson` in the create request.
- Extend `UpdateAlbumRequest` and the edit flow in `AlbumDetailComponent` to allow toggling the smart/static mode and updating the stored filter.

## Capabilities

### New Capabilities

- `smart-albums`: Albums can optionally store a `filter_json` field containing filter criteria (`search`, `dateFrom`, `dateTo`, `minRating`). When `filter_json` is non-null the album is a "smart album": its assets are computed at query time by running the existing filter logic across all catalogued folders. The album page shows a visual indicator and hides the manual add/remove controls. Smart albums are always up to date with the catalog without any user action.

### Modified Capabilities

- **Albums — create and update**: `CreateAlbumRequest` and `UpdateAlbumRequest` gain an optional `filterJson` object. Setting it converts the album to smart mode; sending `null` or omitting it keeps/reverts the album to static mode.
- **Albums — browse contents**: `GET /api/albums/{id}` branches on whether `filter_json` is set; if it is, the paginated assets are sourced from the filter query rather than the `album_assets` join table.
- **Albums list**: `AlbumSummaryDto` and the Angular `AlbumSummary` model expose `filterJson` so the frontend can badge smart albums in the list view.
- **Album detail UI**: `AlbumDetailComponent` conditionally shows the smart-album mode indicator and hides the per-asset remove button and the "Add to album" action in the gallery when the target album is a smart album.

## Impact

- **`V14__add_smart_albums.sql`** *(new)*: `ALTER TABLE albums ADD COLUMN filter_json JSONB`; creates partial index.
- **`AlbumEntity.java`**: add `@Column(name = "filter_json", columnDefinition = "jsonb") String filterJson` (stored as a JSON string, parsed by the service layer).
- **`Album.java`** (domain model): add `String filterJson` field.
- **`AlbumData.java`**: add `String filterJson` to the record.
- **`AlbumSummaryDto.java`**: add `filterJson` field exposed in list responses.
- **`AlbumDto.java`**: add `filterJson` field exposed in detail response.
- **`CreateAlbumRequest.java`**: add optional `FilterJson filterJson` nested object (reusing `FilterPreset` shape).
- **`UpdateAlbumRequest.java`**: add optional `FilterJson filterJson` nested object.
- **`AlbumRepository`** (port): add `PaginatedResult<Asset> findSmartAlbumAssets(AssetFilter filter, int page, int pageSize)`.
- **`AlbumRepositoryImpl.java`**: implement `findSmartAlbumAssets` by delegating to `AssetRepositoryImpl.findFiltered` with no folder constraint; extend `countAssets` to accept an optional `AssetFilter` for smart albums.
- **`GetAlbumUseCaseImpl.java`**: branch in `executeAssets` and `executeSummary`'s count call based on `album.getFilterJson()`.
- **`CreateAlbumUseCaseImpl.java`**: persist `filterJson` when present.
- **`UpdateAlbumUseCaseImpl.java`**: update `filterJson` field; allow toggling smart/static mode.
- **`AlbumEntityMapper.java`** and **`AlbumWebMapper.java`**: map `filterJson` field through both mapper chains.
- **`album.model.ts`**: add `AlbumFilterJson` interface and optional `filterJson` field to `AlbumSummary` and `Album`.
- **`album.service.ts`**: no new HTTP methods; existing methods already carry `filterJson` in request/response bodies.
- **`AlbumsComponent`**: smart-album badge on cards.
- **`AlbumDetailComponent`**: mode indicator; conditional hide of remove-asset button.
- **Create-album form** in `AlbumsComponent`: "Make smart" toggle revealing filter inputs.
- **No changes to `album_assets` join table** — static album membership remains intact.
- **No breaking API changes** — `filterJson` is optional in all request and response DTOs; existing static albums continue to work identically.
