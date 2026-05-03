# soft-delete-recycle-bin

Specifies how removing an asset from the catalog becomes reversible by routing it through a recycle bin rather than immediately destroying the database row, and how users can browse, restore, or permanently purge recycle-bin items.

---

### Requirement: "Remove from catalog" moves assets to the recycle bin instead of deleting them

When `DELETE /api/assets` is called with `deleteFiles=false`, the backend SHALL set `deleted_at = NOW()` on each targeted asset row rather than deleting the row. The asset's file on disk and its thumbnail SHALL be left untouched. The response SHALL remain `204 No Content`. A subsequent `GET /api/assets` for the same folder SHALL NOT include the soft-deleted assets.

#### Scenario: Remove from catalog is reversible

- **GIVEN** asset 42 exists in folder `/photos/2024` and is visible in the gallery
- **WHEN** the client calls `DELETE /api/assets?assetIds=42&deleteFiles=false`
- **THEN** the response is `204 No Content`; a subsequent `GET /api/assets?folderPath=/photos/2024` does NOT include asset 42; the asset row in the `assets` table has `deleted_at` set to the current time; the file `/photos/2024/<filename>` still exists on disk; the thumbnail `.bin` file still exists

#### Scenario: Multiple assets soft-deleted in one request

- **GIVEN** assets 10, 11, 12 are visible in the gallery
- **WHEN** `DELETE /api/assets?assetIds=10,11,12&deleteFiles=false`
- **THEN** all three rows have `deleted_at` set; the gallery shows neither of them; all three files remain on disk

---

### Requirement: "Delete files" bypasses the recycle bin entirely

When `DELETE /api/assets` is called with `deleteFiles=true`, the backend SHALL delete the file from disk, delete the thumbnail, and remove the asset row from the database immediately. The asset SHALL NOT appear in the recycle bin.

#### Scenario: Hard delete does not appear in recycle bin

- **GIVEN** asset 99 exists in the gallery
- **WHEN** `DELETE /api/assets?assetIds=99&deleteFiles=true`
- **THEN** asset 99 is removed from the `assets` table; the file on disk is deleted; `GET /api/recycle-bin` does NOT include asset 99

---

### Requirement: The recycle bin lists soft-deleted assets paginated by deletion date

`GET /api/recycle-bin` SHALL return a paginated list of all assets with a non-null `deleted_at`, ordered by `deleted_at` descending (most recently deleted first). The response shape SHALL match the existing `PaginatedData<AssetDto>` structure so the frontend can render thumbnails using the standard `thumbnailUrl`.

#### Scenario: Recycle bin shows soft-deleted assets

- **GIVEN** assets 42 and 43 have been soft-deleted
- **WHEN** the client calls `GET /api/recycle-bin?page=0`
- **THEN** the response is `200 OK`; `items` contains `AssetDto` objects for assets 42 and 43; each has a valid `thumbnailUrl`; `totalItems` is 2

#### Scenario: Recycle bin is empty when no assets are soft-deleted

- **GIVEN** no assets have been soft-deleted
- **WHEN** the client calls `GET /api/recycle-bin?page=0`
- **THEN** the response is `200 OK`; `items` is empty; `totalItems` is 0

---

### Requirement: Restored assets return to the gallery

`POST /api/recycle-bin/restore` SHALL accept `{ "assetIds": [...] }`, set `deleted_at = NULL` on each matching asset, and return `204 No Content`. A subsequent `GET /api/assets` for the asset's folder SHALL include the restored asset.

#### Scenario: Asset is restored to the gallery

- **GIVEN** asset 42 is in the recycle bin with `deleted_at` set
- **WHEN** `POST /api/recycle-bin/restore` with body `{ "assetIds": [42] }`
- **THEN** the response is `204 No Content`; `deleted_at` for asset 42 is `NULL`; `GET /api/assets?folderPath=/photos/2024` includes asset 42 again; the thumbnail is still intact

---

### Requirement: Assets can be permanently purged from the recycle bin

`DELETE /api/recycle-bin` SHALL accept an optional `{ "assetIds": [...] }` body. When `assetIds` is provided, only those assets are purged. When the body is absent or `assetIds` is null, all soft-deleted assets are purged. Purge SHALL delete the file from disk (logging a warning if the file is missing), delete the thumbnail, and remove the asset row from the database.

#### Scenario: Selective purge of recycle-bin items

- **GIVEN** assets 42 and 43 are in the recycle bin
- **WHEN** `DELETE /api/recycle-bin` with body `{ "assetIds": [42] }`
- **THEN** the response is `204 No Content`; asset 42 is removed from the database and its thumbnail deleted; asset 43 remains in the recycle bin

#### Scenario: Empty entire recycle bin

- **GIVEN** three assets are in the recycle bin
- **WHEN** `DELETE /api/recycle-bin` with no body
- **THEN** the response is `204 No Content`; all three assets are removed from the database; all three thumbnails are deleted; `GET /api/recycle-bin` returns `totalItems: 0`

#### Scenario: Purge of asset whose file is already missing from disk

- **GIVEN** an asset is in the recycle bin but its file was already deleted from disk by an external process
- **WHEN** `DELETE /api/recycle-bin` with that asset's ID
- **THEN** the response is `204 No Content`; a warning is logged; the thumbnail and DB row are still removed

---

### Requirement: Soft-deleted assets are excluded from duplicate detection

`GET /api/assets/duplicates` SHALL NOT include assets with a non-null `deleted_at`. Two copies of the same file where one is soft-deleted SHALL NOT be reported as a duplicate group.

#### Scenario: Soft-deleted asset excluded from duplicates

- **GIVEN** assets 10 and 11 share the same SHA-256 hash; asset 10 is soft-deleted
- **WHEN** `GET /api/assets/duplicates`
- **THEN** neither asset 10 nor asset 11 appears in the response (the group has only one active member; a single-member group is not a duplicate)

---

### Requirement: Recycle-bin items older than the retention period are auto-purged

Assets with `deleted_at` older than `photomanager.recycle-bin-retention-days` (default 30 days) SHALL be automatically purged at 02:00 server time each day. Auto-purge SHALL behave identically to a manual purge: delete file (with warning on error), delete thumbnail, delete DB row.

#### Scenario: Expired item is auto-purged

- **GIVEN** asset 55 was soft-deleted 31 days ago and retention is 30 days
- **WHEN** the scheduled auto-purge job runs
- **THEN** asset 55 is removed from the database and its thumbnail deleted; `GET /api/recycle-bin` does NOT include asset 55

#### Scenario: Non-expired item is not auto-purged

- **GIVEN** asset 56 was soft-deleted 10 days ago and retention is 30 days
- **WHEN** the scheduled auto-purge job runs
- **THEN** asset 56 remains in the recycle bin with its `deleted_at` unchanged

---

### Requirement: The frontend shows a Recycle Bin page with restore and purge actions

The `RecycleBinComponent` at `/recycle-bin` SHALL display a paginated thumbnail grid of soft-deleted assets. Each asset SHALL be selectable via a checkbox. The toolbar SHALL include "Restore" and "Delete Permanently" buttons enabled when at least one asset is selected, and an "Empty Recycle Bin" button that purges all items regardless of selection.

#### Scenario: User restores a soft-deleted asset

- **GIVEN** the Recycle Bin page shows asset 42
- **WHEN** the user selects asset 42 and clicks "Restore"
- **THEN** `POST /api/recycle-bin/restore` is called with `{ assetIds: [42] }`; a "Restored successfully" snack bar appears; the recycle bin grid reloads and asset 42 is no longer shown

#### Scenario: User permanently deletes a selected asset

- **GIVEN** the Recycle Bin page shows assets 42 and 43; asset 42 is checked
- **WHEN** the user clicks "Delete Permanently"
- **THEN** `DELETE /api/recycle-bin` is called with `{ assetIds: [42] }`; a "Permanently deleted" snack bar appears; asset 42 is absent from the reloaded grid; asset 43 remains

#### Scenario: User empties the entire recycle bin

- **GIVEN** the Recycle Bin page shows three assets
- **WHEN** the user clicks "Empty Recycle Bin"
- **THEN** `DELETE /api/recycle-bin` is called with no body; a "Recycle bin emptied" snack bar appears; the grid shows zero items

---

### Requirement: Recycle-bin endpoints require authentication

All `/api/recycle-bin/**` endpoints SHALL require a valid JWT cookie. Unauthenticated requests SHALL receive `401 Unauthorized`.

#### Scenario: Unauthenticated recycle-bin access

- **GIVEN** no valid JWT cookie is present
- **WHEN** `GET /api/recycle-bin` is called
- **THEN** the response is `401 Unauthorized`
