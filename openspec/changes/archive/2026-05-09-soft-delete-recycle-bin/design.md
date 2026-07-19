## Context

`MoveAssetsServiceImpl.deleteAssets(Asset[], boolean deleteFile)` is the single deletion entry point. When `deleteFile = false` it currently calls `thumbnailStorageService.deleteThumbnail` then `assetRepository.delete`. When `deleteFile = true` it additionally calls `storageService.deleteFile`. The soft-delete change intercepts only the `deleteFile = false` branch and redirects it to a timestamp update; the `deleteFile = true` path is unchanged.

`AssetRepository.findByFolder(Folder, Pageable)` is a Spring Data derived query used in `PhotoManagerFacadeImpl.getAssets`. Because it uses no explicit JPQL, replacing it with `findByFolderAndDeletedAtIsNull(Folder, Pageable)` costs nothing — Spring Data generates the correct `WHERE deleted_at IS NULL` automatically.

The `FindDuplicatedAssetsService` also queries all assets (via `assetRepository.findAll()`). Soft-deleted assets should be excluded from duplicate detection, so that method also needs updating.

The planned migration is `V10` (after V9 for refresh-token).

## Goals / Non-Goals

**Goals:**

- Make "Remove from catalog" (`deleteFiles=false`) reversible via a recycle bin.
- Keep `deleteFiles=true` as a hard, immediate, unrecoverable operation (no recycle bin).
- Normal gallery queries see no soft-deleted assets.
- Recycle bin is browsable, with per-item and bulk restore/purge.
- Auto-purge items older than a configurable retention period.
- Thumbnails are preserved during soft delete for recycle-bin previews.

**Non-Goals:**

- Per-user recycle bins (the bin is shared across all users, consistent with the shared catalog model).
- Undo/redo history beyond the recycle bin.
- Soft-deleting folders (only individual assets are soft-deleted).
- Purge confirmation dialog on the backend (frontend responsibility).

## Decisions

### 1. `deleted_at` column on `assets` — not a separate `recycle_bin` table

**Decision:** A nullable `deleted_at TIMESTAMPTZ` column is added to the existing `assets` table. Soft-deleted assets remain in the same table and are excluded from normal queries via `AND deleted_at IS NULL`.

**Rationale:** A separate recycle-bin table would require copying or moving the full asset row (including all catalog metadata) on soft-delete and back on restore. This duplicates data, complicates restore, and splits asset metadata across two tables. A single `deleted_at` column keeps all metadata co-located, makes restore a single `UPDATE`, and aligns with the standard soft-delete pattern for relational databases.

### 2. Thumbnail is NOT deleted on soft delete — preserved for recycle-bin preview

**Decision:** `MoveAssetsServiceImpl.deleteAssets` when `deleteFile = false` does NOT call `thumbnailStorageService.deleteThumbnail`. The thumbnail `.bin` file remains on disk. On restore, the thumbnail is immediately available. On hard purge from the recycle bin, the thumbnail is then deleted.

**Rationale:** The recycle-bin UI uses the thumbnail URL to render a preview of soft-deleted assets. Without the thumbnail the recycle bin would show broken image placeholders. The storage cost of retaining thumbnails during the retention period is minimal (thumbnails are 200×150 JPEG, approximately 5–20 KB each).

### 3. Hard delete (`deleteFiles=true`) bypasses the recycle bin entirely

**Decision:** When `deleteFiles = true` in the `DELETE /api/assets` request, `MoveAssetsServiceImpl.deleteAssets` continues to delete the file, delete the thumbnail, and `assetRepository.delete` the row — no `deleted_at` is set and nothing appears in the recycle bin.

**Rationale:** The user explicitly chose "Delete files" — a destructive action confirmed in the UI. Routing it through the recycle bin would contradict the intent and confuse users who expect the file to be gone. The two actions have different semantics: soft-delete is "I don't want this in my gallery" whereas hard delete is "destroy this".

### 4. `RecycleBinService` as a dedicated domain service — not added to `MoveAssetsService`

**Decision:** All recycle-bin read/restore/purge operations are implemented in a new `RecycleBinService` interface and `RecycleBinServiceImpl`. `MoveAssetsServiceImpl` is only responsible for the soft-delete write (setting `deletedAt`). The facade delegates recycle-bin queries and mutations to `RecycleBinService`.

**Rationale:** `MoveAssetsService` is focused on file I/O and DB moves. Adding recycle-bin listing, restore, and scheduled purge to it would violate single responsibility. A dedicated service also makes the purge scheduler (`@Scheduled`) self-contained.

### 5. `@Scheduled` daily auto-purge in `RecycleBinServiceImpl`

**Decision:** `RecycleBinServiceImpl` has a `@Scheduled(cron = "0 0 2 * * *")` method `autoPurgeExpired()` that calls `purgeExpired(retentionDays)`. `retentionDays` is read from `@Value("${photomanager.recycle-bin-retention-days:30}")`. `AppConfig` must have `@EnableScheduling`.

**Rationale:** A scheduled purge is the simplest way to enforce a retention limit without requiring a separate cron job or manual admin action. Running at 02:00 server time avoids peak usage hours. The retention period is configurable so operators running on resource-constrained hardware can reduce it.

### 6. `GET /api/assets` exclusion via Spring Data `findByFolderAndDeletedAtIsNull`

**Decision:** Replace the `findByFolder(Folder, Pageable)` call in `PhotoManagerFacadeImpl.getAssets` with `findByFolderAndDeletedAtIsNull(Folder, Pageable)`. `FindDuplicatedAssetsService` is similarly updated to use `findByHashAndDeletedAtIsNull` or a new `@Query` that excludes `deleted_at IS NOT NULL`.

**Rationale:** Spring Data's derived query keywords handle the null check without requiring a hand-written JPQL query. This approach is consistent with the existing repository style and requires minimal code change.

## Data Flow

```
User selects assets → clicks "Remove from catalog" (deleteFiles=false)
  → DELETE /api/assets?assetIds=...&deleteFiles=false
    → AssetController.deleteAssets()
    → facade.deleteAssets(assetIds, false)
      → moveAssetsService.deleteAssets(assets, false)
        → for each asset:
            asset.setDeletedAt(LocalDateTime.now())
            assetRepository.save(asset)       [thumbnail kept]
    → 204 No Content
  → Gallery reloads; soft-deleted assets absent from GET /api/assets

User opens Recycle Bin (/recycle-bin)
  → RecycleBinComponent loads
  → recycleBinService.getRecycleBin(0)
    → GET /api/recycle-bin?page=0
      → RecycleBinController.listDeleted(page)
        → facade.getRecycleBin(page)
          → recycleBinService.getDeletedAssets(page)
            → assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable)
    → 200 OK PaginatedData<AssetDto>

User selects asset → clicks "Restore"
  → recycleBinService.restoreAssets([assetId])
    → POST /api/recycle-bin/restore { assetIds: [...] }
      → RecycleBinController.restore()
        → facade.restoreAssets(assetIds)
          → recycleBinService.restoreAssets(assetIds)
            → asset.setDeletedAt(null); assetRepository.save()
    → 204 No Content; recycle bin reloads

User clicks "Purge" on a recycle-bin item
  → recycleBinService.purgeAssets([assetId])
    → DELETE /api/recycle-bin { assetIds: [...] }
      → RecycleBinController.purge()
        → facade.purgeRecycleBin(assetIds)
          → recycleBinService.purgeAssets(assetIds)
            → storageService.deleteFile(path)  [optional — file may already be gone]
            → thumbnailStorageService.deleteThumbnail(blob)
            → assetRepository.delete(asset)
    → 204 No Content

Scheduled auto-purge at 02:00 daily
  → RecycleBinServiceImpl.autoPurgeExpired()
    → assetRepository.findByDeletedAtBeforeAndDeletedAtIsNotNull(cutoff)
    → purge each (delete file if present, delete thumbnail, delete row)
```

## File Change List

**New files:**

- `backend/.../db/migration/V10__add_soft_delete.sql`
- `backend/.../domain/service/RecycleBinService.java`
- `backend/.../infrastructure/service/RecycleBinServiceImpl.java`
- `backend/.../api/RecycleBinController.java`
- `backend/.../api/dto/RecycleBinPurgeRequest.java`
- `backend/.../api/dto/RecycleBinRestoreRequest.java`
- `backend/.../test/.../RecycleBinControllerTest.java`
- `backend/.../test/.../RecycleBinServiceTest.java`
- `frontend/src/app/core/services/recycle-bin.service.ts`
- `frontend/src/app/features/recycle-bin/recycle-bin.component.ts/html/scss/cy.ts`

**Modified files:**

- `backend/.../domain/entity/Asset.java` — add `deletedAt` field
- `backend/.../domain/repository/AssetRepository.java` — add `findByFolderAndDeletedAtIsNull`, `findByDeletedAtIsNotNullOrderByDeletedAtDesc`, `findByDeletedAtBefore`
- `backend/.../infrastructure/service/MoveAssetsServiceImpl.java` — soft-delete path for `deleteFile=false`
- `backend/.../infrastructure/service/FindDuplicatedAssetsServiceImpl.java` — exclude soft-deleted from duplicate detection
- `backend/.../application/PhotoManagerFacade.java` — three new method signatures
- `backend/.../application/PhotoManagerFacadeImpl.java` — delegate to `RecycleBinService`; update `getAssets` to use `findByFolderAndDeletedAtIsNull`
- `backend/.../config/AppConfig.java` — add `@EnableScheduling`
- `frontend/src/app/app.routes.ts` — new `/recycle-bin` lazy route
- `frontend/src/app/app.component.html` — new "Recycle Bin" nav link
