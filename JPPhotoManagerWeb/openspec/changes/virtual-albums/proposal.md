## Why

The application currently organises photos exclusively by their on-disk folder hierarchy: a user browses photos by selecting a catalogued folder in the `FolderNavComponent` tree. There is no mechanism for grouping assets across folder boundaries without physically moving files. Photographers commonly want to assemble a curated set of images from several shoot locations, dates, or drives — a wedding gallery that draws from both the ceremony and reception folders, or a "best-of" collection spanning multiple years. Without virtual albums, users must either duplicate files into a dedicated folder (wasting storage) or rely on external tools. Virtual albums solve this by associating assets with named collections that exist only in the database, leaving files exactly where they are on disk.

## What Changes

- Add a Flyway migration `V8__add_albums.sql` creating two new tables: `albums` (owned by a user) and `album_assets` (the M:N join between albums and assets).
- Add two JPA entities: `Album` (`@ManyToOne` to `User`, `@ManyToMany` to `Asset` via `album_assets`) and the join table is managed by the `Album` entity.
- Add `AlbumRepository` with `findByUser_Id` and `findByAlbumIdAndUser_Id` derived query methods.
- Add an `AlbumService` interface in `domain/service/` and `AlbumServiceImpl` in `infrastructure/service/` implementing create, read, update, delete, and asset-membership operations scoped to the authenticated user.
- Add seven facade methods to `PhotoManagerFacade` and `PhotoManagerFacadeImpl`: `getAlbums`, `createAlbum`, `getAlbum`, `updateAlbum`, `deleteAlbum`, `addAssetsToAlbum`, `removeAssetsFromAlbum`.
- Add `AlbumController` in `api/` exposing seven REST endpoints under `/api/albums`, all requiring authentication.
- Add request/response DTOs in `api/dto/`: `AlbumDto`, `AlbumSummaryDto`, `CreateAlbumRequest`, `UpdateAlbumRequest`, `AlbumAssetIdsRequest`.
- Add an `application/dto/AlbumData` record used across the facade boundary.
- Add a `GalleryComponent` context menu "Add to album" option that opens a dialog for selecting an existing album or creating a new one.
- Add `AlbumsComponent` (list of the user's albums, route `/albums`) and `AlbumDetailComponent` (paginated thumbnail grid of album assets, route `/albums/:id`) as new features in the frontend.
- Add `album.model.ts` TypeScript interface and `album.service.ts` Angular service in `core/`.
- Wire the new `/albums` and `/albums/:id` routes into `app.routes.ts` with `authGuard`.
- Add an "Albums" nav link to `app.component.html`.

## Capabilities

### New Capabilities

- `virtual-albums`: Create and manage named photo collections that group assets from any catalogued folder without moving files on disk. Users can create albums, rename them, add or remove individual assets, browse album contents in a thumbnail grid, and delete albums without affecting the underlying files.

### Modified Capabilities

_(none — no existing spec files are affected)_

## Impact

- **`V8__add_albums.sql`** *(new)*: creates `albums` (5 columns: PK, name, description, created_at, FK to users) and `album_assets` (3 columns: album_id FK, asset_id FK, added_at) with a composite PK and `ON DELETE CASCADE` constraints on both FKs.
- **`Album.java`** *(new)*: JPA `@Entity` with `@ManyToOne(fetch = LAZY)` to `User`, `@ManyToMany` to `Asset` using `@JoinTable(name = "album_assets")`, Lombok `@Data @NoArgsConstructor`.
- **`AlbumRepository.java`** *(new)*: `JpaRepository<Album, Long>` with `List<Album> findByUser_Id(UUID userId)` and `Optional<Album> findByAlbumIdAndUser_Id(Long albumId, UUID userId)`.
- **`AlbumService.java`** *(new)*: domain service interface declaring all album operations.
- **`AlbumServiceImpl.java`** *(new)*: `@Service` implementation; all write operations annotated `@Transactional`, reads annotated `@Transactional(readOnly = true)`.
- **`AlbumData.java`** *(new)*: application-layer record used as the return type for album queries through the facade; contains album metadata and a `PaginatedData<Asset>` for assets when detail is requested.
- **`PhotoManagerFacade.java`**: seven new method signatures.
- **`PhotoManagerFacadeImpl.java`**: delegates all seven new methods to `AlbumService`.
- **`AlbumDto.java`** *(new)*: API response DTO with album metadata and paginated asset list (for detail endpoint).
- **`AlbumSummaryDto.java`** *(new)*: lightweight list-item DTO (id, name, description, assetCount, createdAt).
- **`CreateAlbumRequest.java`** *(new)*: request body for `POST /api/albums` (`name`, optional `description`).
- **`UpdateAlbumRequest.java`** *(new)*: request body for `PUT /api/albums/{id}` (`name`, optional `description`).
- **`AlbumAssetIdsRequest.java`** *(new)*: request body for `POST /api/albums/{id}/assets` and `DELETE /api/albums/{id}/assets` (`assetIds: List<Long>`).
- **`AlbumController.java`** *(new)*: `@RestController` at `/api/albums`; extracts the authenticated user from `SecurityContextHolder`, delegates to facade, maps results to DTOs.
- **`album.model.ts`** *(new)*: TypeScript interfaces `Album`, `AlbumSummary`, `CreateAlbumRequest`, `UpdateAlbumRequest`.
- **`album.service.ts`** *(new)*: Angular service wrapping all seven album API calls.
- **`AlbumsComponent`** *(new)*: standalone component at `/albums` showing a Material card grid of user albums with create/delete actions.
- **`AlbumDetailComponent`** *(new)*: standalone component at `/albums/:id` showing a paginated thumbnail grid (reusing `ThumbnailComponent`) plus "Remove from album" per-asset action.
- **`app.routes.ts`**: two new lazy routes for `/albums` and `/albums/:id`, both protected by `authGuard`.
- **`app.component.html`**: new Albums nav link between Duplicates and Users.
- **`gallery.component.ts`** and **`gallery.component.html`**: "Add to album" context menu item on the `ThumbnailComponent`; opens a `MatDialog` to pick or create an album.
- **No breaking API changes** — all existing endpoints are unchanged.
