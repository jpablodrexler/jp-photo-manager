---
name: bugs-archive
description: Marks bugs resolved in JPPhotoManagerWeb/docs/backlog/bugs-open.md and moves them to JPPhotoManagerWeb/docs/backlog/bugs-fixed.md (or records them as 🚫 Won't fix / ❓ Cannot reproduce). Appends a Resolution line to the bug's details file JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md — the file itself never moves. TRIGGER when a bug has been fixed (by bug-fix, bugs-batch-fix, or by hand) and needs closing, or when the user decides not to fix one. This is the bug-family counterpart to features-archive.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Close one or more bugs: move the table row from
`JPPhotoManagerWeb/docs/backlog/bugs-open.md` to
`JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` (for a real fix), or update
the row's Status in place to `🚫 Won't fix` / `❓ Cannot reproduce` (for a
resolution without a code change). In every case the bug's details file
`JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md` stays where it is — only
a `- **Resolution:**` bullet is appended to it.

**Input**: One or more Bug IDs (e.g. `BUG-004 BUG-007`), plus optionally a
disposition (`fixed` — the default — or `wont-fix` / `cannot-repro`) and a
one-line resolution note / fix reference (PR link or commit). If the Bug
IDs are omitted, auto-detect (see step 1).

---

## Steps

### 1. Resolve which bugs to close

**If the user provided Bug IDs**, use them directly — skip to step 2.

**Otherwise, auto-detect**: read `bugs-open.md` and list every row showing
`🔶 In Progress` (a `bug-fix` run that got as far as marking it in progress
but not archived), plus any showing `✅ Fixed` (an interrupted archive).
Present them:
- **4 or fewer**: `AskUserQuestion` with `multiSelect: true`, one option
  per candidate.
- **More than 4**: print the numbered list as plain text, then
  `AskUserQuestion` with "Close all listed" / "Close specific ones (I'll
  list the IDs)" / "Cancel", resolving "specific ones" from the free-text
  follow-up.

### 2. Read both files

Read `JPPhotoManagerWeb/docs/backlog/bugs-open.md` (source) and
`JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` (destination). If either
doesn't exist, there's nothing to archive — report and stop.

The write ordering below is deliberate and must not be reordered:
everything is **extracted read-only first**, then written to the
**destination**, and only after that save succeeds is the **source**
mutated in a single edit. If the skill is interrupted mid-run, the worst
case is content duplicated across both files, never lost.

### 3. For each selected Bug ID — validate

Find the row in `bugs-open.md`'s `## Bug List` table whose `Bug ID`
matches (the first cell is the link `[BUG-NNN](bugs/BUG-NNN.md)`). If it's
not there (already archived, or never existed), warn and skip that ID.
Check that `JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md` exists; if it
doesn't, warn (the Resolution line can't be recorded — suggest running
`bugs-status`) and ask whether to create it with just the H1
`# BUG-NNN — <title from the row's Summary>` or skip that ID.

### 4. Handle by disposition

**Disposition `wont-fix` or `cannot-repro`** — the bug stays in
`bugs-open.md`; it is *not* moved. In a single edit to `bugs-open.md`:
1. Change the row's **Status** to `🚫 Won't fix` or `❓ Cannot reproduce`.
2. Remove it from `## Recommended fix order` if listed.

Also append a `- **Resolution:** <note>` bullet to the end of
`bugs/BUG-NNN.md` (a separate file from `bugs-open.md`; if the file
already has a `**Resolution:**` bullet — an interrupted earlier run —
replace it rather than adding a second one).

(These states are still visible to `bugs-status` and deliberately kept in
the open file so a `❓ Cannot reproduce` bug that resurfaces is easy to
reopen. They're excluded from `bugs-next`'s candidate set.) Skip steps 5–6.

**Disposition `fixed`** (default) — proceed to step 5.

### 5. Write to bugs-fixed.md first

1. Copy the full table row for each fixed bug. **In the copy**, drop the
   `Status` column and set the `Fixed` column to today's date
   (`YYYY-MM-DD`) and the `Fix` column to the supplied PR link / commit
   (or leave `Fix` blank if none was given). The destination table's
   columns are `Bug ID | Severity | Area | Environment | Fixed | Fix |
   Summary` — reshape the row to match.
   The first cell keeps its link (`[BUG-NNN](bugs/BUG-NNN.md)`) — the
   file never moves, so the link stays valid.
2. Append each reshaped row directly under the last row of
   `bugs-fixed.md`'s `## Bug List` table (no blank line inside the
   table).
3. Append a `- **Resolution:** <note — what the fix changed, and the
   PR/commit reference>` bullet to the end of each bug's
   `JPPhotoManagerWeb/docs/backlog/bugs/BUG-NNN.md` (replace an existing
   `**Resolution:**` bullet instead of duplicating it, in case a previous
   run was interrupted). The file stays in `bugs/`; nothing is copied or
   moved.

Save `bugs-fixed.md` and the bug file(s). **Confirm the saves succeeded
before step 6.**

### 6. Now mutate bugs-open.md — a single edit, a single save

1. Delete each fixed bug's row from the `## Bug List` table.
2. Remove each fixed bug from `## Recommended fix order` if listed — it's
   a plain bullet list, so removing an entry needs no renumbering.
3. If the `## Bug List` table is now empty, restore the `_No open bugs._`
   placeholder line **after a blank line** following the header separator row
   (the skeleton's shape — `bug-report` removes both when it adds a row; a
   placeholder directly under the separator row would be read as a table
   row); if `## Recommended fix order` is now empty, restore
   `_No open bugs to order._`.

Save `bugs-open.md`.

### 7. Display summary

```
## Bugs Closed

- BUG-NNN `<title>` — <fixed → moved to bugs-fixed.md | 🚫 Won't fix | ❓ Cannot reproduce>
- …

Resolution lines appended to bugs/BUG-NNN.md: <count>
```

---

## Guardrails

- **Destination before source, always.** Never edit or save
  `bugs-open.md` until the corresponding write to `bugs-fixed.md` and the
  bug file in step 5 has been made and confirmed saved (for the `fixed`
  disposition). The `wont-fix` / `cannot-repro` dispositions skip step 5
  (they edit `bugs-open.md` and the bug file only).
- **The per-bug file never moves and never loses content** — only a
  `**Resolution:**` bullet is appended. Severity, Area, and Status are
  never written into it.
- Preserve the exact Markdown table formatting (pipe characters) in both
  files, and reshape the row to the destination table's column set — the
  destination has a `Fixed` date column and no `Status` column.
- If a selected Bug ID doesn't exist in `bugs-open.md`, report it and
  continue with the rest.
- Keep both files' section headers and structure intact — only add/remove
  table rows, never rewrite whole sections, and never append a details
  block to either table file (their `## Details` section holds only its
  pointer sentence).
- The auto-detection in step 1 is a convenience — the user always has the
  final say on which bugs to close and with what disposition.
- This skill never writes code, runs tests, or touches git. It records an
  outcome that a fix (or a decision) already produced.
