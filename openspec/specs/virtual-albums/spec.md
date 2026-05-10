# virtual-albums

Specifies how authenticated users create and manage named photo collections that group assets from any catalogued folder without moving files on disk.

---

### Requirement: Authenticated users can create, list, rename, and delete their own albums

The system SHALL expose CRUD endpoints under `/api/albums`. Each album SHALL be scoped to the authenticated user; no user SHALL be able to read or modify another user's albums. `GET /api/albums` SHALL return a summary list; `POST /api/albums` SHALL create a new album with a required `name` and optional `description`; `PUT /api/albums/{id}` SHALL update `name` and `description`; `DELETE /api/albums/{id}` SHALL delete the album and all its membership records. Albums SHALL be stored in the database only — no filesystem changes occur on create, update, or delete.

#### Scenario: User lists their albums

- **GIVEN** the authenticated user has two albums: "Wedding" and "Best of 2025"
- **WHEN** the client calls `GET /api/albums`
- **THEN** the response is `200 OK` with a JSON array of two `AlbumSummaryDto` objects, each containing `albumId`, `name`, `description`, `assetCount`, and `createdAt`; no albums belonging to other users are included

#### Scenario: User creates an album

- **GIVEN** the user has no albums
- **WHEN** the client calls `POST /api/albums` with body `{ "name": "Vacation 2025" }`
- **THEN** the response is `201 Created` with an `AlbumSummaryDto` containing the new `albumId` and `assetCount: 0`; a subsequent `GET /api/albums` includes the new album

#### Scenario: User renames an album

- **GIVEN** an album with `albumId = 7` and `name = "Old Name"` exists for the user
- **WHEN** the client calls `PUT /api/albums/7` with body `{ "name": "New Name" }`
- **THEN** the response is `200 OK` with the updated `AlbumSummaryDto`; `GET /api/albums` shows the new name

#### Scenario: User deletes an album

- **GIVEN** an album with `albumId = 3` exists and contains five assets
- **WHEN** the client calls `DELETE /api/albums/3`
- **THEN** the response is `204 No Content`; `GET /api/albums` no longer includes album 3; the five assets still exist in the catalog and are unaffected

#### Scenario: User accesses another user's album

- **GIVEN** album with `albumId = 10` belongs to user B
- **WHEN** user A calls `GET /api/albums/10`
- **THEN** the response is `404 Not Found`

---

### Requirement: Assets can be added to and removed from albums

The system SHALL expose `POST /api/albums/{id}/assets` accepting `{ "assetIds": [...] }` to add one or more assets to an album, and `DELETE /api/albums/{id}/assets` with the same body shape to remove them. Adding an asset that is already in the album SHALL be idempotent. The endpoint SHALL return `404 Not Found` if the album does not belong to the authenticated user.

#### Scenario: Assets are added to an album

- **GIVEN** an empty album with `albumId = 5` and three catalogued assets with IDs 101, 102, 103
- **WHEN** the client calls `POST /api/albums/5/assets` with body `{ "assetIds": [101, 102, 103] }`
- **THEN** the response is `204 No Content`; a subsequent `GET /api/albums/5` returns `assetCount: 3` and the assets appear in the paginated grid

#### Scenario: Adding a duplicate asset is idempotent

- **GIVEN** asset 101 is already in album 5
- **WHEN** the client calls `POST /api/albums/5/assets` with body `{ "assetIds": [101] }`
- **THEN** the response is `204 No Content`; `GET /api/albums/5` still shows `assetCount: 3` (no duplicate row created)

#### Scenario: Assets are removed from an album

- **GIVEN** album 5 contains assets 101, 102, 103
- **WHEN** the client calls `DELETE /api/albums/5/assets` with body `{ "assetIds": [102] }`
- **THEN** the response is `204 No Content`; a subsequent `GET /api/albums/5` returns `assetCount: 2`; asset 102 still exists in the catalog

#### Scenario: Adding assets to an unknown album

- **GIVEN** album 999 does not exist or belongs to another user
- **WHEN** the client calls `POST /api/albums/999/assets`
- **THEN** the response is `404 Not Found`

---

### Requirement: Album contents are browsable in a paginated thumbnail grid

`GET /api/albums/{id}` SHALL return an `AlbumDto` containing album metadata and a paginated list of `AssetDto` items (page size 50). The response SHALL include `pageIndex`, `totalPages`, and `totalItems`. The frontend `AlbumDetailComponent` SHALL render the asset thumbnails using the same `ThumbnailComponent` used in the gallery and SHALL show a pagination control when `totalPages > 1`.

#### Scenario: Browsing album page 0

- **GIVEN** album 5 contains 75 assets
- **WHEN** the client calls `GET /api/albums/5?page=0`
- **THEN** the response is `200 OK` with `assets.items` containing 50 `AssetDto` objects, `assets.totalPages: 2`, `assets.totalItems: 75`

#### Scenario: Browsing album page 1

- **GIVEN** album 5 contains 75 assets and page 0 has been loaded
- **WHEN** the client calls `GET /api/albums/5?page=1`
- **THEN** the response is `200 OK` with `assets.items` containing 25 `AssetDto` objects, `assets.pageIndex: 1`

---

### Requirement: Deleting a catalogued asset removes it from all albums

When an asset is deleted from the catalog (via `DELETE /api/assets`), all `album_assets` rows referencing that asset SHALL be removed automatically. The `album_assets.asset_id` column SHALL have `ON DELETE CASCADE` referencing `assets.asset_id` so no orphaned membership rows remain.

#### Scenario: Asset deletion cascades to albums

- **GIVEN** asset 101 is a member of albums 5 and 6
- **WHEN** `DELETE /api/assets?assetIds=101` is called
- **THEN** asset 101 is removed from the catalog; both album 5 and album 6 show `assetCount` reduced by 1; no `album_assets` row for asset 101 remains

---

### Requirement: The frontend shows an Albums section in the navigation

The `app.component.html` navigation bar SHALL include an "Albums" link between the Duplicates and Admin links. Navigating to `/albums` SHALL display the `AlbumsComponent` listing the user's albums as Material cards. Each card SHALL show the album name, asset count, and a delete button. A creation control SHALL allow entering a new album name inline.

#### Scenario: User navigates to Albums

- **GIVEN** the user is authenticated and has two albums
- **WHEN** the user clicks the "Albums" nav link
- **THEN** the `/albums` route loads `AlbumsComponent`; two Material cards appear, each showing the album name and asset count

#### Scenario: User creates an album from the Albums page

- **GIVEN** the Albums page is visible with no albums
- **WHEN** the user enters "Summer 2025" in the create-album input and confirms
- **THEN** `POST /api/albums` is called; a new card appears for "Summer 2025" with `0 photos`

---

### Requirement: Assets can be added to albums from the gallery

The `GalleryComponent` thumbnail grid SHALL include an "Add to album" action per asset. Activating it SHALL open a dialog listing the user's albums with the option to create a new one inline. Confirming SHALL call `POST /api/albums/{id}/assets`. The gallery grid SHALL NOT reload after the action — album membership is a silent background operation from the gallery's perspective.

#### Scenario: User adds a gallery asset to an existing album

- **GIVEN** the user has album "Favourites" (albumId=2) and is viewing the gallery
- **WHEN** the user activates "Add to album" on asset 55 and selects "Favourites"
- **THEN** `POST /api/albums/2/assets` is called with `{ "assetIds": [55] }`; a success snack bar appears; the gallery grid is unchanged

#### Scenario: User creates a new album from the gallery dialog

- **GIVEN** the user has no albums
- **WHEN** the user activates "Add to album" on asset 55, enters "New Album" in the dialog, and confirms
- **THEN** `POST /api/albums` is called first to create the album; then `POST /api/albums/{newId}/assets` is called with `{ "assetIds": [55] }`; both succeed with 201/204

---

### Requirement: Unauthenticated requests to album endpoints are rejected

All `/api/albums/**` endpoints SHALL require a valid JWT cookie. Requests without a valid session SHALL receive `401 Unauthorized`.

#### Scenario: Unauthenticated album list request

- **GIVEN** no valid JWT cookie is present
- **WHEN** a client calls `GET /api/albums`
- **THEN** the response is `401 Unauthorized`
