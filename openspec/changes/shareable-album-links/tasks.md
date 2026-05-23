## 1. Backend — Database migration

- [ ] 1.1 Create `V14__add_shared_albums.sql` in `backend/src/main/resources/db/migration/`; create table `shared_albums` with columns `id BIGSERIAL PRIMARY KEY`, `album_id BIGINT NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE`, `token UUID NOT NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `expires_at TIMESTAMPTZ NULL`; add constraint `CONSTRAINT uq_shared_albums_token UNIQUE (token)`; add index `CREATE INDEX ix_shared_albums_token ON shared_albums(token)`

## 2. Backend — Entity, JPA Repository, and Mapper

- [ ] 2.1 Create `SharedAlbumEntity.java` in `infrastructure/persistence/entity/`; annotate `@Entity @Table(name = "shared_albums") @Data @NoArgsConstructor`; fields: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`, `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "album_id", nullable = false) AlbumEntity album`, `@Column(nullable = false, unique = true) UUID token`, `@Column(nullable = false, updatable = false) Instant createdAt`, `@Column Instant expiresAt`
- [ ] 2.2 Create `JpaSharedAlbumRepository.java` in `infrastructure/persistence/jpa/` extending `JpaRepository<SharedAlbumEntity, Long>`; add `Optional<SharedAlbumEntity> findByToken(UUID token)`
- [ ] 2.3 Create `SharedAlbumMapper.java` in `infrastructure/persistence/mapper/` as a MapStruct `@Mapper(componentModel = "spring")` interface; map `SharedAlbumEntity` ↔ `SharedAlbum` domain model (defined in step 3.1)

## 3. Backend — Domain model and out-port

- [ ] 3.1 Create `SharedAlbum.java` in `domain/model/` as a Java record with fields `Long id`, `Long albumId`, `UUID token`, `Instant createdAt`, `Instant expiresAt`
- [ ] 3.2 Create `SharedAlbumRepository.java` in `domain/port/out/` with methods:
  - `SharedAlbum save(SharedAlbum sharedAlbum)`
  - `Optional<SharedAlbum> findByToken(UUID token)`
  - `void delete(Long id)`
- [ ] 3.3 Create `SharedAlbumRepositoryImpl.java` in `infrastructure/persistence/adapter/` annotated `@Repository @RequiredArgsConstructor`; inject `JpaSharedAlbumRepository` and `SharedAlbumMapper`; implement all three methods

## 4. Backend — Domain in-ports (use-case interfaces)

- [ ] 4.1 Create `ShareAlbumUseCase.java` in `domain/port/in/album/`; define record `ShareAlbumResult(UUID token, String url)` as a nested type; single method `ShareAlbumResult share(Long albumId, UUID userId, Instant expiresAt)`
- [ ] 4.2 Create `GetSharedAlbumUseCase.java` in `domain/port/in/album/`; single method `SharedAlbumView getByToken(UUID token, int pageIndex)`; define record `SharedAlbumView(String albumName, String albumDescription, PaginatedResult<Asset> assets)` as a nested type (import `PaginatedResult` from `application/dto/`)

## 5. Backend — Application use-case implementations

- [ ] 5.1 Create `ShareAlbumUseCaseImpl.java` in `application/usecase/album/`; annotate `@Service @Transactional @RequiredArgsConstructor @Slf4j`; inject `AlbumRepository albumRepository` and `SharedAlbumRepository sharedAlbumRepository`
- [ ] 5.2 Implement `share(albumId, userId, expiresAt)`: call `albumRepository.findByIdAndUserId(albumId, userId)` — throw `AlbumNotFoundException(albumId)` if empty; generate `UUID token = UUID.randomUUID()`; build `SharedAlbum` record with `id = null`, `albumId`, `token`, `createdAt = Instant.now()`, `expiresAt`; call `sharedAlbumRepository.save(sharedAlbum)`; return `ShareAlbumResult(token, "/s/" + token)`
- [ ] 5.3 Create `GetSharedAlbumUseCaseImpl.java` in `application/usecase/album/`; annotate `@Service @Transactional(readOnly = true) @RequiredArgsConstructor @Slf4j`; inject `SharedAlbumRepository sharedAlbumRepository`, `AlbumRepository albumRepository`, `AssetRepository assetRepository`
- [ ] 5.4 Implement `getByToken(token, pageIndex)`: call `sharedAlbumRepository.findByToken(token)` — throw `SharedAlbumNotFoundException(token)` if empty; if `sharedAlbum.expiresAt() != null && sharedAlbum.expiresAt().isBefore(Instant.now())` throw `SharedAlbumExpiredException(token)`; retrieve album via `albumRepository.findById(sharedAlbum.albumId())` (throw `AlbumNotFoundException` if missing); fetch `PaginatedResult<Asset>` of album assets scoped to the album; return `SharedAlbumView(album.name(), album.description(), paginatedAssets)`

## 6. Backend — Exceptions

- [ ] 6.1 Create `SharedAlbumNotFoundException.java` in `infrastructure/web/exception/` as a `RuntimeException` subclass with constructor `SharedAlbumNotFoundException(UUID token)` setting message `"Shared album not found for token: " + token`
- [ ] 6.2 Create `SharedAlbumExpiredException.java` in `infrastructure/web/exception/` as a `RuntimeException` subclass with constructor `SharedAlbumExpiredException(UUID token)` setting message `"Shared album link has expired: " + token`
- [ ] 6.3 Add exception handler methods to `GlobalExceptionHandler`: map `SharedAlbumNotFoundException` → `404 Not Found`; map `SharedAlbumExpiredException` → `410 Gone`

## 7. Backend — HTTP layer

- [ ] 7.1 Create request/response DTOs in `infrastructure/web/dto/`:
  - `ShareAlbumRequest.java` — record with `@JsonInclude(NON_NULL) Instant expiresAt`
  - `ShareAlbumResponse.java` — record with `UUID token`, `String url`
  - `SharedAlbumDto.java` — record with `String name`, `String description`, `PaginatedResult<AssetDto> assets`
- [ ] 7.2 Create `SharedAlbumController.java` in `infrastructure/web/controller/` annotated `@RestController @RequiredArgsConstructor @Slf4j`; inject `ShareAlbumUseCase shareAlbumUseCase`, `GetSharedAlbumUseCase getSharedAlbumUseCase`, and `UserRepository userRepository`; add private helper `resolveUserId()` reading username from `SecurityContextHolder` and looking up the user UUID
- [ ] 7.3 Add `POST /api/albums/{id}/share` → `shareAlbum(@PathVariable Long id, @RequestBody(required = false) ShareAlbumRequest request)`: call `resolveUserId()`; call `shareAlbumUseCase.share(id, userId, request != null ? request.expiresAt() : null)`; return `ResponseEntity.status(201).body(new ShareAlbumResponse(result.token(), result.url()))`
- [ ] 7.4 Add `GET /api/albums/shared/{token}` → `getSharedAlbum(@PathVariable UUID token, @RequestParam(defaultValue = "0") int page)`: call `getSharedAlbumUseCase.getByToken(token, page)`; map `SharedAlbumView` to `SharedAlbumDto`; return `200 OK`
- [ ] 7.5 Update `SecurityConfig.securityFilterChain()`: add `"/api/albums/shared/**"` to the `.requestMatchers(...).permitAll()` list alongside `"/api/auth/login"`, `"/api/auth/logout"`, and `"/api/auth/refresh"`

## 8. Backend — Tests

- [ ] 8.1 Create `ShareAlbumUseCaseImplTest` (`@ExtendWith(MockitoExtension.class)`): mock `AlbumRepository` and `SharedAlbumRepository`; test `share` with valid owned album — verify `sharedAlbumRepository.save()` is called, result `token` is non-null, `url` starts with `/s/`; test `share` when `findByIdAndUserId` returns empty — verify `AlbumNotFoundException` is thrown
- [ ] 8.2 Create `GetSharedAlbumUseCaseImplTest` (`@ExtendWith(MockitoExtension.class)`): mock `SharedAlbumRepository`, `AlbumRepository`, `AssetRepository`; test `getByToken` with valid non-expired token returns correct `SharedAlbumView`; test `getByToken` with unknown token throws `SharedAlbumNotFoundException`; test `getByToken` with expired token throws `SharedAlbumExpiredException`; test `getByToken` with null `expiresAt` succeeds (never expires)
- [ ] 8.3 Create `SharedAlbumControllerTest` (`@WebMvcTest(SharedAlbumController.class)`): mock `ShareAlbumUseCase`, `GetSharedAlbumUseCase`, `UserRepository`; assert `POST /api/albums/7/share` (authenticated) returns `201` with `ShareAlbumResponse`; assert `GET /api/albums/shared/{token}` (no auth cookie) returns `200` with `SharedAlbumDto`; assert `GET /api/albums/shared/{token}` when `GetSharedAlbumUseCase` throws `SharedAlbumNotFoundException` returns `404`; assert `GET /api/albums/shared/{token}` when `GetSharedAlbumUseCase` throws `SharedAlbumExpiredException` returns `410`
- [ ] 8.4 Create `SharedAlbumIntegrationTest` (`@SpringBootTest`) extending `PostgresIntegrationTest`; seed a user, an album, and several assets; call `ShareAlbumUseCaseImpl.share()` to generate a token; call `GetSharedAlbumUseCaseImpl.getByToken()` and assert the correct album name and asset count are returned; create a token with `expiresAt` in the past and assert `SharedAlbumExpiredException` is thrown

## 9. Frontend — Models and Service

- [ ] 9.1 Create `shared-album.model.ts` in `frontend/src/app/core/models/` with interfaces:
  - `ShareAlbumRequest { expiresAt?: string; }`
  - `ShareAlbumResponse { token: string; url: string; }`
  - `SharedAlbumView { name: string; description: string | null; assets: PaginatedData<Asset>; }`
- [ ] 9.2 Add two methods to `album.service.ts`:
  - `shareAlbum(albumId: number, req: ShareAlbumRequest): Observable<ShareAlbumResponse>` — `POST /api/albums/:id/share`
  - `getSharedAlbum(token: string, page?: number): Observable<SharedAlbumView>` — `GET /api/albums/shared/:token?page=:page`

## 10. Frontend — SharedAlbumComponent

- [ ] 10.1 Create `frontend/src/app/features/shared-album/` directory with `shared-album.component.ts`, `.html`, `.scss`, `.cy.ts`
- [ ] 10.2 Declare standalone `SharedAlbumComponent` with `@Component({ selector: 'app-shared-album' })`; inject `ActivatedRoute` and `AlbumService`; declare `album: SharedAlbumView | null = null`, `notFound = false`, `expired = false`, `currentPage = 0`
- [ ] 10.3 On `ngOnInit`: read `:token` from route params; call `albumService.getSharedAlbum(token, 0)` — on success set `album`; on error status 404 set `notFound = true`; on error status 410 set `expired = true`
- [ ] 10.4 Add `loadPage(page: number)` method: update `currentPage`; call `albumService.getSharedAlbum(token, page)`; update `album.assets`
- [ ] 10.5 Build template: `@if (album)` renders an `<h1>` with `album.name`, an optional `<p>` with `album.description`, a `ThumbnailComponent` grid `@for (asset of album.assets.items)`, and pagination controls (Previous / Next buttons) visible when `album.assets.totalPages > 1`; `@if (expired)` renders `<p>This link has expired.</p>`; `@if (notFound)` renders `<p>Album not found.</p>`; no `MatToolbar` nav bar; import `ThumbnailComponent`, `MatButtonModule`, `MatIconModule`
- [ ] 10.6 Write SCSS: full-page layout with `padding: 24px`; thumbnail grid using `display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px`; heading and description centered

## 11. Frontend — AlbumDetailComponent share action

- [ ] 11.1 Inject `AlbumService` (already injected) and `MatSnackBar` and `MatDialog` into `AlbumDetailComponent`
- [ ] 11.2 Add `shareAlbum()` method: call `albumService.shareAlbum(albumId, {})` — on success build the full URL `window.location.origin + response.url`; attempt `navigator.clipboard.writeText(url)` — on success show `MatSnackBar` "Link copied to clipboard"; if clipboard API is unavailable or rejects, open a `MatDialog` (inline `ShareLinkDialogComponent`) displaying the URL
- [ ] 11.3 Create `ShareLinkDialogComponent` in `features/albums/album-detail/` as a minimal standalone `MatDialog` component accepting `MAT_DIALOG_DATA: { url: string }`; renders a read-only `<input [value]="data.url">` and a Close button
- [ ] 11.4 Add a "Share" `mat-icon-button` with `mat-icon` "share" to the `AlbumDetailComponent` toolbar template, calling `shareAlbum()`; import `MatIconModule`, `MatButtonModule`, `MatSnackBarModule`, `MatDialogModule`

## 12. Frontend — Routing

- [ ] 12.1 Add a lazy route to `app.routes.ts` with no `canActivate` guard:
  `{ path: 's/:token', loadComponent: () => import('./features/shared-album/shared-album.component').then(m => m.SharedAlbumComponent) }`

## 13. Frontend — Tests

- [ ] 13.1 In `shared-album.component.cy.ts`: mount with stubbed `AlbumService.getSharedAlbum` returning a valid `SharedAlbumView` with three assets; assert album name appears in `<h1>`; assert three `ThumbnailComponent` elements are rendered; assert no toolbar nav bar element is present
- [ ] 13.2 Add test: when `getSharedAlbum` returns a 410 error, assert "This link has expired." text is displayed and the thumbnail grid is absent
- [ ] 13.3 Add test: when `getSharedAlbum` returns a 404 error, assert "Album not found." text is displayed
- [ ] 13.4 Add test: when `totalPages > 1`, assert Next button is visible; clicking it calls `getSharedAlbum` with `page = 1`
- [ ] 13.5 In `album-detail.component.cy.ts` (existing file): add a test — stub `albumService.shareAlbum` to return a `ShareAlbumResponse`; stub `navigator.clipboard.writeText` to resolve; click the Share button; assert `albumService.shareAlbum` was called with the correct `albumId`; assert snack bar text contains "copied"
- [ ] 13.6 Add test: when `navigator.clipboard` is unavailable, clicking Share opens `ShareLinkDialogComponent` displaying the URL

## 14. Testing and Commit

- [ ] 14.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 14.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 14.3 Commit all changes (only after both test suites pass)
