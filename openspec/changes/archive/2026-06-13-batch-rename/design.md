## Context

The gallery already supports multi-select (`selectedAssets: Set<number>` in `GalleryComponent`) and exposes a selection actions menu for Move, Copy, Download, Tag, and Delete. The `Asset` domain model carries `fileName`, `folder.path`, and `fileCreationDateTime`, which are the inputs needed to expand pattern tokens. The `StoragePort` (`StorageServiceAdapter`) already provides `moveFile()` / `copyFile()` for on-disk operations; renaming can be implemented as a `moveFile()` from the old path to the new path within the same directory. No new infrastructure dependency is required.

The `AddToAlbumDialogComponent`, `FolderPickerDialogComponent`, and `BulkTagDialogComponent` in `features/gallery/` establish the project's dialog pattern: open via `MatDialog`, pass context as `MAT_DIALOG_DATA`, return a result via `dialogRef.close()`, and handle the async call in `GalleryComponent`.

## Goals / Non-Goals

**Goals:**
- A pattern language with four tokens: `{date:yyyy-MM-dd}`, `{index:03d}`, `{original}`, `{ext}`.
- A two-step UX: enter pattern → see live preview table → confirm to apply.
- The same `POST /api/assets/rename` endpoint handles both preview (dry-run) and apply, controlled by `applied` in the request body.
- After a successful rename the gallery reloads, the selection is cleared, and a confirmation snackbar is shown.
- On a name collision (another asset already has the target filename in the same folder) the use case returns an error and no files are renamed.

**Non-Goals:**
- Undo/undo history for rename operations.
- Renaming files across folders (pattern always produces a filename, not a path).
- Support for custom token plugins or user-defined token sequences beyond the four listed tokens.
- Progress streaming via SSE (renames are fast; synchronous HTTP is sufficient).

## Decisions

### 1. Single endpoint for preview and apply

**Decision:** `POST /api/assets/rename` accepts `{ assetIds, pattern, applied }`. When `applied` is `false` (or absent) the use case resolves the pattern for each asset and returns the preview list without touching the filesystem. When `applied` is `true` it also renames each file and updates the DB record.

**Rationale:** Keeping preview and apply in one endpoint avoids a separate `GET /api/assets/rename/preview` route that would either need to accept a request body (non-idiomatic for GET) or encode asset IDs and pattern in query parameters (fragile for long lists). A single endpoint also means the frontend can call the same service method twice — first for preview, then for apply — with a trivial boolean toggle.

**Alternatives considered:**
- *Two separate endpoints (`/preview` + `/apply`)*: cleaner semantics but doubles the surface area and requires the frontend to remember state between calls. A stale preview between the two calls is a minor inconsistency risk.
- *Apply-only endpoint, preview computed on the frontend*: eliminates the round-trip for preview but duplicates the pattern-expansion logic across Java and TypeScript, making them drift over time.

### 2. Pattern token resolution in the use case, not the controller

**Decision:** `RenameAssetsUseCaseImpl` is responsible for resolving `{date:...}`, `{index:...}`, `{original}`, and `{ext}` tokens. The controller receives raw `assetIds` and `pattern` and returns the resolved `RenameAssetsResponse` without knowing anything about token semantics.

**Rationale:** Token resolution touches the domain (asset fields, date formatting, zero-padding) and belongs in the use case where it can be unit-tested without Spring context. The controller stays thin — it validates the request, delegates, and maps the response.

**Alternatives considered:**
- *Token resolution in a domain service injected by the use case*: correct layering, but the resolution logic is simple enough that a private method in the use case avoids an extra class without violating single-responsibility.

### 3. Collision detection before any rename

**Decision:** Before renaming any file, the use case checks that no two assets in the batch resolve to the same target filename and that no existing asset in the same folder already has any of the target filenames. If a collision is detected the entire operation is aborted and a `400 Bad Request` with an `ASSET_NAME_COLLISION` error is returned; no files are modified.

**Rationale:** Partial renames leave the catalog in an inconsistent state where some files have been moved on disk but their DB records still reference the old name. An all-or-nothing check before touching any file is safer and easier to explain to the user ("two photos would get the same name — change the pattern").

**Alternatives considered:**
- *Skip colliding files and rename the rest*: unpredictable for the user; silently leaving some files unmodified is worse than a clear error.
- *Auto-deduplicate by appending `_2`, `_3`*: useful UX but increases implementation complexity and may still surprise users. Defer to a later iteration.

### 4. Live preview via `applied: false` call on pattern change (debounced)

**Decision:** The `BatchRenameDialogComponent` calls `POST /api/assets/rename` with `applied: false` after a 400 ms debounce on every pattern change. The response populates a `MatTable` showing `oldName → newName` for each selected asset.

**Rationale:** Computing the preview on the backend (single source of truth for token resolution) means the user sees exactly what the backend will produce. A 400 ms debounce keeps the number of requests reasonable while still feeling responsive.

**Alternatives considered:**
- *Client-side preview*: instant feedback, no network round-trip, but duplicates the resolution logic in TypeScript.
- *Preview only on an explicit "Preview" button click*: avoids debounced requests but adds friction — the user must click a button to see whether their pattern is valid.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| A race condition where two users rename the same asset simultaneously | The use case runs inside `@Transactional`; the DB update is atomic. The last writer wins, which is acceptable for a single-user application. |
| The `{date:...}` format string is attacker-controlled input | The use case validates that the format string contains only safe `DateTimeFormatter` pattern letters (letters, digits, `-`, `_`, `.`, `:`) before passing it to `DateTimeFormatter.ofPattern()`. An invalid pattern returns `400 Bad Request`. |
| A rename leaves a thumbnail orphan if the `assetId` does not change | Thumbnails are keyed by `assetId` (e.g., `42.bin`), not by filename, so renaming the file does not affect the thumbnail. No cleanup needed. |
| Very long asset lists slow the preview response | The use case processes assets in memory (no DB call per asset during preview); the typical selection is dozens of assets, not thousands. Acceptable for v1. |
