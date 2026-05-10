## Context

The application currently organises photos exclusively by their on-disk folder hierarchy. There is no way to group assets from different folders without physically moving files. The album feature introduces a pure-database layer of organisation: `albums` and `album_assets` tables that associate assets with named, user-owned collections without touching the filesystem.

`User` is already a JPA entity with a UUID PK (`@GeneratedValue(strategy = GenerationType.UUID)`) and `@Table(name = "users")`. `Asset` has a `Long` PK (`asset_id`) with `@GeneratedValue(strategy = GenerationType.IDENTITY)`. Both are available as targets for the new `@ManyToMany` relationship.

`UserRepository.findByUsername(String)` and `SecurityContextHolder.getContext().getAuthentication().getName()` give controllers the currently authenticated user's username, from which the `User` entity can be looked up. Controllers in this project delegate to `PhotoManagerFacade`, which delegates to domain services.

The frontend uses Angular 19 standalone components, lazy-loaded routes, and Angular Material. `ThumbnailComponent` is a reusable card already used in the gallery — it can be reused in `AlbumDetailComponent`.

## Goals / Non-Goals

**Goals:**

- Let authenticated users create, rename, and delete named albums.
- Let users add any catalogued asset to one or more albums and remove it.
- Browse album contents in a paginated thumbnail grid.
- Keep albums user-scoped: each user sees only their own albums.
- No filesystem changes — albums are pure-DB metadata.

**Non-Goals:**

- Sharing albums between users.
- Album ordering beyond creation order.
- Album thumbnails / cover images (out of scope for this iteration).
- Moving or copying files to a new folder when adding to an album.

## Decisions

### 1. User-scoped albums via `@ManyToOne` to `User`

**Decision:** `Album` has a `@ManyToOne(fetch = LAZY)` relationship to `User` (FK column `user_id`). All repository queries and facade methods take `userId: UUID` as a parameter to scope results to the authenticated user. The controller resolves the user from `SecurityContextHolder` and passes the UUID down — it does not pass `User` objects through the facade boundary.

**Rationale:** Scoping by `userId` UUID keeps the facade and service interfaces independent of Spring Security types. The controller is the only layer that touches the security context; downstream layers are testable without security infrastructure.

### 2. Many-to-many relationship managed by `Album` entity

**Decision:** `Album` owns the `@ManyToMany` relationship to `Asset` via `@JoinTable(name = "album_assets", joinColumns = @JoinColumn(name = "album_id"), inverseJoinColumns = @JoinColumn(name = "asset_id"))`. The `Asset` entity is not modified — it does not have an inverse `@ManyToMany` collection.

**Rationale:** Keeping `Asset` unmodified avoids loading album collections when assets are fetched for the gallery, preventing accidental N+1 queries. Album membership is always queried from the `Album` side. The join table has `ON DELETE CASCADE` on both FKs so deleting an album or an asset automatically removes orphaned rows.

### 3. Dedicated `AlbumService` interface + `AlbumServiceImpl` — not added to `CatalogAssetsService`

**Decision:** Album CRUD and membership operations are encapsulated in a new `AlbumService` interface in `domain/service/` and `AlbumServiceImpl` in `infrastructure/service/`. The facade delegates to `AlbumService`, not to any existing service.

**Rationale:** Album operations have no dependency on file I/O or cataloging. Mixing them into `CatalogAssetsService` or `PhotoManagerFacadeImpl` directly would violate the single-responsibility principle and complicate testing.

### 4. Paginated asset list in album detail — reuse existing `PaginatedData<Asset>`

**Decision:** `AlbumService.getAlbumAssets(albumId, userId, pageIndex)` queries the `album_assets` join table with a `Page<Asset>` query and returns `PaginatedData<Asset>`. The controller maps it to `PaginatedData<AssetDto>` using the existing `toDto(Asset)` helper extracted from `AssetController` into a shared utility or duplicated per controller.

**Rationale:** `PaginatedData<T>` is already the standard paginated response shape used by `GET /api/assets`. Reusing it means the frontend `AlbumDetailComponent` can use the same `ThumbnailComponent` and pagination logic as the gallery.

### 5. `AlbumsComponent` and `AlbumDetailComponent` in `features/albums/`

**Decision:** Two new Angular standalone components: `AlbumsComponent` at `/albums` (list of user's albums) and `AlbumDetailComponent` at `/albums/:id` (paginated asset grid for one album). Both are placed in `frontend/src/app/features/albums/`. Routes are lazy-loaded and protected by `authGuard`.

**Rationale:** Albums are a distinct application feature, not a gallery sub-mode. A separate feature folder and separate routes keep `GalleryComponent` focused on folder-based browsing and allow the albums list and detail pages to evolve independently.

### 6. "Add to album" dialog in `GalleryComponent` — `MatDialog` with inline album creation

**Decision:** Each thumbnail's context menu (right-click or overflow menu) includes an "Add to album" action that opens a `MatDialog`. The dialog lists the user's existing albums (fetched on open) and has a text input for creating a new album inline. Submitting the dialog calls either `albumService.addAssetsToAlbum` or first `albumService.createAlbum` then `addAssetsToAlbum`.

**Rationale:** Inline creation avoids forcing the user to navigate away to the `/albums` list just to create a new album before adding photos. The dialog is a focused interaction that does not affect the gallery grid state.

## Data Flow

```
GET /albums
  → AlbumController.listAlbums()
    → resolve username from SecurityContextHolder → userRepository.findByUsername()
    → facade.getAlbums(userId)
      → albumService.findByUserId(userId)
    → List<AlbumSummaryDto>

POST /albums
  → AlbumController.createAlbum(@RequestBody CreateAlbumRequest)
    → facade.createAlbum(userId, name, description)
      → albumService.createAlbum(userId, name, description)
        → new Album(user, name, description) → albumRepository.save()
    → 201 Created {AlbumSummaryDto}

GET /albums/{id}
  → AlbumController.getAlbum(@PathVariable Long id, @RequestParam int page)
    → facade.getAlbum(id, userId, page)
      → albumService.getAlbum(id, userId)          → 404 if not owned by user
      → albumService.getAlbumAssets(id, userId, page) → PaginatedData<Asset>
    → AlbumDto {metadata + PaginatedData<AssetDto>}

PUT /albums/{id}
  → AlbumController.updateAlbum(@PathVariable Long id, @RequestBody UpdateAlbumRequest)
    → facade.updateAlbum(id, userId, name, description)
      → albumService.updateAlbum(id, userId, name, description) → 404 if not owned

DELETE /albums/{id}
  → AlbumController.deleteAlbum(@PathVariable Long id)
    → facade.deleteAlbum(id, userId)
      → albumService.deleteAlbum(id, userId)       → 404 if not owned
    → 204 No Content

POST /albums/{id}/assets
  → AlbumController.addAssets(@PathVariable Long id, @RequestBody AlbumAssetIdsRequest)
    → facade.addAssetsToAlbum(id, userId, assetIds)
      → albumService.addAssets(id, userId, assetIds)
    → 204 No Content

DELETE /albums/{id}/assets
  → AlbumController.removeAssets(@PathVariable Long id, @RequestBody AlbumAssetIdsRequest)
    → facade.removeAssetsFromAlbum(id, userId, assetIds)
      → albumService.removeAssets(id, userId, assetIds)
    → 204 No Content
```

## File Change List

**New files:**

- `backend/.../db/migration/V8__add_albums.sql` — creates `albums` and `album_assets` tables
- `backend/.../domain/entity/Album.java` — JPA entity; `@ManyToOne` User, `@ManyToMany` Asset
- `backend/.../domain/repository/AlbumRepository.java` — `findByUser_Id`, `findByIdAndUser_Id`
- `backend/.../domain/service/AlbumService.java` — domain service interface
- `backend/.../infrastructure/service/AlbumServiceImpl.java` — `@Service` implementation
- `backend/.../application/dto/AlbumData.java` — application-layer record (album metadata)
- `backend/.../api/dto/AlbumSummaryDto.java` — list-item DTO (id, name, description, assetCount, createdAt)
- `backend/.../api/dto/AlbumDto.java` — detail DTO (metadata + `PaginatedData<AssetDto>`)
- `backend/.../api/dto/CreateAlbumRequest.java` — `@NotBlank name`, optional `description`
- `backend/.../api/dto/UpdateAlbumRequest.java` — `@NotBlank name`, optional `description`
- `backend/.../api/dto/AlbumAssetIdsRequest.java` — `List<Long> assetIds`
- `backend/.../api/AlbumController.java` — `@RestController` at `/api/albums`
- `backend/.../test/.../AlbumControllerTest.java` — `@WebMvcTest` for album endpoints
- `backend/.../test/.../AlbumServiceIntegrationTest.java` — `@SpringBootTest` integration test
- `frontend/src/app/core/models/album.model.ts` — TypeScript interfaces
- `frontend/src/app/core/services/album.service.ts` — Angular service
- `frontend/src/app/features/albums/albums.component.ts/html/scss/cy.ts`
- `frontend/src/app/features/albums/album-detail/album-detail.component.ts/html/scss/cy.ts`

**Modified files:**

- `backend/.../application/PhotoManagerFacade.java` — seven new method signatures
- `backend/.../application/PhotoManagerFacadeImpl.java` — delegate to `AlbumService`; inject `UserRepository`
- `frontend/src/app/app.routes.ts` — add `/albums` and `/albums/:id` lazy routes
- `frontend/src/app/app.component.html` — add "Albums" nav link
- `frontend/src/app/features/gallery/gallery.component.ts` — "Add to album" context menu + dialog
- `frontend/src/app/features/gallery/gallery.component.html` — add context menu item to thumbnail card
