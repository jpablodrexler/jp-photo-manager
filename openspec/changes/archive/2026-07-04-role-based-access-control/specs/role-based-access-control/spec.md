# role-based-access-control

A `VIEWER` role can browse, view, download, and rate assets but cannot delete, move, catalog, upload, or administer users. Write-path use cases and controllers are protected with `@PreAuthorize("hasRole('ADMIN')")` annotations.

---

## ADDED Requirements

### Requirement: VIEWER role is defined and enforced on write paths

The `Role` enum SHALL include `VIEWER`. All write-path use cases (catalog, sync, convert, soft-delete, restore, move, copy, upload, user administration) SHALL be annotated with `@PreAuthorize("hasRole('ADMIN')")`. `@EnableMethodSecurity` SHALL be active in `SecurityConfig`.

#### Scenario: VIEWER cannot trigger catalog

- **GIVEN** a user with `VIEWER` role is authenticated
- **WHEN** `POST /api/assets/catalog` is called
- **THEN** the response is `403 Forbidden`

#### Scenario: VIEWER cannot delete an asset

- **GIVEN** a user with `VIEWER` role is authenticated
- **WHEN** `DELETE /api/assets` is called
- **THEN** the response is `403 Forbidden`

#### Scenario: ADMIN can perform all write operations

- **GIVEN** a user with `ADMIN` role is authenticated
- **WHEN** any write-path endpoint is called
- **THEN** the request proceeds normally

### Requirement: VIEWER can browse and view assets

Read-only endpoints (asset listing, image serving, thumbnail serving, EXIF, albums listing) SHALL be accessible to both `ADMIN` and `VIEWER` roles.

#### Scenario: VIEWER can view gallery

- **GIVEN** a user with `VIEWER` role is authenticated
- **WHEN** `GET /api/assets` is called
- **THEN** the response is `200 OK` with asset data

#### Scenario: VIEWER can rate assets

- **GIVEN** a user with `VIEWER` role is authenticated
- **WHEN** `PATCH /api/assets/{id}/rating` is called
- **THEN** the response is `200 OK` and the rating is updated

### Requirement: Frontend hides write actions for VIEWER role

The `GalleryComponent`, `SyncComponent`, `ConvertComponent`, and navigation bar SHALL hide write-action buttons (delete, move, upload, catalog trigger, sync, convert, user administration) when the authenticated user has the `VIEWER` role.

#### Scenario: VIEWER does not see the Catalog button

- **GIVEN** a user with `VIEWER` role is logged in
- **WHEN** the gallery page loads
- **THEN** the "Catalog" button is not rendered in the toolbar
