## Context

The backend (`POST /api/assets/move`) and the Angular service layer (`AssetService.moveAssets()`) both already support moving and copying assets between folders. The `preserveOriginal` flag on the request body distinguishes a destructive move from a non-destructive copy. The gap is entirely in the UI: the gallery selection-mode actions menu only exposes Download, Remove from catalog, and Delete — no Move or Copy actions.

The existing `AddToAlbumDialogComponent` provides a near-identical dialog pattern: it opens a `MatDialog`, presents a list of options (albums), and returns a selection to `GalleryComponent` for processing. The `FolderNavComponent` is a standalone tree control that already emits a `folderSelected` event — it is designed to be embedded wherever a folder needs to be chosen.

## Goals / Non-Goals

**Goals:**
- Two new actions in the selection menu: "Move to folder…" and "Copy to folder…".
- A `FolderPickerDialogComponent` that embeds `FolderNavComponent` and returns the selected folder path.
- On confirmation: call `AssetService.moveAssets()`, reload the gallery, show snackbar feedback.
- Actions disabled (greyed out) when no assets are selected.

**Non-Goals:**
- Creating a new destination folder from within the dialog (out of scope; folders are created by cataloging).
- Drag-and-drop from the thumbnail grid to the folder tree (a separate, larger interaction design).
- Undo / undo history for move operations.
- Progress indication for large bulk moves (the existing snackbar feedback is sufficient for v1).

## Decisions

### 1. Reuse `FolderNavComponent` inside the dialog

**Decision:** `FolderPickerDialogComponent` imports and renders `FolderNavComponent` directly. When the user clicks a folder in the tree, the dialog captures the path but does not close immediately — the user must click **Move here** or **Copy here** to confirm.

**Rationale:** `FolderNavComponent` already handles lazy-loading the folder tree via `FolderService`, keyboard navigation, and expand/collapse. Building a separate folder picker from scratch would duplicate all of that. The two-step (select → confirm) UX prevents accidental moves on a single click.

**Alternatives considered:**
- *Inline folder path text input with autocomplete*: simpler dialog, but requires the user to know or type the exact path. Error-prone and not discoverable.
- *Close dialog immediately on folder click*: faster but risky — a mis-click on the wrong folder triggers an irreversible move.

### 2. Single dialog component, mode passed as data

**Decision:** One `FolderPickerDialogComponent` receives `{ mode: 'move' | 'copy', assetCount: number }` as `MAT_DIALOG_DATA`. The title, confirm button label, and `preserveOriginal` value are derived from `mode`.

**Rationale:** Move and Copy are identical interactions — the only difference is the label and whether `preserveOriginal` is true. A single component with a `mode` input avoids duplicating template and logic. This is the same pattern used by `AddToAlbumDialogComponent` which receives album list via dialog data.

### 3. Reload gallery after move/copy

**Decision:** On a successful `moveAssets()` response, call `GalleryComponent.loadAssets()` to reload the current folder. Selected assets are cleared.

**Rationale:** After a move the source files no longer exist in the current folder — the gallery must refresh to reflect this. After a copy the originals remain, so a reload shows the unchanged state (which is correct). A full reload is simpler and more reliable than trying to surgically remove moved items from `this.assets`.

### 4. Disable actions when destination equals source folder

**Decision:** The confirm button in the dialog is disabled when the selected folder path equals `GalleryComponent.currentFolder`.

**Rationale:** Moving assets to the same folder they are already in is a no-op but triggers a backend call and a reload. Disabling the button prevents the confusing experience of "Move here" doing nothing visible.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `FolderNavComponent` emits `folderSelected` and also navigates the gallery (if used in the sidenav context) | The dialog renders `FolderNavComponent` in isolation — its output is captured by the dialog, not routed to the gallery. The component is standalone and has no side-effects beyond its `@Output()`. |
| Moving 200+ assets is slow; no progress indication | A "Moving…" snackbar with `duration: 0` is shown during the request and dismissed on completion. A dedicated progress stream is a v2 concern. |
| Dialog is tall on small screens (folder tree can be deep) | Set `maxHeight: 80vh` on the dialog; `FolderNavComponent` already scrolls internally. |
| User copies to a folder that is not yet cataloged | The copy still succeeds at the filesystem level. The new location won't appear in the catalog until the next catalog run — this is existing behavior, not a regression. |

## Migration Plan

- No backend changes, no database migrations, no Flyway scripts.
- The change is purely additive to `GalleryComponent` and adds one new dialog component.
- Rollback: remove the two menu items and delete `FolderPickerDialogComponent`.
