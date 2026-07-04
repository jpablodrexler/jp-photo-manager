## Why

All authenticated users currently have identical access. A `VIEWER` role that can browse, view, and download assets but cannot delete, move, catalog, upload, or administer users would allow read-only guest accounts. The `role` column already exists on the `User` entity; only enforcement is missing.

## What Changes

- Add `@PreAuthorize("hasRole('ADMIN')")` (or `hasAnyRole`) annotations to write-path use cases and controller methods that should be restricted to `ADMIN` users
- Add a `VIEWER` role value alongside the existing `ADMIN` role
- Update `SecurityConfig` to expose role-based method security (`@EnableMethodSecurity`)
- No schema migration required — the `role` column already exists

## Capabilities

### New Capabilities

- `role-based-access-control`: A `VIEWER` role can browse, view, download, and rate assets but cannot delete, move, catalog, upload, or administer users. Write-path use cases and controllers are protected with `@PreAuthorize` annotations.

### Modified Capabilities

- `app-navigation`: The frontend conditionally hides write-action buttons (delete, move, upload, catalog) for `VIEWER` role users.

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/SecurityConfig.java` — add `@EnableMethodSecurity`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/domain/enums/Role.java` — add `VIEWER` value
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/` — add `@PreAuthorize` to write-path controller methods
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/` — add `@PreAuthorize` to delete, move, catalog, sync, convert use cases
- `JPPhotoManagerWeb/frontend/src/app/core/services/auth.service.ts` — expose current user role
- `JPPhotoManagerWeb/frontend/src/app/features/gallery/gallery.component.ts` — conditionally hide write actions
