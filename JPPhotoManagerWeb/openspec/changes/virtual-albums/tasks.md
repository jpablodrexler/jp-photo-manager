## 1. Backend — Database migration

- [x] 1.1 Create `V8__add_albums.sql` in `backend/src/main/resources/db/migration/`; create table `albums` with columns `album_id BIGSERIAL PRIMARY KEY`, `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`, `name VARCHAR(255) NOT NULL`, `description TEXT`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`; create table `album_assets` with columns `album_id BIGINT NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE`, `asset_id BIGINT NOT NULL REFERENCES assets(asset_id) ON DELETE CASCADE`, `added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `CONSTRAINT pk_album_assets PRIMARY KEY (album_id, asset_id)`; create index `ix_albums_user_id ON albums(user_id)`

## 2. Backend — Entity and Repository

- [x] 2.1 Create `Album.java` in `domain/entity/`; annotate with `@Entity @Table(name = "albums") @Data @NoArgsConstructor`; fields: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long albumId`, `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) User user`, `@Column(nullable = false) String name`, `String description`, `@Column(nullable = false, updatable = false) Instant createdAt`; add `@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "album_assets", joinColumns = @JoinColumn(name = "album_id"), inverseJoinColumns = @JoinColumn(name = "asset_id")) List<Asset> assets = new ArrayList<>()`
- [x] 2.2 Create `AlbumRepository.java` in `domain/repository/` extending `JpaRepository<Album, Long>`; add `List<Album> findByUser_Id(UUID userId)`, `Optional<Album> findByAlbumIdAndUser_Id(Long albumId, UUID userId)`, and `@Query("SELECT COUNT(aa) FROM Album a JOIN a.assets aa WHERE a.albumId = :albumId") long countAssets(@Param("albumId") Long albumId)`

## 3. Backend — Domain service

- [x] 3.1 Create `AlbumService.java` interface in `domain/service/` with methods:
  - `List<Album> findByUserId(UUID userId)`
  - `Optional<Album> findByIdAndUserId(Long albumId, UUID userId)`
  - `Album createAlbum(UUID userId, String name, String description)`
  - `Album updateAlbum(Long albumId, UUID userId, String name, String description)`
  - `void deleteAlbum(Long albumId, UUID userId)`
  - `PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex)`
  - `void addAssets(Long albumId, UUID userId, List<Long> assetIds)`
  - `void removeAssets(Long albumId, UUID userId, List<Long> assetIds)`
- [x] 3.2 Create `AlbumServiceImpl.java` in `infrastructure/service/`; annotate `@Service @RequiredArgsConstructor @Slf4j`; inject `AlbumRepository albumRepository`, `UserRepository userRepository`, `AssetRepository assetRepository`; all write methods annotated `@Transactional`, reads annotated `@Transactional(readOnly = true)`
- [x] 3.3 Implement `createAlbum`: look up `User` by `userId` (throw `IllegalArgumentException` if not found); construct `Album`, set fields, call `albumRepository.save()`; return saved entity
- [x] 3.4 Implement `updateAlbum`: call `findByIdAndUserId` (throw `AlbumNotFoundException` if empty); update `name` and `description`; return saved entity
- [x] 3.5 Implement `deleteAlbum`: call `findByIdAndUserId` (throw `AlbumNotFoundException` if empty); call `albumRepository.delete(album)`
- [x] 3.6 Implement `getAlbumAssets`: fetch album via `findByIdAndUserId` (throw `AlbumNotFoundException` if empty); query `Page<Asset>` using `AssetRepository` with a `@Query` or `Pageable` on the join table; wrap in `PaginatedData<Asset>` (page size 50)
- [x] 3.7 Implement `addAssets`: fetch album; fetch `List<Asset>` by IDs via `assetRepository.findAllById(assetIds)`; add to `album.getAssets()` (skip duplicates); save
- [x] 3.8 Implement `removeAssets`: fetch album; remove matching assets from `album.getAssets()`; save

## 4. Backend — Exception

- [x] 4.1 Create `AlbumNotFoundException.java` in `api/exception/` as a `RuntimeException` subclass with constructor `AlbumNotFoundException(Long albumId)` setting message `"Album not found: " + albumId`

## 5. Backend — Application DTOs and Facade

- [x] 5.1 Create `AlbumData.java` in `application/dto/` as a Java record with fields `Long albumId`, `String name`, `String description`, `Instant createdAt`, `long assetCount`
- [x] 5.2 Add seven method signatures to `PhotoManagerFacade`:
  - `List<AlbumData> getAlbums(UUID userId)`
  - `AlbumData createAlbum(UUID userId, String name, String description)`
  - `AlbumData getAlbumSummary(Long albumId, UUID userId)`
  - `PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex)`
  - `AlbumData updateAlbum(Long albumId, UUID userId, String name, String description)`
  - `void deleteAlbum(Long albumId, UUID userId)`
  - `void addAssetsToAlbum(Long albumId, UUID userId, List<Long> assetIds)`
  - `void removeAssetsFromAlbum(Long albumId, UUID userId, List<Long> assetIds)`
- [x] 5.3 Inject `AlbumService albumService` and `UserRepository userRepository` into `PhotoManagerFacadeImpl`; implement all seven methods by delegating to `albumService`; map `Album` → `AlbumData` using a private helper `toAlbumData(Album)`

## 6. Backend — API layer

- [x] 6.1 Create request/response DTOs in `api/dto/`:
  - `AlbumSummaryDto.java` — fields `Long albumId`, `String name`, `String description`, `long assetCount`, `Instant createdAt`
  - `AlbumDto.java` — fields `Long albumId`, `String name`, `String description`, `Instant createdAt`, `PaginatedData<AssetDto> assets`
  - `CreateAlbumRequest.java` — record with `@NotBlank String name`, `String description`
  - `UpdateAlbumRequest.java` — record with `@NotBlank String name`, `String description`
  - `AlbumAssetIdsRequest.java` — record with `@NotEmpty List<Long> assetIds`
- [x] 6.2 Create `AlbumController.java` in `api/` annotated `@RestController @RequestMapping("/api/albums") @RequiredArgsConstructor`; inject `PhotoManagerFacade facade` and `UserRepository userRepository`; add private helper `resolveUserId()` that calls `SecurityContextHolder.getContext().getAuthentication().getName()` and looks up the UUID via `userRepository.findByUsername(...).map(User::getId).orElseThrow()`
- [x] 6.3 Add `GET /api/albums` → `listAlbums()`: call `facade.getAlbums(resolveUserId())`; map to `List<AlbumSummaryDto>`; return `200 OK`
- [x] 6.4 Add `POST /api/albums` → `createAlbum(@Valid @RequestBody CreateAlbumRequest)`: call `facade.createAlbum(userId, name, description)`; map to `AlbumSummaryDto`; return `201 Created`
- [x] 6.5 Add `GET /api/albums/{id}` → `getAlbum(@PathVariable Long id, @RequestParam(defaultValue="0") int page)`: call `facade.getAlbumSummary(id, userId)` and `facade.getAlbumAssets(id, userId, page)`; build `AlbumDto`; return `200 OK`; catch `AlbumNotFoundException` → `404`
- [x] 6.6 Add `PUT /api/albums/{id}` → `updateAlbum(@PathVariable Long id, @Valid @RequestBody UpdateAlbumRequest)`: call `facade.updateAlbum(id, userId, name, description)`; return `200 OK` with updated `AlbumSummaryDto`; catch `AlbumNotFoundException` → `404`
- [x] 6.7 Add `DELETE /api/albums/{id}` → `deleteAlbum(@PathVariable Long id)`: call `facade.deleteAlbum(id, userId)`; return `204 No Content`; catch `AlbumNotFoundException` → `404`
- [x] 6.8 Add `POST /api/albums/{id}/assets` → `addAssets(@PathVariable Long id, @Valid @RequestBody AlbumAssetIdsRequest)`: call `facade.addAssetsToAlbum(id, userId, assetIds)`; return `204 No Content`; catch `AlbumNotFoundException` → `404`
- [x] 6.9 Add `DELETE /api/albums/{id}/assets` → `removeAssets(@PathVariable Long id, @Valid @RequestBody AlbumAssetIdsRequest)`: call `facade.removeAssetsFromAlbum(id, userId, assetIds)`; return `204 No Content`; catch `AlbumNotFoundException` → `404`

## 7. Backend — Tests

- [x] 7.1 Create `AlbumControllerTest` (`@WebMvcTest(AlbumController.class)`): mock `PhotoManagerFacade` and `UserRepository`; stub `resolveUserId()` path; assert `GET /api/albums` returns `200` with `AlbumSummaryDto` list; assert `POST /api/albums` returns `201` with correct DTO; assert `GET /api/albums/{id}` returns `200` with `AlbumDto`; assert unknown album ID returns `404`
- [x] 7.2 Add `AlbumControllerTest` cases: `PUT /api/albums/{id}` returns `200`; `DELETE /api/albums/{id}` returns `204`; `POST /api/albums/{id}/assets` returns `204`; `DELETE /api/albums/{id}/assets` returns `204`
- [x] 7.3 Create `AlbumServiceIntegrationTest` (`@SpringBootTest`) extending `PostgresIntegrationTest`; seed a user and assets; create an album; add assets; verify `getAlbumAssets` returns correct page; remove an asset; verify count decrements; delete album; verify `findByIdAndUserId` returns empty
- [x] 7.4 Run `mvn test` and confirm all tests pass

## 8. Frontend — Models and Service

- [x] 8.1 Create `album.model.ts` in `frontend/src/app/core/models/` with interfaces:
  - `AlbumSummary { albumId: number; name: string; description: string | null; assetCount: number; createdAt: string; }`
  - `Album { albumId: number; name: string; description: string | null; createdAt: string; assets: PaginatedData<Asset>; }`
  - `CreateAlbumRequest { name: string; description?: string; }`
  - `UpdateAlbumRequest { name: string; description?: string; }`
  - `AlbumAssetIdsRequest { assetIds: number[]; }`
- [x] 8.2 Create `album.service.ts` in `frontend/src/app/core/services/` with methods:
  - `getAlbums(): Observable<AlbumSummary[]>` — `GET /api/albums`
  - `createAlbum(req: CreateAlbumRequest): Observable<AlbumSummary>` — `POST /api/albums`
  - `getAlbum(id: number, page?: number): Observable<Album>` — `GET /api/albums/:id`
  - `updateAlbum(id: number, req: UpdateAlbumRequest): Observable<AlbumSummary>` — `PUT /api/albums/:id`
  - `deleteAlbum(id: number): Observable<void>` — `DELETE /api/albums/:id`
  - `addAssets(albumId: number, assetIds: number[]): Observable<void>` — `POST /api/albums/:id/assets`
  - `removeAssets(albumId: number, assetIds: number[]): Observable<void>` — `DELETE /api/albums/:id/assets`

## 9. Frontend — AlbumsComponent

- [x] 9.1 Create `frontend/src/app/features/albums/` directory with `albums.component.ts`, `.html`, `.scss`, `.cy.ts`
- [x] 9.2 Declare standalone `AlbumsComponent` with `@Component({ selector: 'app-albums' })`; inject `AlbumService` and `MatSnackBar`; on `ngOnInit` call `albumService.getAlbums()` and store in `albums: AlbumSummary[]`
- [x] 9.3 Add `createAlbum(name: string)` method: call `albumService.createAlbum({ name })`; on success push result to `albums` and show snack bar
- [x] 9.4 Add `deleteAlbum(album: AlbumSummary)` method: call `albumService.deleteAlbum(album.albumId)`; on success remove from `albums` and show snack bar
- [x] 9.5 Build template: `MatToolbar` with title "Albums" and a `mat-icon-button` to open inline create form; `MatCard` grid `@for (album of albums)` showing name, assetCount, a `routerLink` to `/albums/:albumId`, and a delete button; import `MatCardModule`, `MatButtonModule`, `MatIconModule`, `MatToolbarModule`, `MatSnackBarModule`, `RouterLink`
- [x] 9.6 Write SCSS: card grid using `display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; padding: 16px`

## 10. Frontend — AlbumDetailComponent

- [x] 10.1 Create `frontend/src/app/features/albums/album-detail/` directory with `album-detail.component.ts`, `.html`, `.scss`, `.cy.ts`
- [x] 10.2 Declare standalone `AlbumDetailComponent`; inject `ActivatedRoute`, `AlbumService`, `MatSnackBar`, `Router`; on `ngOnInit` read `:id` from route params, call `albumService.getAlbum(id, 0)` and store in `album: Album | null`
- [x] 10.3 Add `loadPage(page: number)` method calling `albumService.getAlbum(id, page)` to refresh paginated assets; add `removeAsset(assetId: number)` that calls `albumService.removeAssets(albumId, [assetId])` then reloads
- [x] 10.4 Build template: `MatToolbar` with album name and a back button (`routerLink="/albums"`); `ThumbnailComponent` grid `@for (asset of album.assets.items)` with a "Remove" overlay button per asset; pagination controls showing `currentPage + 1 / totalPages`; import `ThumbnailComponent`, `MatToolbarModule`, `MatButtonModule`, `MatIconModule`, `RouterLink`
- [x] 10.5 Write SCSS: reuse `.thumbnail-grid` layout from gallery

## 11. Frontend — GalleryComponent wiring

- [x] 11.1 Inject `AlbumService` into `GalleryComponent`; add `userAlbums: AlbumSummary[] = []`; load albums on folder selection
- [x] 11.2 Add `addToAlbum(asset: Asset)` method: open a `MatDialog` (create `AddToAlbumDialogComponent` inline or as a separate file) that lists `userAlbums` with radio buttons and an input for a new album name; on confirm call `albumService.addAssets(selectedAlbumId, [asset.assetId])` or first `albumService.createAlbum(...)` then `addAssets`
- [x] 11.3 Add a context menu item "Add to album" to each thumbnail card in `gallery.component.html` triggering `addToAlbum(asset)`
- [x] 11.4 Create `AddToAlbumDialogComponent` in `features/gallery/` as a standalone `MatDialog` component; accept `MAT_DIALOG_DATA: { albums: AlbumSummary[] }`; render album list and new-album input; return `{ albumId: number | null, newAlbumName: string | null }` on confirm

## 12. Frontend — Routing and navigation

- [x] 12.1 Add two lazy routes to `app.routes.ts`:
  - `{ path: 'albums', loadComponent: () => import('./features/albums/albums.component').then(m => m.AlbumsComponent), canActivate: [authGuard] }`
  - `{ path: 'albums/:id', loadComponent: () => import('./features/albums/album-detail/album-detail.component').then(m => m.AlbumDetailComponent), canActivate: [authGuard] }`
- [x] 12.2 Add `<a mat-button routerLink="/albums">Albums</a>` to the nav section in `app.component.html` between the Duplicates and Admin links

## 13. Frontend — Tests

- [x] 13.1 In `albums.component.cy.ts`: mount with stubbed `AlbumService` returning two albums; assert two cards are rendered with correct names and asset counts
- [x] 13.2 Add test: clicking "Delete" on an album calls `albumService.deleteAlbum` with the correct ID and removes the card
- [x] 13.3 In `album-detail.component.cy.ts`: mount with stub returning an album with three assets; assert three `ThumbnailComponent` elements are rendered; assert album name appears in toolbar
- [x] 13.4 Add test: clicking "Remove" on an asset calls `albumService.removeAssets` with the correct `assetId`
- [x] 13.5 Run `npm test` and confirm all tests pass
