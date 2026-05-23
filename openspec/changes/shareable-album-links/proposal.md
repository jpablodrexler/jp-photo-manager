## Why

Albums (introduced in `virtual-albums`) are private to their owner. There is no way to share a curated photo collection with someone who does not have an account — a common real-world need: sending a holiday album to family, handing a client their wedding photos, or sharing a portfolio with a collaborator. Adding shareable album links closes this gap without requiring the recipient to register or authenticate.

## What Changes

- Add a Flyway migration `V14__add_shared_albums.sql` creating a new `shared_albums` table with columns `id BIGSERIAL PRIMARY KEY`, `album_id BIGINT NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE`, `token UUID NOT NULL UNIQUE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, and `expires_at TIMESTAMPTZ NULL`.
- Add a `SharedAlbumEntity` JPA entity mapping to the `shared_albums` table with a `@ManyToOne` FK to `AlbumEntity`.
- Add a `SharedAlbumRepository` port in `domain/port/out/` and an `SharedAlbumRepositoryImpl` persistence adapter.
- Add two new use-case interfaces in `domain/port/in/album/`: `ShareAlbumUseCase` and `GetSharedAlbumUseCase`.
- Add `ShareAlbumUseCaseImpl` and `GetSharedAlbumUseCaseImpl` in `application/usecase/album/`.
- Expose `POST /api/albums/{id}/share` (authenticated) — generates a UUID token, persists a `SharedAlbum` row, and returns a `ShareAlbumResponse` containing the token and the public URL.
- Expose `GET /api/albums/shared/{token}` (public, `permitAll()`) — resolves the token, validates expiry, and returns the album's paginated assets as a `SharedAlbumDto`.
- Update `SecurityConfig` to add `"/api/albums/shared/**"` to the `permitAll()` list.
- Add a `SharedAlbumComponent` in `features/shared-album/` for the Angular frontend, rendered at route `/s/:token` with no navigation bar and no auth guard.
- Add a "Share" action to `AlbumDetailComponent` that calls `POST /api/albums/{id}/share` and copies the public URL to the clipboard.
- Add `shared-album.model.ts` and extend `album.service.ts` with the two new API calls.

## Capabilities

### New Capabilities

- `shareable-album-links`: Album owners can generate a signed UUID token for any of their albums. Anyone with the token URL can view the album's assets in a minimal read-only page without logging in. Tokens have an optional expiry date; expired tokens return `410 Gone`. Owners can share multiple tokens per album and revoke a token by deleting it.

### Modified Capabilities

- `virtual-albums`: `AlbumDetailComponent` gains a "Share" toolbar action that invokes the share endpoint and copies the resulting URL to the clipboard.

## Impact

- **`V14__add_shared_albums.sql`** *(new)*: creates `shared_albums` (5 columns: PK, album_id FK with `ON DELETE CASCADE`, token UUID `NOT NULL UNIQUE`, `created_at`, `expires_at` nullable); adds index `ix_shared_albums_token ON shared_albums(token)`.
- **`SharedAlbumEntity.java`** *(new)*: `@Entity @Table(name = "shared_albums")` with `@ManyToOne(fetch = LAZY)` to `AlbumEntity`; Lombok `@Data @NoArgsConstructor`.
- **`JpaSharedAlbumRepository.java`** *(new)*: `JpaRepository<SharedAlbumEntity, Long>` with `Optional<SharedAlbumEntity> findByToken(UUID token)`.
- **`SharedAlbumRepository.java`** *(new)*: domain out-port interface with `save`, `findByToken`, and `delete` methods.
- **`SharedAlbumRepositoryImpl.java`** *(new)*: `@Repository` persistence adapter implementing `SharedAlbumRepository`.
- **`SharedAlbumMapper.java`** *(new)*: MapStruct `@Mapper` for `SharedAlbumEntity` ↔ domain model.
- **`ShareAlbumUseCase.java`** *(new)*: single-method domain in-port `ShareAlbumResult share(Long albumId, UUID userId, Instant expiresAt)`.
- **`GetSharedAlbumUseCase.java`** *(new)*: single-method domain in-port `SharedAlbumView getByToken(UUID token)`.
- **`ShareAlbumUseCaseImpl.java`** *(new)*: `@Service @Transactional`; looks up the album (throws `AlbumNotFoundException` if not owned by user), generates `UUID.randomUUID()`, persists `SharedAlbum`, returns `ShareAlbumResult`.
- **`GetSharedAlbumUseCaseImpl.java`** *(new)*: `@Service @Transactional(readOnly = true)`; resolves token, validates expiry (throws `SharedAlbumExpiredException` if `expiresAt` is set and in the past), returns album assets as `SharedAlbumView`.
- **`SharedAlbumController.java`** *(new)*: `@RestController`; `POST /api/albums/{id}/share` (authenticated) delegates to `ShareAlbumUseCase`; `GET /api/albums/shared/{token}` (`permitAll`) delegates to `GetSharedAlbumUseCase`.
- **`ShareAlbumRequest.java`** *(new)*: HTTP request DTO with optional `expiresAt: Instant`.
- **`ShareAlbumResponse.java`** *(new)*: HTTP response DTO with `token: UUID` and `url: String`.
- **`SharedAlbumDto.java`** *(new)*: public response DTO — album `name`, `description`, and `PaginatedResult<AssetDto> assets`.
- **`SecurityConfig.java`**: adds `"/api/albums/shared/**"` to the `permitAll()` matcher list.
- **`shared-album.model.ts`** *(new)*: TypeScript interfaces `ShareAlbumRequest`, `ShareAlbumResponse`, `SharedAlbumView`.
- **`album.service.ts`**: two new methods — `shareAlbum(id, req)` and `getSharedAlbum(token, page)`.
- **`SharedAlbumComponent`** *(new)*: standalone component at `/s/:token`; minimal layout — no `app.component` shell nav bar; renders album name, description, and `ThumbnailComponent` grid; shows "Link has expired" message for 410 responses.
- **`app.routes.ts`**: new lazy route `{ path: 's/:token', ... }` with no `canActivate` guard.
- **`AlbumDetailComponent`**: "Share" toolbar button that opens a small dialog or inline action calling `albumService.shareAlbum(id, {})` and writing the returned URL to the clipboard.
- **No breaking API changes** — all existing endpoints are unchanged.
