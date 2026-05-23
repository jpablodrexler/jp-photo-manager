## 1. Domain — Role enum

- [ ] 1.1 Add `VIEWER` to `domain/enums/Role.java` (alongside existing `ADMIN`)

## 2. Security configuration

- [ ] 2.1 Add `@EnableMethodSecurity` to `SecurityConfig` (or a dedicated `MethodSecurityConfig`)

## 3. Backend — @PreAuthorize on write-path use cases

- [ ] 3.1 Add `@PreAuthorize("hasRole('ADMIN')")` to `CatalogAssetsUseCaseImpl.execute()`
- [ ] 3.2 Add `@PreAuthorize("hasRole('ADMIN')")` to `SyncAssetsUseCaseImpl.execute()`
- [ ] 3.3 Add `@PreAuthorize("hasRole('ADMIN')")` to `ConvertAssetsUseCaseImpl.execute()`
- [ ] 3.4 Add `@PreAuthorize("hasRole('ADMIN')")` to `SoftDeleteAssetUseCaseImpl.execute()`
- [ ] 3.5 Add `@PreAuthorize("hasRole('ADMIN')")` to `RestoreAssetUseCaseImpl.execute()`
- [ ] 3.6 Add `@PreAuthorize("hasRole('ADMIN')")` to `MoveAssetsUseCaseImpl.execute()`
- [ ] 3.7 Add `@PreAuthorize("hasRole('ADMIN')")` to `CopyAssetsUseCaseImpl.execute()`
- [ ] 3.8 Add `@PreAuthorize("hasRole('ADMIN')")` to `UploadAssetUseCaseImpl.execute()` (if applicable)
- [ ] 3.9 Add `@PreAuthorize("hasRole('ADMIN')")` to all user administration use cases

## 4. Backend tests

- [ ] 4.1 Add integration test: `VIEWER` calling `POST /api/assets/catalog` receives `403 Forbidden`
- [ ] 4.2 Add integration test: `VIEWER` calling `DELETE /api/assets` receives `403 Forbidden`
- [ ] 4.3 Add integration test: `VIEWER` calling `GET /api/assets` receives `200 OK`

## 5. Frontend — expose role from /api/auth/me

- [ ] 5.1 Ensure `GET /api/auth/me` returns `{ username, role }` (add `role` field to the response DTO if missing)
- [ ] 5.2 In `AuthService`, store the user's `role` after login and expose it as `isAdmin(): boolean`

## 6. Frontend — conditionally hide write actions

- [ ] 6.1 In `GalleryComponent` template, wrap the Catalog button in `@if (authService.isAdmin())`
- [ ] 6.2 Wrap delete, move, and copy action buttons in `@if (authService.isAdmin())`
- [ ] 6.3 In `AppComponent` navigation, hide the Sync, Convert, and Admin links for `VIEWER` users
- [ ] 6.4 In `SyncComponent` and `ConvertComponent`, disable the start button for `VIEWER` users (defense-in-depth)

## 7. Frontend tests

- [ ] 7.1 Cypress component test: `GalleryComponent` with `isAdmin() = false` does not render the Catalog or Delete buttons

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
