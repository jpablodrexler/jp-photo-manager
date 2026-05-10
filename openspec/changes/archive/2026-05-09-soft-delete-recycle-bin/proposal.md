## Why

The gallery's "Remove from catalog" action immediately and permanently deletes the asset's database row and thumbnail, with no possibility of recovery. A misclick or an accidental bulk-remove wipes metadata that took the catalog process time to generate (hash, thumbnail, pixel dimensions, EXIF data). The user's only recourse is to re-catalog the affected folder. Photographers working with large libraries regularly need a safety net between "I don't want to see this image right now" and "I want this image destroyed permanently."

A soft-delete recycle bin addresses this by changing "Remove from catalog" to a reversible operation: the asset row is kept in the database with a `deleted_at` timestamp and hidden from the normal gallery view. Files on disk are never touched by a soft delete. Users can browse the recycle bin, selectively restore assets back into the gallery, or permanently purge them when they are confident. A configurable auto-purge (default 30 days) keeps the recycle bin from accumulating indefinitely.

## What Changes

- Add a Flyway migration `V10__add_soft_delete.sql` adding a nullable `deleted_at TIMESTAMPTZ` column to the `assets` table and an index on it.
- Add `LocalDateTime deletedAt` field to the `Asset` JPA entity.
- Change the existing `DELETE /api/assets` behaviour: when `deleteFiles = false`, set `deleted_at = NOW()` on each asset row (soft delete, file untouched); when `deleteFiles = true`, continue to hard-delete the file and purge the DB row immediately (hard delete bypasses the recycle bin).
- Update `AssetRepository` to filter soft-deleted assets out of all normal gallery queries: replace `findByFolder(Folder, Pageable)` with `findByFolderAndDeletedAtIsNull(Folder, Pageable)`.
- Add `RecycleBinService` interface in `domain/service/` and `RecycleBinServiceImpl` in `infrastructure/service/` implementing: `getDeletedAssets(int pageIndex)`, `restoreAssets(List<Long> assetIds)`, `purgeAssets(List<Long> assetIds)`, `purgeAll()`, `purgeExpired(int retentionDays)`.
- Add three facade methods: `getRecycleBin(int pageIndex)`, `restoreAssets(List<Long> assetIds)`, `purgeRecycleBin(List<Long> assetIds)`.
- Add `RecycleBinController` in `api/` at `/api/recycle-bin` exposing: `GET /api/recycle-bin` (paginated list of soft-deleted assets), `POST /api/recycle-bin/restore` (restore by IDs), `DELETE /api/recycle-bin` (purge by IDs or all).
- Add a scheduled `@Scheduled` method in `RecycleBinServiceImpl` that calls `purgeExpired(retentionDays)` once per day; retention is configurable via `photomanager.recycle-bin-retention-days: 30`.
- Add `RecycleBinComponent` at `/recycle-bin` in the Angular frontend: a paginated thumbnail grid showing soft-deleted assets, with restore and permanent-delete per-item and bulk actions.
- Add a "Recycle Bin" nav link in `app.component.html`.

## Capabilities

### New Capabilities

- `soft-delete-recycle-bin`: Assets removed from the catalog without `deleteFiles=true` are moved to a recycle bin rather than permanently deleted. Users can browse, restore, or permanently purge recycle-bin items. Items older than a configurable retention period are auto-purged.

### Modified Capabilities

- **`DELETE /api/assets` (asset-deletion)**: `deleteFiles=false` now performs a soft delete (sets `deleted_at`) instead of removing the DB row. `deleteFiles=true` continues to hard-delete. The response contract (`204 No Content`) is unchanged.

## Impact

- **`V10__add_soft_delete.sql`** *(new)*: `ALTER TABLE assets ADD COLUMN deleted_at TIMESTAMPTZ NULL`; `CREATE INDEX ix_assets_deleted_at ON assets(deleted_at)`.
- **`Asset.java`**: new `@Column(name = "deleted_at") LocalDateTime deletedAt` field (nullable).
- **`AssetRepository.java`**: `findByFolderAndDeletedAtIsNull(Folder, Pageable)` replaces `findByFolder(Folder, Pageable)` for paginated gallery queries; new `findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable)` for recycle-bin listing; new `@Modifying @Query` to bulk-set `deleted_at`.
- **`MoveAssetsServiceImpl.deleteAssets`**: when `deleteFile = false`, set `asset.setDeletedAt(LocalDateTime.now())` and save instead of `assetRepository.delete(asset)`; thumbnail is kept (not deleted) so it remains available for the recycle-bin preview.
- **`RecycleBinService.java`** *(new)*: domain service interface in `domain/service/`.
- **`RecycleBinServiceImpl.java`** *(new)*: `@Service` in `infrastructure/service/`; implements restore (sets `deletedAt = null`), purge (calls `storageService.deleteFile` + `thumbnailStorageService.deleteThumbnail` + `assetRepository.delete`), and scheduled `@Scheduled(cron = "0 0 2 * * *")` auto-purge.
- **`PhotoManagerFacade.java`**: three new method signatures.
- **`PhotoManagerFacadeImpl.java`**: delegates to `RecycleBinService`; inject `RecycleBinService`.
- **`RecycleBinController.java`** *(new)*: `@RestController` at `/api/recycle-bin`.
- **`recycle-bin.service.ts`** *(new)*: Angular service wrapping the three recycle-bin API calls.
- **`RecycleBinComponent`** *(new)*: standalone component at `/recycle-bin` with paginated grid, restore, and purge actions.
- **`app.routes.ts`**: new lazy route for `/recycle-bin`.
- **`app.component.html`**: new "Recycle Bin" nav link.
- **No breaking API changes** — `DELETE /api/assets` response shape is unchanged; `GET /api/assets` naturally excludes soft-deleted assets via the updated repository query.
