## 1. Backend — Database migration

- [ ] 1.1 Create `V14__add_smart_albums.sql` in `backend/src/main/resources/db/migration/`:
  ```sql
  ALTER TABLE albums ADD COLUMN filter_json JSONB NULL;
  CREATE INDEX ix_albums_filter_json_not_null ON albums(album_id) WHERE filter_json IS NOT NULL;
  ```

## 2. Backend — Domain model

- [ ] 2.1 Add `String filterJson` field to `Album.java` (domain model in `domain/model/`); annotate with no special annotation — it is a plain nullable String holding serialized JSON
- [ ] 2.2 Add `String filterJson` field to `AlbumData.java` record in `application/dto/` (add as the last component of the record)

## 3. Backend — Infrastructure entity and mapper

- [ ] 3.1 Add `@Column(name = "filter_json", columnDefinition = "jsonb") String filterJson` to `AlbumEntity.java` in `infrastructure/persistence/entity/`
- [ ] 3.2 Update `AlbumEntityMapper.java` (MapStruct interface in `infrastructure/persistence/mapper/`) to map `filterJson` in both `toDomain` and `toEntity` directions; the field names are identical so MapStruct maps it automatically — verify or add explicit `@Mapping` if the generated code omits it
- [ ] 3.3 Update `AlbumWebMapper.java` (MapStruct interface in `infrastructure/web/mapper/`) to map `filterJson` from `AlbumData` to `AlbumSummaryDto` — add explicit `@Mapping(source = "filterJson", target = "filterJson")` to `toSummaryDto`

## 4. Backend — HTTP DTOs

- [ ] 4.1 Add `AlbumFilterJson` nested record in `infrastructure/web/dto/` (new file `AlbumFilterJson.java`):
  ```java
  public record AlbumFilterJson(
      String search,
      String dateFrom,
      String dateTo,
      Integer minRating
  ) {}
  ```
- [ ] 4.2 Add `AlbumFilterJson filterJson` field to `CreateAlbumRequest.java`; no validation annotation — the field is optional; add class-level `@AssertTrue` validation method `isFilterJsonValid()` that returns `true` when `filterJson == null` or at least one field of `filterJson` is non-null
- [ ] 4.3 Add `AlbumFilterJson filterJson` field to `UpdateAlbumRequest.java` with the same `isFilterJsonValid()` constraint
- [ ] 4.4 Add `AlbumFilterJson filterJson` field to `AlbumSummaryDto.java`
- [ ] 4.5 Add `AlbumFilterJson filterJson` field to `AlbumDto.java`

## 5. Backend — JPA filter query extension

- [ ] 5.1 Modify `JpaAssetRepository.findWithFilters` in `infrastructure/persistence/jpa/JpaAssetRepository.java`: change the line `predicates.add(cb.equal(root.get("folder"), folder));` to be conditional — `if (folder != null) { predicates.add(cb.equal(root.get("folder"), folder)); }` — so that passing `folder = null` queries across all folders; no other changes to the method signature or existing callers (all existing callers pass a non-null folder entity)

## 6. Backend — Repository port and adapter

- [ ] 6.1 Add method `PaginatedResult<Asset> findSmartAlbumAssets(AssetFilter filter, int page, int pageSize)` to `AlbumRepository` interface in `domain/port/out/AlbumRepository.java`
- [ ] 6.2 Implement `findSmartAlbumAssets` in `AlbumRepositoryImpl.java`: delegate to `jpaAssetRepository.findWithFilters(null, ...)` using the filter fields; wrap the `Page<AssetEntity>` result in `PaginatedResult<Asset>` (same pattern as `findAssetsByAlbumId`); inject `JpaAssetRepository` as a new constructor dependency

## 7. Backend — Exception

- [ ] 7.1 Create `SmartAlbumMembershipException.java` in `application/exception/` as a `RuntimeException` subclass:
  ```java
  public class SmartAlbumMembershipException extends RuntimeException {
      public SmartAlbumMembershipException(String action) {
          super("Cannot manually " + action + " assets to/from a smart album");
      }
  }
  ```
- [ ] 7.2 Register a handler in `infrastructure/web/exception/GlobalExceptionHandler.java` mapping `SmartAlbumMembershipException` to `422 Unprocessable Entity` with body `{ "code": "SMART_ALBUM_MEMBERSHIP_FORBIDDEN", "message": "<exception message>" }`

## 8. Backend — Use cases

- [ ] 8.1 Modify `CreateAlbumUseCaseImpl.java`: inject `ObjectMapper`; when `request.filterJson()` is non-null, serialize it to a JSON string via `objectMapper.writeValueAsString(filterJson)` and set on the `Album` domain object before saving; wrap `JsonProcessingException` in `RuntimeException`
- [ ] 8.2 Modify `UpdateAlbumUseCaseImpl.java`: when `request.filterJson()` is explicitly included in the update call, serialize and set the new value on the album (including `null` to clear smart mode); leave the `album_assets` table untouched
- [ ] 8.3 Modify `GetAlbumUseCaseImpl.executeAssets`: inject `ObjectMapper`; after loading the album via `albumRepository.findByIdAndUserId`, check `album.getFilterJson() != null`; if true, deserialize `filterJson` to `AlbumFilterJson`, build an `AssetFilter` with `folderId = null`, `search = filterJson.search()`, `dateFrom = parseLocalDate(filterJson.dateFrom())`, `dateTo = parseLocalDate(filterJson.dateTo())`, `minRating = filterJson.minRating()`, `page = page`, `pageSize = PAGE_SIZE`, `includeDeleted = false`, `tags = Set.of()`, `sortCriteria = SortCriteria.FILE_NAME`; call `albumRepository.findSmartAlbumAssets(filter, page, PAGE_SIZE)`; otherwise fall through to existing `albumRepository.findAssetsByAlbumId` path
- [ ] 8.4 Modify `GetAlbumUseCaseImpl.executeSummary`: when the album is a smart album, obtain `assetCount` from `albumRepository.findSmartAlbumAssets(filter, 0, 1).total()` rather than `albumRepository.countAssets(albumId)` — the `PaginatedResult.total()` gives the filtered total from the first-page query; add a private helper `buildAssetFilter(AlbumFilterJson, int, int)` to avoid code duplication between `executeAssets` and `executeSummary`
- [ ] 8.5 Modify `AddAssetsToAlbumUseCaseImpl.java`: load the album; if `album.getFilterJson() != null` throw `new SmartAlbumMembershipException("add")`; otherwise proceed with existing logic
- [ ] 8.6 Modify `RemoveAssetsFromAlbumUseCaseImpl.java`: load the album; if `album.getFilterJson() != null` throw `new SmartAlbumMembershipException("remove")`; otherwise proceed with existing logic

## 9. Backend — Controller

- [ ] 9.1 Update `AlbumController.createAlbum`: pass `filterJson` from the request to `createAlbumUseCase.execute` — update the use case interface `CreateAlbumUseCase` to accept the serialized `filterJson` string (or an `AlbumFilterJson` DTO); serialize in the controller using a private helper that calls `objectMapper.writeValueAsString` on the DTO before passing to the use case; or handle serialization inside the use case (consistent with Decision 3 — serialization belongs in the use case)
- [ ] 9.2 Update `AlbumController.updateAlbum`: pass `filterJson` from the request to `updateAlbumUseCase.execute`; update the `UpdateAlbumUseCase` interface accordingly
- [ ] 9.3 Update `AlbumController.getAlbum`: the `AlbumDto` response already maps `filterJson` through `AlbumWebMapper`; verify that the `filterJson` field is populated in the response for smart albums; deserialize the stored JSON string back to `AlbumFilterJson` DTO in `AlbumWebMapper.toDto` using `@Named` qualifier and a default method or Jackson `ObjectMapper` injected into the mapper

## 10. Backend — Tests

- [ ] 10.1 Add unit test in `AlbumUseCasesTest.java` (`@ExtendWith(MockitoExtension.class)`): `createAlbum_withFilterJson_storesSerializedJson` — stub `albumRepository.save` to capture the argument; assert the saved `Album.getFilterJson()` is a valid JSON string containing `minRating: 4`
- [ ] 10.2 Add unit test: `getAlbumAssets_smartAlbum_callsSmartAlbumAssets` — stub album with non-null `filterJson`; verify `albumRepository.findSmartAlbumAssets` is called; verify `albumRepository.findAssetsByAlbumId` is NOT called
- [ ] 10.3 Add unit test: `getAlbumAssets_staticAlbum_callsJoinTable` — stub album with null `filterJson`; verify `albumRepository.findAssetsByAlbumId` is called; verify `albumRepository.findSmartAlbumAssets` is NOT called
- [ ] 10.4 Add unit test: `addAssetsToSmartAlbum_throws422` — stub album with non-null `filterJson`; call `addAssetsToAlbumUseCase.execute`; assert `SmartAlbumMembershipException` is thrown
- [ ] 10.5 Add unit test: `removeAssetsFromSmartAlbum_throws422` — same pattern for remove
- [ ] 10.6 Update `AlbumControllerTest.java` (`@WebMvcTest`):
  - Add test: `POST /api/albums` with `filterJson` body returns `201 Created` with `filterJson` in the response DTO
  - Add test: `PUT /api/albums/{id}` with `filterJson: null` body returns `200 OK` with `filterJson: null`
  - Add test: `POST /api/albums/{id}/assets` for a smart album returns `422` with body containing `SMART_ALBUM_MEMBERSHIP_FORBIDDEN`
  - Add test: `DELETE /api/albums/{id}/assets` for a smart album returns `422`
  - Add test: `POST /api/albums` with `filterJson: {}` (all-null criteria) returns `400 Bad Request`
- [ ] 10.7 Update `AlbumServiceIntegrationTest.java` (`@SpringBootTest`): add integration test `smartAlbum_returnsFilteredAssets` — create assets with rating 4 and 3; create smart album with `filterJson = { "minRating": 4 }`; call `getAlbumUseCase.executeAssets`; assert only the 4-star asset is returned; assert no `album_assets` rows were created

## 11. Frontend — Models

- [ ] 11.1 Add `AlbumFilterJson` interface to `frontend/src/app/core/models/album.model.ts`:
  ```typescript
  export interface AlbumFilterJson {
    search?: string;
    dateFrom?: string;
    dateTo?: string;
    minRating?: number;
  }
  ```
- [ ] 11.2 Add `filterJson?: AlbumFilterJson | null` to the `AlbumSummary` interface
- [ ] 11.3 Add `filterJson?: AlbumFilterJson | null` to the `Album` interface
- [ ] 11.4 Add `filterJson?: AlbumFilterJson` to the `CreateAlbumRequest` interface
- [ ] 11.5 Add `filterJson?: AlbumFilterJson | null` to the `UpdateAlbumRequest` interface

## 12. Frontend — AlbumsComponent

- [ ] 12.1 Add a `isSmartAlbum(album: AlbumSummary): boolean` helper that returns `album.filterJson != null`
- [ ] 12.2 In `albums.component.html`, add a `<mat-chip>Smart</mat-chip>` inside each album card's `mat-card-header` conditioned on `@if (isSmartAlbum(album))`; bind `[matTooltip]="formatFilterSummary(album.filterJson!)"` so hovering shows the criteria
- [ ] 12.3 Add `formatFilterSummary(filter: AlbumFilterJson): string` method to `AlbumsComponent` that builds a human-readable string from the filter fields (e.g. `"Min rating: 4, Search: vacation"`)
- [ ] 12.4 Add a "Make smart" `mat-slide-toggle` to the inline create-album form; when toggled on, show filter fields: `<input matInput placeholder="Search">`, two `<mat-datepicker>` fields for "Date from" and "Date to", and a star-rating selector for "Min rating" (reuse the same `mat-icon`-based rating widget used in the gallery filter toolbar); import `MatSlideToggleModule`, `MatDatepickerModule`, `MatNativeDateModule`, `MatChipsModule`, `MatTooltipModule`
- [ ] 12.5 Update `createAlbum(name: string, filterJson?: AlbumFilterJson)` method: when the smart toggle is on and at least one filter field is filled, include `filterJson` in the `CreateAlbumRequest`; when smart toggle is off, omit `filterJson`

## 13. Frontend — AlbumDetailComponent

- [ ] 13.1 In `album-detail.component.ts`, add `isSmartAlbum(): boolean` getter that returns `this.album?.filterJson != null`
- [ ] 13.2 Add `filterSummary(): string` getter that delegates to the same formatting logic as `AlbumsComponent.formatFilterSummary`; consider extracting to a shared pipe `album-filter-summary.pipe.ts` in `shared/pipes/` if the duplication is significant
- [ ] 13.3 In `album-detail.component.html`, add a smart-album banner after the toolbar:
  ```html
  @if (isSmartAlbum()) {
    <mat-card class="smart-album-banner">
      <mat-icon>auto_awesome</mat-icon>
      Smart album — contents are populated automatically based on: {{ filterSummary() }}
      <button mat-button (click)="openEditFilterDialog()">Edit filter</button>
    </mat-card>
  }
  ```
- [ ] 13.4 Hide the per-asset "Remove" button conditionally: change the existing `<button>` for remove to `@if (!isSmartAlbum()) { <button ...>Remove</button> }`
- [ ] 13.5 Implement `openEditFilterDialog(): void`: open a `MatDialog` pre-populated with the album's current `filterJson` criteria using the same filter inputs as the create form; on confirm, call `albumService.updateAlbum(albumId, { name: this.album!.name, filterJson: updatedFilter })` then reload the album
- [ ] 13.6 Update `album-detail.component.scss`: add `.smart-album-banner` styles with a subtle background color, `display: flex; align-items: center; gap: 8px; padding: 8px 16px; margin: 8px 16px`

## 14. Frontend — AddToAlbumDialogComponent

- [ ] 14.1 In `AddToAlbumDialogComponent`, filter or mark the albums list so that smart albums are not selectable: for each album option, bind `[disabled]="isSmartAlbum(album)"` on the `<mat-option>` and add `[matTooltip]="isSmartAlbum(album) ? 'Smart album — managed automatically' : ''"` so the tooltip is visible on hover
- [ ] 14.2 Import `MatTooltipModule` into `AddToAlbumDialogComponent` if not already imported

## 15. Frontend — Tests

- [ ] 15.1 In `albums.component.cy.ts`: add test `smartAlbum_showsSmartBadge` — mount with one album having `filterJson: { minRating: 4 }` and one without; assert the smart album card has a `mat-chip` with text "Smart"; assert the static album card has no such chip
- [ ] 15.2 Add test `createAlbumForm_smartToggle_showsFilterFields` — mount component; find and toggle "Make smart"; assert filter inputs appear; set `minRating` to 4; confirm; assert `albumService.createAlbum` is called with `filterJson: { minRating: 4 }`
- [ ] 15.3 In `album-detail.component.cy.ts`: add test `smartAlbumDetail_showsBannerAndHidesRemoveButton` — mount with album having `filterJson: { minRating: 4 }`; assert the smart-album banner text is visible; assert no "Remove" button is rendered on asset cards
- [ ] 15.4 Add test `staticAlbumDetail_showsRemoveButtonAndNoBanner` — mount with album having `filterJson: null`; assert no banner; assert "Remove" button is visible on each asset card
- [ ] 15.5 Add test `smartAlbumDetail_editFilter_callsUpdateAlbum` — mount with smart album; click "Edit filter"; change `minRating` to 5 in dialog; confirm; assert `albumService.updateAlbum` is called with `filterJson: { minRating: 5 }`

## 16. Testing and Commit

- [ ] 16.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 16.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 16.3 Commit all changes (only after both test suites pass)
