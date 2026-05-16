> **Prerequisite:** Apply the `hexagonal-architecture` change before this one. The tasks below target the post-refactor package structure: use-case implementations in `application/usecase/`, driven ports in `domain/port/out/`, controllers in `infrastructure/web/controller/`, JPA interfaces in `infrastructure/persistence/jpa/`, persistence adapters in `infrastructure/persistence/adapter/`.

## 1. Backend: Data Projections and DTOs

- [ ] 1.1 Create `AssetSummary` Spring Data interface projection in `infrastructure/persistence/jpa/` with `getAssetId()`, `getFileName()`, `getFolderPath()`, and `getThumbnailUrl()` (derived as `/api/assets/{assetId}/thumbnail`)
- [ ] 1.2 Create `FolderStat` record in `application/dto/` with fields `String path` and `long assetCount`
- [ ] 1.3 Extend the `HomeStats` record in `application/dto/` with four new fields: `long totalFileSize`, `long duplicateCount`, `List<FolderStat> topFolders`, `List<AssetSummaryDto> recentAssets`

## 2. Backend: Repository Port and Adapter

- [ ] 2.1 Add `long sumFileSize()` method to `domain/port/out/AssetRepository`; implement in `infrastructure/persistence/adapter/AssetRepositoryImpl` using a JPQL `@Query` on `JpaAssetRepository` that returns `SUM(a.fileSize)` across all non-deleted assets
- [ ] 2.2 Add `long countDuplicates()` method to `domain/port/out/AssetRepository`; implement in `AssetRepositoryImpl` using a JPQL `@Query` on `JpaAssetRepository` that counts assets whose hash appears more than once (scalar subquery approach)
- [ ] 2.3 Add `List<FolderStat> findTopFoldersByAssetCount(int limit)` to `domain/port/out/AssetRepository`; create `FolderAssetCount` Spring Data interface projection in `infrastructure/persistence/jpa/`; implement in `AssetRepositoryImpl` using a `GROUP BY` JPQL query on `JpaAssetRepository` returning folder path and count ordered descending
- [ ] 2.4 Add `List<Asset> findRecentAssets(int limit)` to `domain/port/out/AssetRepository`; implement in `AssetRepositoryImpl` by querying `JpaAssetRepository` with the `AssetSummary` projection ordered by `thumbnailCreationDateTime DESC`, then mapping to domain `Asset` objects

## 3. Backend: Use Case and Controller

- [ ] 3.1 Update `application/usecase/home/GetHomeStatsUseCaseImpl` to call the four new `AssetRepository` methods and populate the extended `HomeStats` record; wrap each new call in a null-safe guard for empty libraries
- [ ] 3.2 Verify `infrastructure/web/controller/HomeController` delegates to `GetHomeStatsUseCase` — no HTTP changes needed; same endpoint, same path; update any DTO mapping in `infrastructure/web/mapper/` if the controller uses an HTTP-layer DTO
- [ ] 3.3 Write unit tests for `GetHomeStatsUseCaseImpl`: verify each new field is populated correctly; mock `AssetRepository`; cover the empty-library case

## 4. Backend: API Tests

- [ ] 4.1 Update the `@WebMvcTest` for `infrastructure/web/controller/HomeController` to assert the four new fields are present in the response JSON; mock `GetHomeStatsUseCase`
- [ ] 4.2 Write an integration test (`@SpringBootTest` + Testcontainers) that seeds assets with known sizes and hashes, calls `GET /api/home/stats`, and asserts `totalFileSize`, `duplicateCount`, `topFolders`, and `recentAssets` values

## 5. Frontend: Models

- [ ] 5.1 Add `AssetSummary` interface to `core/models/` with `assetId`, `fileName`, `folderPath`, `thumbnailUrl`
- [ ] 5.2 Add `FolderStat` interface to `core/models/` with `path` and `assetCount`
- [ ] 5.3 Extend the `HomeStats` interface in `core/models/home-stats.model.ts` with `totalFileSize`, `duplicateCount`, `topFolders: FolderStat[]`, `recentAssets: AssetSummary[]`

## 6. Frontend: Gallery Query-Param Pre-selection

- [ ] 6.1 Inject `ActivatedRoute` into `GalleryComponent`
- [ ] 6.2 In `GalleryComponent.ngOnInit()`, read `queryParams.folder`; if present, call `onFolderSelected(folderPath)` to pre-load that folder's assets
- [ ] 6.3 Write a Cypress component test for the query-param pre-selection: mount `GalleryComponent` with `?folder=/photos`; verify `onFolderSelected` is called with the correct path

## 7. Frontend: HomeComponent Rewrite

- [ ] 7.1 Add `MatBadgeModule`, `MatListModule`, `RouterLink` imports to `HomeComponent`; add `ThumbnailComponent` and `FileSizePipe` to the imports array
- [ ] 7.2 Implement the **Quick Actions row** in the template: four `mat-button` elements with `routerLink`; bind `[matBadge]` and `[matBadgeHidden]` on the Duplicates button using `stats.duplicateCount`
- [ ] 7.3 Add the **Total Size** stat card alongside the existing three cards, piping `stats.totalFileSize` through `FileSizePipe`
- [ ] 7.4 Implement the **Recent Photos strip**: a horizontally scrollable `div` containing `<app-thumbnail>` for each entry in `stats.recentAssets`; bind a `(click)` handler that navigates to `/gallery?folder=<folderPath>`
- [ ] 7.5 Implement the **Top Folders list**: an `@for` loop over `stats.topFolders`; each row shows path, asset count, and a `div` whose `width` is set to `(folder.assetCount / maxFolderCount) * 100 + '%'`
- [ ] 7.6 Add `@if` guards to hide the recent photos and top folders sections when their lists are empty, showing appropriate empty-state messages
- [ ] 7.7 Add `home.component.scss` styles: grid layout for stat cards, horizontal scroll for the photo strip, bar styling for top folders, responsive breakpoints

## 8. Frontend: HomeComponent Tests

- [ ] 8.1 Write a Cypress component test: stats with all four new fields populated — assert Total Size card is visible, badge shows on Duplicates button, recent photos strip has 12 thumbnails, top folders list has 5 rows
- [ ] 8.2 Write a Cypress component test: empty library stats — assert recent photos strip is hidden, top folders section is hidden, Total Size shows zero, badge is hidden
- [ ] 8.3 Write a Cypress component test: clicking a recent photo thumbnail — verify router navigates to `/gallery?folder=<path>`

## 9. Integration Verification

- [ ] 9.1 Run `mvn test` in the backend — all tests pass
- [ ] 9.2 Run `npm test` in the frontend — all Cypress component tests pass
- [ ] 9.3 Run `npm run lint` — no lint errors
- [ ] 9.4 Start the app locally; navigate to `/home`; verify all four dashboard sections render correctly with real data; click a recent photo and confirm gallery opens at the correct folder
