## 1. Flyway migration V15

- [ ] 1.1 Create `V15__add_aspect_ratio.sql`:
  ```sql
  ALTER TABLE assets ADD COLUMN aspect_ratio FLOAT;
  UPDATE assets SET aspect_ratio = CAST(pixel_width AS FLOAT) / pixel_height WHERE pixel_height > 0;
  ```

## 2. Domain model

- [ ] 2.1 Add `Double aspectRatio` field to `AssetEntity` with `@Column(name = "aspect_ratio")`
- [ ] 2.2 Add `Double aspectRatio` field to `Asset` domain model
- [ ] 2.3 Update the MapStruct mapper to include `aspectRatio`

## 3. Catalog service update

- [ ] 3.1 In the catalog service (`CatalogAssetsUseCaseImpl` or `StorageServiceImpl`), compute `aspectRatio = pixelHeight > 0 ? (double) pixelWidth / pixelHeight : null` and set it on the `Asset` before saving

## 4. Repository query

- [ ] 4.1 Add a native query or JPQL method to `JpaAssetRepository`:
  ```java
  @Query(value = "SELECT * FROM assets WHERE deleted_at IS NULL AND pixel_width >= :w AND pixel_height >= :h AND ABS(aspect_ratio - :ratio) <= 0.02 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
  Optional<AssetEntity> findWallpaperSuggestion(@Param("w") int w, @Param("h") int h, @Param("ratio") double ratio);
  ```

## 5. Use case

- [ ] 5.1 Create `domain/port/in/asset/GetWallpaperSuggestionUseCase.java` with `Optional<Asset> execute(int screenWidth, int screenHeight)`
- [ ] 5.2 Create `application/usecase/asset/GetWallpaperSuggestionUseCaseImpl.java` computing `ratio = (double) screenWidth / screenHeight` and calling the repository

## 6. HTTP adapter

- [ ] 6.1 Add `GET /api/assets/wallpaper-suggestion?screenWidth={w}&screenHeight={h}` to `AssetController` returning `200 AssetDto` or `404`
- [ ] 6.2 Add `getWallpaperSuggestion(int screenWidth, int screenHeight)` to `PhotoManagerFacade`

## 7. Backend unit tests

- [ ] 7.1 Test that a matching asset is returned when dimensions and ratio meet the criteria
- [ ] 7.2 Test that soft-deleted assets are excluded
- [ ] 7.3 Test that `404` is returned when no asset matches
- [ ] 7.4 Test that `aspect_ratio` is correctly computed and set during cataloging

## 8. Frontend — AssetService

- [ ] 8.1 Add `getWallpaperSuggestion(): Observable<Asset>` to `AssetService` using `window.screen.width` and `window.screen.height` as query parameters

## 9. Frontend — Gallery toolbar action and dialog

- [ ] 9.1 Add "Suggest wallpaper" button to `GalleryComponent` toolbar
- [ ] 9.2 On click, call `assetService.getWallpaperSuggestion()` and open a `MatDialog` with the result
- [ ] 9.3 Dialog shows asset thumbnail, filename, dimensions, and a download link (`<a [href]="imageUrl" [download]="asset.fileName">Download</a>`)
- [ ] 9.4 When the endpoint returns 404, show `MatSnackBar`: "No wallpaper found matching your screen resolution"

## 10. Frontend tests

- [ ] 10.1 Cypress component test: dialog shows asset info and download link when service returns a result
- [ ] 10.2 Cypress component test: snackbar message shown when service returns 404

## 11. Testing and Commit

- [ ] 11.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 11.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 11.3 Commit all changes (only after both test suites pass)
