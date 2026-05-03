## 1. Backend — Database migration

- [ ] 1.1 Create `V11__add_asset_rating.sql` in `backend/src/main/resources/db/migration/`:
  ```sql
  ALTER TABLE assets ADD COLUMN rating SMALLINT NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 5);
  CREATE INDEX ix_assets_rating ON assets(rating);
  ```

## 2. Backend — Entity and DTO

- [ ] 2.1 Add `@Column(name = "rating", nullable = false) private int rating = 0;` to `Asset.java`
- [ ] 2.2 In `AssetController`, find the private `toDto(Asset asset)` helper method and add `rating: asset.getRating()` to the `AssetDto` construction (add `int rating` field to `AssetDto` if it does not already exist)

## 3. Backend — Rating request DTO

- [ ] 3.1 Create `RateAssetRequest.java` in `api/dto/` as a Java record:
  ```java
  public record RateAssetRequest(
      @Min(0) @Max(5) int rating
  ) {}
  ```
  Import `jakarta.validation.constraints.Min` and `jakarta.validation.constraints.Max`

## 4. Backend — SortCriteria enum

- [ ] 4.1 Add `RATING` to the `SortCriteria` enum in `domain/enums/SortCriteria.java`
- [ ] 4.2 Add `SortCriteria.RATING, Sort.by("rating").descending()` to the `SORT_MAP` in `PhotoManagerFacadeImpl`

## 5. Backend — Repository

- [ ] 5.1 Extend the `findByFolderWithFilters` JPQL in `AssetRepository` (introduced by search-and-filter) with an additional predicate:
  ```
  AND (:minRating IS NULL OR a.rating >= :minRating)
  ```
  Add `@Param("minRating") Integer minRating` parameter to the method signature. If search-and-filter has not yet been applied, create the full method from scratch:
  ```java
  @Query("""
      SELECT a FROM Asset a
      WHERE a.folder = :folder
        AND (:search   IS NULL OR LOWER(a.fileName) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:dateFrom IS NULL OR a.fileCreationDateTime >= :dateFrom)
        AND (:dateTo   IS NULL OR a.fileCreationDateTime <= :dateTo)
        AND (:minRating IS NULL OR a.rating >= :minRating)
      """)
  Page<Asset> findByFolderWithFilters(
      @Param("folder")    Folder folder,
      @Param("search")    String search,
      @Param("dateFrom")  LocalDateTime dateFrom,
      @Param("dateTo")    LocalDateTime dateTo,
      @Param("minRating") Integer minRating,
      Pageable pageable);
  ```

## 6. Backend — Facade

- [ ] 6.1 Add `void rateAsset(Long assetId, int rating)` to `PhotoManagerFacade`
- [ ] 6.2 Extend `getAssets` in `PhotoManagerFacade` with `Integer minRating` parameter (add after existing params)
- [ ] 6.3 Implement `rateAsset` in `PhotoManagerFacadeImpl` annotated `@Transactional`:
  ```java
  Asset asset = assetRepository.findById(assetId)
      .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
  asset.setRating(rating);
  assetRepository.save(asset);
  ```
- [ ] 6.4 Update `PhotoManagerFacadeImpl.getAssets`: coerce `minRating = 0` to `null` before passing to the repository (`Integer ratingFilter = (minRating != null && minRating > 0) ? minRating : null`); pass `ratingFilter` to `findByFolderWithFilters`

## 7. Backend — Controller

- [ ] 7.1 Add `PATCH /{id}/rating` to `AssetController`:
  ```java
  @PatchMapping("/{id}/rating")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rateAsset(@PathVariable Long id,
                         @Valid @RequestBody RateAssetRequest body) {
      facade.rateAsset(id, body.rating());
  }
  ```
- [ ] 7.2 Add `@RequestParam(required = false) Integer minRating` to the existing `getAssets` handler; pass it to `facade.getAssets`

## 8. Backend — Tests

- [ ] 8.1 `AssetControllerTest`: add test `PATCH /api/assets/42/rating` with body `{"rating":4}` returns `204` and verifies `facade.rateAsset(42L, 4)` is called
- [ ] 8.2 Add test: `PATCH /api/assets/42/rating` with `{"rating":6}` returns `400 Bad Request`
- [ ] 8.3 Add test: `PATCH /api/assets/42/rating` with `{"rating":-1}` returns `400 Bad Request`
- [ ] 8.4 Add test: `GET /api/assets?folderPath=/photos&page=0&minRating=3` verifies `facade.getAssets` is called with `minRating = 3`
- [ ] 8.5 Add unit test for `PhotoManagerFacadeImpl.rateAsset`: mock `assetRepository.findById(42L)` returning a test asset; call `rateAsset(42L, 4)`; assert `assetRepository.save(asset)` is called and `asset.getRating() == 4`
- [ ] 8.6 Add unit test: call `rateAsset(99L, 3)` with `assetRepository.findById` returning empty; assert `NoSuchElementException` is thrown
- [ ] 8.7 Run `mvn test` and confirm all tests pass

## 9. Frontend — Asset model and service

- [ ] 9.1 Add `rating: number` to the `Asset` interface in `asset.model.ts`
- [ ] 9.2 Add `'RATING'` to the `SortCriteria` type in `asset.model.ts`
- [ ] 9.3 Add `rateAsset(assetId: number, rating: number): Observable<void>` to `AssetService`:
  ```typescript
  rateAsset(assetId: number, rating: number): Observable<void> {
    return this.http.patch<void>(`/api/assets/${assetId}/rating`, { rating });
  }
  ```
- [ ] 9.4 Extend `getAssets` in `AssetService` with an optional `minRating?: number` parameter; append `params = params.set('minRating', minRating)` to `HttpParams` when `minRating` is a positive integer

## 10. Frontend — GalleryComponent

- [ ] 10.1 Add `minRating = 0` to `GalleryComponent` state fields
- [ ] 10.2 Add `{ value: 'RATING', label: 'Rating: High → Low' }` to the `sortOptions` array
- [ ] 10.3 Add method `onMinRatingChange(): void` — set `this.pageIndex = 0`; call `this.loadAssets()`
- [ ] 10.4 Add method `rateAsset(asset: Asset, star: number, event: Event): void`:
  ```typescript
  event.stopPropagation();
  const newRating = asset.rating === star ? 0 : star;
  this.assetService.rateAsset(asset.assetId, newRating).subscribe({
    next: () => { asset.rating = newRating; },
    error: () => this.snackBar.open('Failed to rate asset', 'Dismiss', { duration: 3000 })
  });
  ```
- [ ] 10.5 Add method `rateCurrentAsset(star: number): void` — delegate to `rateAsset(this.currentViewerAsset!, star, new Event('click'))`
- [ ] 10.6 Update `loadAssets()` to pass `this.minRating > 0 ? this.minRating : undefined` as the `minRating` argument to `assetService.getAssets`
- [ ] 10.7 In `clearFilters()` (or `onFolderSelected`), reset `this.minRating = 0`

## 11. Frontend — ThumbnailComponent

- [ ] 11.1 Add `@Input() rating = 0` to `ThumbnailComponent`
- [ ] 11.2 Add `@Output() ratingChange = new EventEmitter<number>()` to `ThumbnailComponent`
- [ ] 11.3 In `thumbnail.component.html`, add a star row below the image:
  ```html
  <div class="thumbnail-stars">
    @for (star of [1,2,3,4,5]; track star) {
      <mat-icon class="thumb-star" [class.filled]="rating >= star"
                (click)="ratingChange.emit(star); $event.stopPropagation()">
        {{ rating >= star ? 'star' : 'star_border' }}
      </mat-icon>
    }
  </div>
  ```
- [ ] 11.4 Add `.thumbnail-stars` CSS: `display: flex; justify-content: center; gap: 2px; padding: 4px 0;` with `.thumb-star { font-size: 16px; cursor: pointer; color: rgba(255,255,255,0.38); } .thumb-star.filled { color: #ffa726; }`
- [ ] 11.5 Add `MatIconModule` to `ThumbnailComponent`'s imports array if not already present

## 12. Frontend — Gallery template wiring

- [ ] 12.1 In `gallery.component.html`, bind rating to each thumbnail:
  ```html
  <app-thumbnail
    [asset]="asset"
    [rating]="asset.rating"
    [selected]="isSelected(asset)"
    (click)="toggleSelection(asset)"
    (dblclick)="openViewer(i)"
    (ratingChange)="rateAsset(asset, $event, $event)"
  />
  ```
- [ ] 12.2 Add a minimum-rating filter to the filter toolbar (inside `@if (viewMode === 'thumbnails')`):
  ```html
  <div class="filter-rating">
    @for (star of [1,2,3,4,5]; track star) {
      <mat-icon class="filter-star" [class.active]="minRating >= star"
                (click)="minRating = (minRating === star ? 0 : star); onMinRatingChange()">
        {{ minRating >= star ? 'star' : 'star_border' }}
      </mat-icon>
    }
    <span class="filter-rating-label">{{ minRating > 0 ? minRating + '★ +' : 'Any rating' }}</span>
  </div>
  ```
- [ ] 12.3 Add viewer toolbar star rating row inside `@if (viewMode === 'viewer')`:
  ```html
  @for (star of [1,2,3,4,5]; track star) {
    <button mat-icon-button (click)="rateCurrentAsset(star)">
      <mat-icon>{{ (currentViewerAsset?.rating ?? 0) >= star ? 'star' : 'star_border' }}</mat-icon>
    </button>
  }
  ```
- [ ] 12.4 Add to `gallery.component.scss`:
  ```scss
  .filter-rating {
    display: flex;
    align-items: center;
    gap: 2px;

    .filter-star {
      cursor: pointer;
      font-size: 20px;
      color: rgba(255, 255, 255, 0.38);
      &.active { color: #ffa726; }
    }

    .filter-rating-label {
      font-size: 12px;
      margin-left: 6px;
      color: rgba(255, 255, 255, 0.6);
    }
  }
  ```

## 13. Frontend — Tests

- [ ] 13.1 `gallery.component.cy.ts`: mount with a stubbed `AssetService` returning assets with `rating: 0`; click the 4th star on the first thumbnail; assert `assetService.rateAsset` is called with the correct asset ID and `rating = 4`; assert the 4th star icon has class `filled` after the call
- [ ] 13.2 Add test: click the 4th star again (asset now has `rating: 4`); assert `assetService.rateAsset` is called with `rating = 0` (toggle off)
- [ ] 13.3 Add test: click the 3rd filter-star in the filter toolbar; assert `assetService.getAssets` is called with `minRating = 3`; click the 3rd star again; assert `getAssets` is called with `minRating = 0` or no `minRating` param
- [ ] 13.4 Add test: select `"Rating: High → Low"` from the sort dropdown; assert `assetService.getAssets` is called with `sort = 'RATING'`
- [ ] 13.5 Run `npm test` and confirm all tests pass
