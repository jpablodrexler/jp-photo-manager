# Open Bugs

## Bug List

| Bug ID | Severity | Area | Environment | Status | Fix | Summary |
| ------ | -------- | ---- | ----------- | ------ | --- | ------- |
| [BUG-001](bugs/BUG-001.md) | S3 | backend | both | ⬜ Open | | `POST`/`DELETE /api/albums/{id}/assets` accepts an unbounded `assetIds` list and runs one or two SQL statements per id inside a single transaction, so a very large request ties up the database and `null` ids reach the repository. |
| [BUG-002](bugs/BUG-002.md) | S3 | /albums | both | ⬜ Open | | `GET /api/albums` (the `/albums` page) runs one asset-count query per album, and for a smart album a full filtered search just to read its total, so the list's load time grows with the number of albums. |

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

- BUG-001 — Album add/remove assets: unbounded id list drives one SQL statement per id (S3, backend, both; the only open bug so far — a one-line DTO guard is the quick win, the bulk statements the fuller fix)
- BUG-002 — Album list is N+1: one count query per album (S3, /albums, both; same tier as BUG-001 but lower blast radius — it only slows the list, it can't be driven by a single request — so it follows it)
