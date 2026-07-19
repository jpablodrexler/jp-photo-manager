## Why

Photographers cull large shoots by marking keepers with star ratings. The application currently has no way to flag preferred photos short of deleting the rest. Without a rating system, every selection decision must be redone in the next session because nothing is persisted. A 0–5 star rating lets users mark photo quality during review, then filter or sort by rating to quickly surface the best shots from any folder. The workflow maps naturally onto the soft-delete recycle bin: rate low → remove from catalog → purge, while high-rated photos are promoted to virtual albums.

## What Changes

- Add a Flyway migration `V11__add_asset_rating.sql` adding a `rating SMALLINT NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5)` column to the `assets` table with a supporting index.
- Add `int rating` to the `Asset` JPA entity and expose it in `AssetDto` so the gallery receives rating data alongside thumbnail URLs.
- Add `PATCH /api/assets/{id}/rating` accepting `{ "rating": 0-5 }` and returning `204 No Content`. A rating of 0 means unrated.
- Add `RATING` to the `SortCriteria` enum and wire it to `Sort.by("rating").descending()` in `SORT_MAP`, giving the gallery a "Rating: High → Low" sort option.
- Extend `GET /api/assets` with an optional `minRating` query param (integer 1–5); assets with `rating < minRating` are excluded. Extend `findByFolderWithFilters` in `AssetRepository` with `:minRating IS NULL OR a.rating >= :minRating`.
- Add a 5-star click widget to `ThumbnailComponent` that fires on hover; clicking a filled star again sets the rating to 0 (toggle-off). Show the same widget in the full-size viewer toolbar.
- Add a minimum-rating star selector to the gallery filter toolbar (next to the date pickers added by search-and-filter).

## Capabilities

### New Capabilities

- `star-ratings`: Every asset has a 0–5 star rating stored in the database. Users can rate photos from the thumbnail grid or the full-size viewer. Gallery queries support filtering by minimum rating and sorting by rating descending.

### Modified Capabilities

- **`GET /api/assets` (gallery listing)**: new optional `minRating` param; `AssetDto` gains a `rating` field.
- **`SortCriteria` enum**: new `RATING` value available in the sort dropdown.

## Impact

- **`V11__add_asset_rating.sql`** *(new)*: `ALTER TABLE assets ADD COLUMN rating SMALLINT NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5)`; `CREATE INDEX ix_assets_rating ON assets(rating)`.
- **`Asset.java`**: new `rating` int field (default 0).
- **`AssetDto`** (in `AssetController`): add `int rating` to the DTO assembled by `toDto(Asset)`.
- **`RateAssetRequest.java`** *(new)*: record in `api/dto/` with `@Min(0) @Max(5) int rating`.
- **`PhotoManagerFacade.java`**: new `void rateAsset(Long assetId, int rating)` signature; `getAssets` extended with `Integer minRating`.
- **`PhotoManagerFacadeImpl.java`**: implement `rateAsset`; pass `minRating` through to repository.
- **`AssetRepository.java`**: extend `findByFolderWithFilters` JPQL with `minRating` predicate.
- **`SortCriteria.java`**: add `RATING` value.
- **`AssetController.java`**: add `PATCH /{id}/rating` handler; add `minRating` request param to `getAssets`.
- **`asset.model.ts`**: add `rating: number`; add `'RATING'` to `SortCriteria`.
- **`asset.service.ts`**: add `rateAsset(assetId, rating)` method; extend `getAssets` with optional `minRating`.
- **`ThumbnailComponent`**: new `@Input() rating` bound to star icons; click handler emits `(ratingChange)`.
- **`GalleryComponent`**: wire `rateAsset` calls; add `minRating` filter state; add "Rating: High → Low" sort option.
- **No new routes or backend services beyond a single new facade method.**
