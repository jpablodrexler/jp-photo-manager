## Why

Moving and copying photos between folders is a fundamental catalog management workflow, but the web UI provides no way to do it. The backend endpoint (`POST /api/assets/move`) and the frontend service method (`AssetService.moveAssets()`) already exist — the feature is simply not surfaced in the gallery selection menu.

## What Changes

- Add **Move to Folder** and **Copy to Folder** actions to the gallery selection-mode actions menu.
- Introduce a **Folder Picker Dialog** that renders the existing folder tree and lets the user confirm a destination folder, with a "Keep original (copy mode)" toggle.
- After a successful move or copy, the gallery reloads the current folder's assets and shows a confirmation snackbar.
- No changes to the backend — `POST /api/assets/move` with `preserveOriginal: true/false` already handles both operations.

## Capabilities

### New Capabilities
- `move-copy-assets`: The gallery selection menu exposes Move and Copy actions that open a folder picker dialog; on confirmation the selected assets are moved or copied to the chosen destination folder.

### Modified Capabilities

## Impact

- **Frontend only:** New `FolderPickerDialogComponent` in `features/gallery/`; two new menu items in the gallery selection actions menu in `GalleryComponent`; `GalleryComponent` gains a `moveSelectedAssets()` method wiring the dialog result to `AssetService.moveAssets()`.
- **No backend changes:** `POST /api/assets/move` is already implemented and requires no modification.
- **No breaking changes** to any existing endpoint or component contract.
