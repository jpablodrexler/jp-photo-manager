## Why

Renaming photos meaningfully is one of the most common post-import tasks, but the web UI offers no way to do it. Users must leave the application and use an external tool whenever they need to impose a consistent naming scheme on a set of selected photos. A pattern-based batch rename surfaced directly in the gallery selection menu removes that friction and keeps the workflow in one place.

## What Changes

- Add a **Rename selected…** action to the gallery selection-mode actions menu.
- Introduce a **Batch Rename Dialog** with a pattern input field (`{date:yyyy-MM-dd}`, `{index:03d}`, `{original}`, `{ext}`) and a live preview `MatTable` showing old → new filenames before the user applies the change.
- New `POST /api/assets/rename` endpoint accepts `{ assetIds: number[], pattern: string }` and returns `{ previews: [{assetId, oldName, newName}], applied: boolean }`. Calling with `applied: false` (or omitting it) returns the preview without renaming; calling with `applied: true` renames the files on disk and updates `file_name` in the `assets` table.
- No Flyway migration — the `file_name` and `folder_path` columns already exist in the `assets` table.

## Capabilities

### New Capabilities

- `batch-rename`: The gallery selection menu exposes a "Rename selected…" action that opens the Batch Rename Dialog; the dialog shows a live preview table of old → new filenames derived from the entered pattern and applies the rename to all selected assets on confirmation.

### Modified Capabilities

## Impact

- **Backend:** New `RenameAssetsUseCase` interface in `domain/port/in/asset/`; implementation `RenameAssetsUseCaseImpl` in `application/usecase/asset/`; new HTTP DTOs `RenameAssetsRequest` and `RenameAssetsResponse` in `infrastructure/web/dto/`; `POST /api/assets/rename` endpoint added to `AssetController`.
- **Frontend:** New `BatchRenameDialogComponent` in `features/gallery/batch-rename-dialog/`; one new menu item in the gallery selection actions menu; `GalleryComponent` gains a `renameSelectedAssets()` method wiring the dialog to `AssetService.renameAssets()`; `AssetService` gains a `renameAssets()` method.
- **No breaking changes** to any existing endpoint or component contract. No Flyway migration required.
