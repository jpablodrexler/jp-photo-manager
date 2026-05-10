> **Note:** This change is entirely frontend. It has no dependency on the `hexagonal-architecture` change and can be applied independently in any order. The `POST /api/assets/move` endpoint it calls already exists and its URL and request/response contract will not change after the hexagonal-architecture refactor.

## 1. Folder Picker Dialog Component

- [ ] 1.1 Create `features/gallery/folder-picker-dialog/folder-picker-dialog.component.ts` as a standalone `MatDialog` component; inject `MAT_DIALOG_DATA` typed as `{ mode: 'move' | 'copy', assetCount: number }`
- [ ] 1.2 Add `selectedFolder: string | null` state and a `onFolderSelected(path: string)` handler that captures the chosen path from `FolderNavComponent`
- [ ] 1.3 Implement the template: dialog title derived from `mode`, `<app-folder-nav>` embedded and listening to `(folderSelected)`, confirm button disabled when `selectedFolder` is null or equals the source folder (passed via dialog data), Cancel button
- [ ] 1.4 Add SCSS: constrain dialog height (`max-height: 80vh`), style the embedded folder nav with a scrollable inner area
- [ ] 1.5 Write a Cypress component test for `FolderPickerDialogComponent` covering: correct title for move mode, correct title for copy mode, confirm button disabled before selection, confirm button enabled after valid selection, confirm button disabled when destination equals source, cancel returns null

## 2. Gallery Integration

- [ ] 2.1 Add `moveSelectedAssets(mode: 'move' | 'copy')` method to `GalleryComponent` that opens `FolderPickerDialogComponent` with the current folder and asset count as dialog data
- [ ] 2.2 On dialog confirm: show a "Moving…" / "Copying…" snackbar with `duration: 0`, call `AssetService.moveAssets()` with the appropriate `preserveOriginal` flag, then on success dismiss the snackbar, clear selection, call `loadAssets()`, and show a confirmation snackbar; on error show an error snackbar
- [ ] 2.3 Add **Move to folder…** and **Copy to folder…** items to the `#actionsMenu` in the gallery toolbar template, each calling `moveSelectedAssets('move')` and `moveSelectedAssets('copy')` respectively
- [ ] 2.4 Add `FolderPickerDialogComponent` to the `GalleryComponent` imports array

## 3. Tests

- [ ] 3.1 Write a Cypress component test for the gallery Move flow: stub `AssetService.moveAssets()` to return success; verify snackbar message contains "Moved", selection is cleared, and `loadAssets` is triggered
- [ ] 3.2 Write a Cypress component test for the gallery Copy flow: stub `AssetService.moveAssets()` to return success with `preserveOriginal: true`; verify snackbar message contains "Copied"
- [ ] 3.3 Write a Cypress component test for the failure case: stub `AssetService.moveAssets()` to return an error; verify error snackbar is shown and gallery state is unchanged

## 4. Integration Verification

- [ ] 4.1 Run `npm test` — all Cypress component tests pass
- [ ] 4.2 Run `npm run lint` — no lint errors
- [ ] 4.3 Start backend and frontend locally; select 2–3 photos, click "Move to folder…", pick a destination, confirm — verify assets are gone from the source and appear in the destination after re-cataloging
- [ ] 4.4 Repeat with "Copy to folder…" — verify originals remain in the source folder
- [ ] 4.5 Verify the confirm button stays disabled when the current folder is selected as destination
