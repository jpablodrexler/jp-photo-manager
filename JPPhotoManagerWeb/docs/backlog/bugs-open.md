# Open Bugs

## Bug List

| Bug ID | Severity | Area | Environment | Status | Fix | Summary |
| ------ | -------- | ---- | ----------- | ------ | --- | ------- |
| [BUG-001](bugs/BUG-001.md) | S3 | backend | both | ⬜ Open | | `POST`/`DELETE /api/albums/{id}/assets` accepts an unbounded `assetIds` list and runs one or two SQL statements per id inside a single transaction, so a very large request ties up the database and `null` ids reach the repository. |
| [BUG-002](bugs/BUG-002.md) | S3 | /albums | both | ⬜ Open | | `GET /api/albums` (the `/albums` page) runs one asset-count query per album, and for a smart album a full filtered search just to read its total, so the list's load time grows with the number of albums. |
| [BUG-003](bugs/BUG-003.md) | S4 | backend | both | ⬜ Open | | The Albums slice duplicates the smart-vs-manual asset-count branch across two use cases, assembles a paginated DTO inline in `AlbumController.getAlbum`, and omits a reachable 404 from `createAlbum`'s `@ApiResponses`. |
| [BUG-004](bugs/BUG-004.md) | S1 | backend | both | ⬜ Open | | `PUT /api/sync/configuration` and `PUT /api/convert/configuration` store any directory, and `StorageServiceAdapter` does no canonicalisation or containment check, so a sync run can copy to or delete from arbitrary server paths outside `photomanager.root-catalog-folders`. |
| [BUG-005](bugs/BUG-005.md) | S2 | backend | both | ⬜ Open | | `MediaController.streamAsset` builds a `FileSystemResource` from the stored full path with no containment check, so a stored path outside the catalog roots (reachable through BUG-004's folder paths) can be read. |
| [BUG-006](bugs/BUG-006.md) | S2 | /albums | both | ⬜ Open | | Saving the edit-filter dialog of a smart album sends the update without `description`, and the use case overwrites the stored description with `null`. |
| [BUG-007](bugs/BUG-007.md) | S3 | db | both | ⬜ Open | | `findByHashNotDeleted`, `findDuplicateHashes` and `countDuplicates` scan the whole `assets` table because no migration indexes `hash`, and since V32 the hash lookup also sits on the upload path. |
| [BUG-008](bugs/BUG-008.md) | S3 | /albums | both | ⬜ Open | | The album date filters are formatted with `toISOString().split('T')[0]`, which converts the picked local date to UTC, so the saved `dateFrom`/`dateTo` can be one day early (or late, through the dialog's UTC parsing). |

## Column legend

- **Severity**: `S1` (blocker — app unusable, data loss, security hole, auth bypass) · `S2` (major — a feature is broken with no workaround) · `S3` (minor — a feature is impaired but has a workaround) · `S4` (trivial — cosmetic, copy, layout polish)
- **Area**: the affected route, component, or backend area — e.g. `/gallery`, `/albums`, `auth`, `nav`, `backend`, `db` (Flyway/JPA), `kafka`, `infra`. Use the route where the user hit it; use `db` for a migration/entity/query bug with no single owning route, `backend` for a controller/service bug.
- **Environment**: `local` (reproduces on a local dev run only) · `deployed` (reproduces on the deployed cluster/compose stack only) · `both`
- **Status**: `⬜ Open` · `🔶 In Progress` (a `bug-fix` / `bugs-batch-fix` run has started it) · `✅ Fixed` (transient — `bugs-archive` moves the row to `bugs-fixed.md` in the same pass it sets this) · `🚫 Won't fix` · `❓ Cannot reproduce`
- **Fix**: PR link or commit hash once closed; blank while open.

## Details

<!-- Per-bug details live in bugs/BUG-NNN.md (stable path), one file per bug, linked from its row here. `bugs-archive` moves only the table row to bugs-fixed.md and appends the Resolution line to that file. -->

Details for every bug live in `bugs/BUG-NNN.md`, linked from its row.

## Recommended fix order

Severity tier first (S1 before S2 before S3 before S4), then blast radius / how often it bites, then quick-win effort, then bug number. Not a schedule — a reasoning aid for `bugs-next` and for whoever picks up the next fix by hand. Re-derive whenever the bug set or severities change.

- BUG-004 — Sync/Convert directory pairs saved and used with no root containment (S1, backend, both; the only S1: an administrator's sync run can copy to or delete from arbitrary server paths, and BUG-005 builds on the same gap)
- BUG-005 — `GET /api/assets/{id}/stream` serves any stored path with no root check (S2, backend, both; first of the S2s because it is the read side of BUG-004 and fixing both with one root-resolution helper is the quick win)
- BUG-006 — Editing an album's filter erases its description (S2, /albums, both; silent data loss on a common action, a one-line payload fix)
- BUG-001 — Album add/remove assets: unbounded id list drives one SQL statement per id (S3, backend, both; the only open bug so far — a one-line DTO guard is the quick win, the bulk statements the fuller fix)
- BUG-002 — Album list is N+1: one count query per album (S3, /albums, both; same tier as BUG-001 but lower blast radius — it only slows the list, it can't be driven by a single request — so it follows it)
- BUG-007 — `assets.hash` has no index (S3, db, both; same tier as BUG-001/002 but its cost grows with every library and it needs a migration, so it follows them)
- BUG-008 — Album date filters saved one day off in some time zones (S3, /albums, both; a small formatting fix, last of the S3s because it depends on the viewer's time zone)
- BUG-003 — Albums backend code-quality nits: duplicated count logic, inline page assembly, missing 404 in OpenAPI (S4, backend, both; no runtime effect, so it comes last — but fix it together with BUG-002, which rewrites the same count logic)
