## Context

`Asset.java` currently has fields `assetId`, `folder`, `fileName`, `fileSize`, `pixelWidth`, `pixelHeight`, `thumbnailPixelWidth`, `thumbnailPixelHeight`, `imageRotation`, `thumbnailCreationDateTime`, `hash`, `fileCreationDateTime`, `fileModificationDateTime`. Adding `rating` follows the same pattern as adding `deletedAt` in the soft-delete spec.

`PhotoManagerFacadeImpl` holds a `SORT_MAP` keyed on `SortCriteria` enum values. The gallery's `GalleryComponent` has a `sortOptions` array that is the frontend mirror of this enum.

The `findByFolderWithFilters` JPQL method is introduced by the search-and-filter improvement (V7/improvement 7). This design extends that query; if search-and-filter is not yet applied, the same JPQL must be created from scratch here with the rating predicate included from the outset.

The current Flyway migrations run to V6 in the codebase (V1–V6). Planned migrations in other specs: V7 (EXIF metadata panel), V8 (virtual albums), V9 (refresh token), V10 (soft delete). This spec targets **V11**.

## Goals / Non-Goals

**Goals:**

- Store a 0–5 integer rating per asset, persisted in the database.
- Let users rate photos from the thumbnail grid and the full-size viewer.
- Support filtering the gallery by minimum rating.
- Support sorting the gallery by rating descending.
- Expose rating in `AssetDto` so no extra API call is needed to render stars.

**Non-Goals:**

- Half-star ratings (0.5 increments).
- Rating history / audit trail.
- Per-user ratings (ratings are shared across all users, consistent with the shared catalog model).
- Batch rating of selected assets.

## Decisions

### 1. `rating` column on `assets` — not a separate ratings table

**Decision:** A `rating SMALLINT NOT NULL DEFAULT 0` column is added directly to the `assets` table.

**Rationale:** Ratings are single-valued per asset. A separate table would require a join on every gallery query — the most frequent read path in the application. The `CHECK (rating BETWEEN 0 AND 5)` constraint enforces the valid range at the database level. This mirrors the `deleted_at` pattern from soft-delete: co-located metadata on the asset row is simpler and faster than a sibling table.

### 2. `PATCH /api/assets/{id}/rating` — dedicated partial-update endpoint

**Decision:** A new `PATCH /{id}/rating` handler is added to `AssetController`. The request body is `{ "rating": int }` validated with `@Min(0) @Max(5)`. Response is `204 No Content`.

**Rationale:** `PATCH` is the correct HTTP method for a partial update of a single field. A full `PUT /api/assets/{id}` would require the client to send all writable fields, which is unnecessary here. A query-param approach (`PATCH /api/assets/{id}?rating=4`) would be non-standard for a mutation. The dedicated endpoint is self-documenting and easy to test.

### 3. Rating 0 means "unrated" — not a separate null/unrated state

**Decision:** `rating = 0` represents an unrated asset. The column is `NOT NULL DEFAULT 0`. There is no `null` state.

**Rationale:** A nullable column would require `IS NULL` handling throughout the JPQL, the entity, the DTO, and the frontend. Since the valid range is 1–5 and "no rating" is a meaningful state, mapping it to 0 is simpler and consistent with how many photo apps (Lightroom, digiKam) represent unrated photos. `minRating=1` in the filter naturally excludes unrated photos.

### 4. `RATING` added to the `SortCriteria` enum

**Decision:** `SortCriteria.RATING` maps to `Sort.by("rating").descending()` in the `SORT_MAP`. The frontend adds `{ value: 'RATING', label: 'Rating: High → Low' }` to `sortOptions`.

**Rationale:** The existing sort mechanism is the right extension point. Adding an enum value and a map entry costs nothing and follows the established pattern. There is no need for a separate "sort by rating ascending" option since the primary use case is finding the best-rated photos first.

### 5. `minRating` predicate extends `findByFolderWithFilters` JPQL

**Decision:** The existing JPQL from search-and-filter is extended with `AND (:minRating IS NULL OR a.rating >= :minRating)`. When `minRating` is null (not provided) the predicate is always true. The controller accepts an optional `@RequestParam Integer minRating`; values of 0 are coerced to `null` in the facade before being passed to the repository.

**Rationale:** The `(:param IS NULL OR ...)` JPQL pattern established in search-and-filter is consistent and generated efficiently by Hibernate. Coercing 0 to null in the facade means the repository never receives a semantically meaningless `minRating=0` filter, keeping the query cleaner.

### 6. Star icons rendered with `MatIconModule` — no third-party rating widget

**Decision:** Five `<mat-icon>` elements (`star` / `star_border`) per thumbnail card and viewer toolbar row. Click on a star sets the rating to that value; clicking the currently active star sets it to 0 (toggle off). The widget is controlled — state is read from `asset.rating`.

**Rationale:** Angular Material's icon set includes `star`, `star_half`, and `star_border`, which are sufficient for a 0–5 integer rating. A third-party rating component would add a new npm dependency for functionality that is trivially composable from existing Material icons. The star icons are also used in the filter toolbar for the minimum-rating selector, so a unified implementation avoids two different star-rendering approaches.

## Data Flow

```
User hovers thumbnail → star overlay appears
User clicks star N on asset A
  → GalleryComponent.rateAsset(asset, N, event)
      event.stopPropagation()  // prevent thumbnail selection
      newRating = (asset.rating === N) ? 0 : N
      → assetService.rateAsset(asset.assetId, newRating)
        → PATCH /api/assets/{id}/rating { rating: newRating }
          → AssetController.rateAsset(id, body)
          → facade.rateAsset(id, body.rating())
            → assetRepository.findById(id) → asset.setRating(newRating) → save
          → 204 No Content
      on success: asset.rating = newRating  (local update, no reload needed)

User sets minRating = 3 in filter toolbar
  → GalleryComponent.onMinRatingChange()
      pageIndex = 0
      → loadAssets()
        → assetService.getAssets(folder, 0, sort, search, dateFrom, dateTo, 3)
          → GET /api/assets?folderPath=...&minRating=3
            → AssetController.getAssets(..., minRating=3)
            → facade.getAssets(..., minRating=3)
              → assetRepository.findByFolderWithFilters(folder, ..., 3, pageable)
                // JPQL: AND (:minRating IS NULL OR a.rating >= :minRating)
            → PaginatedData<Asset> with only rating≥3 assets
          → 200 OK PaginatedData<AssetDto>
      → assets updated in grid
```

## File Change List

**New files:**

- `backend/.../db/migration/V11__add_asset_rating.sql`
- `backend/.../api/dto/RateAssetRequest.java`

**Modified files:**

- `backend/.../domain/entity/Asset.java` — add `rating` field
- `backend/.../domain/repository/AssetRepository.java` — add `minRating` param to `findByFolderWithFilters`
- `backend/.../domain/enums/SortCriteria.java` — add `RATING`
- `backend/.../application/PhotoManagerFacade.java` — add `rateAsset`; extend `getAssets` with `minRating`
- `backend/.../application/PhotoManagerFacadeImpl.java` — implement `rateAsset`; pass `minRating`; add `RATING` to `SORT_MAP`
- `backend/.../api/AssetController.java` — add `PATCH /{id}/rating`; add `minRating` param
- `frontend/.../core/models/asset.model.ts` — add `rating`; add `'RATING'` to `SortCriteria`
- `frontend/.../core/services/asset.service.ts` — add `rateAsset`; extend `getAssets`
- `frontend/.../shared/components/thumbnail/thumbnail.component.ts/html` — add `rating` input and star display
- `frontend/.../features/gallery/gallery.component.ts` — rating state and methods
- `frontend/.../features/gallery/gallery.component.html` — star controls in toolbar and viewer
- `frontend/.../features/gallery/gallery.component.scss` — star styling
