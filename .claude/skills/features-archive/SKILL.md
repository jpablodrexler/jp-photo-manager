---
name: features-archive
description: Mark features as implemented in JPPhotoManagerWeb/docs/backlog/features-planned.md and move them to JPPhotoManagerWeb/docs/backlog/features-implemented.md. Use when one or more features have been fully implemented and need to be archived.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.3"
---

Archive one or more implemented features from `JPPhotoManagerWeb/docs/backlog/features-planned.md` into `JPPhotoManagerWeb/docs/backlog/features-implemented.md`.

**Input**: One or more feature numbers (e.g. `30 57`) or names (e.g. `image-rotation-viewer`). If omitted, auto-detect from archived SDD changes and prompt the user to confirm.

---

## Steps

### 1. Resolve which features to archive

**If the user provided numbers or names**, use them directly — skip to step 2.

**Otherwise, auto-detect from the archived SDD changes:**

This project follows a workflow where each feature in `JPPhotoManagerWeb/docs/backlog/features-planned.md` is implemented via a corresponding OpenSpec (SDD) change. The full lifecycle is:
1. Feature added to `JPPhotoManagerWeb/docs/backlog/features-planned.md` with `⬜ Pending`
2. SDD change created under `openspec/changes/<change-name>/` with proposal, specs, design, and tasks artifacts
3. SDD change applied (code implemented and committed)
4. SDD change archived to `openspec/changes/archive/YYYY-MM-DD-<change-name>/` via `/opsx:archive`
5. This skill archives the matching feature in `JPPhotoManagerWeb/docs/backlog/features-planned.md`

To detect candidates at step 5:

a. **List archived SDD changes**: read all directory names under `openspec/changes/archive/`. Strip the leading `YYYY-MM-DD-` date prefix from each directory name to get the raw change names (e.g. `2026-06-21-accent-color-customization` → `accent-color-customization`).

b. **Cross-reference with JPPhotoManagerWeb/docs/backlog/features-planned.md**: read `JPPhotoManagerWeb/docs/backlog/features-planned.md`. For each archived change name, look for a row in the `## Feature List` table whose `Change name` column (the backtick-wrapped value, e.g. `` `accent-color-customization` ``) matches. Collect only the rows still showing `⬜ Pending` in the Implementation column — rows already showing `✅ Implemented` are excluded.

c. **Present candidates to the user**: `AskUserQuestion` only accepts 2–4 options per question (even with `multiSelect`), so how to present candidates depends on the count:
   - **4 or fewer matches**: use the **AskUserQuestion tool** with `multiSelect: true`, one option per candidate (number + name), and ask which to archive.
   - **More than 4 matches**: don't try to cram them into `AskUserQuestion`. Print the full numbered list as plain text first, then use **AskUserQuestion** with a small fixed set of options — "Archive all listed", "Archive specific ones (I'll list the numbers)", "Cancel" — and resolve "specific ones" from the user's free-text follow-up (matched against the printed list) rather than a second attempt to enumerate them through the tool.

d. **Fallback**: if no matches are found (all archived SDD changes either have no corresponding feature row or are already marked `✅ Implemented`), fall back to reading `JPPhotoManagerWeb/docs/backlog/features-planned.md` and listing all `⬜ Pending` features. Given the size of this backlog (see the same 4-option cap noted in step c), do not attempt to present dozens of pending features as `AskUserQuestion` options — print the full list as plain text and let the user pick by naming number(s)/name(s) directly, using **AskUserQuestion** only for the surrounding yes/no/cancel framing if needed.

### 2. Read both files

Read:
- `JPPhotoManagerWeb/docs/backlog/features-planned.md` (source)
- `JPPhotoManagerWeb/docs/backlog/features-implemented.md` (destination)

### 3. For each selected feature — validate it exists and is pending

In the `JPPhotoManagerWeb/docs/backlog/features-planned.md` table find the matching row: for a numeric selection, match the `#` column; for a name (e.g. `image-rotation-viewer`, including how `feature-development`'s Phase 5 always calls this skill — with a name, never a number), match the `Change name` column (the backtick-wrapped value), the same way step 1b's auto-detection already does. If the row already shows `✅ Implemented` warn the user and skip it.

Steps 4–8 below are ordered deliberately: everything is *extracted (read-only)* first, then written to the **destination** file (`JPPhotoManagerWeb/docs/backlog/features-implemented.md`), and only after that save succeeds is the **source** file (`JPPhotoManagerWeb/docs/backlog/features-planned.md`) mutated — once, in a single save. This is a data-safety ordering, not an arbitrary one: if the skill gets interrupted (crash, killed session, disk error) between extraction and the final `JPPhotoManagerWeb/docs/backlog/features-planned.md` save, the worst case is that content is already safely duplicated in `JPPhotoManagerWeb/docs/backlog/features-implemented.md`, never that it existed only in memory and got lost when both files were already mutated. Do not reorder this so that `JPPhotoManagerWeb/docs/backlog/features-planned.md` is edited before `JPPhotoManagerWeb/docs/backlog/features-implemented.md` has been successfully saved.

### 4. Extract the row from JPPhotoManagerWeb/docs/backlog/features-planned.md (read-only) and flip it in that copy

Copy the full table row (the `|` delimited line) for each selected feature. **In that copy** — not in the actual file — change the Implementation column from `| ⬜ Pending |` to `| ✅ Implemented |`. This transformed copy is what step 7 writes into `JPPhotoManagerWeb/docs/backlog/features-implemented.md`, so it must already read `✅ Implemented` at that point; do not defer the flip to step 8, since the row `JPPhotoManagerWeb/docs/backlog/features-planned.md` holds is about to be deleted there regardless and any change made to it at that stage has no lasting effect. Do not edit the actual `JPPhotoManagerWeb/docs/backlog/features-planned.md` file yet.

### 5. Extract associated dependency / implementation notes (read-only)

Scan the **Dependencies** and **Implementation notes** sections of `JPPhotoManagerWeb/docs/backlog/features-planned.md` for any blocks that reference the selected feature numbers. A block is:

- A top-level heading or bold label that mentions the feature number (e.g. `**Feature 30 — …**`, `**Feature 30 → …**`)
- Everything after that heading up to (but not including) the **next top-level `**Feature N …**`-style heading** — not simply the next blank line.

Some blocks span many paragraphs separated by blank lines and use nested italic sub-headers (e.g. `*dev.samstevens.totp:totp (Maven, latest stable: 1.7.1)*`) as internal structure rather than starting a new block — stopping at the first blank line would truncate the block and strand its remaining paragraphs behind in `JPPhotoManagerWeb/docs/backlog/features-planned.md` with no parent heading. Only a line matching the `**Feature N ...**` heading pattern ends the current block.

**Some headings reference more than one feature number** (e.g. `**Features 41, 42, 43 — no schema changes**`, `**Features 46, 50, 53 — no schema changes**`). For each block, record every feature number its heading mentions, not just the one that led you to it — this determines how it's handled in step 8.

Collect these blocks in memory. Do not edit `JPPhotoManagerWeb/docs/backlog/features-planned.md` yet.

### 6. Extract migration table rows (read-only)

In the **Deployment (migration) dependencies** table, find the row(s) mapping a Flyway migration to a selected feature and copy them in memory. Not every feature has one — some are noted "no schema change" instead. Do not edit `JPPhotoManagerWeb/docs/backlog/features-planned.md` yet.

### 7. Write everything to JPPhotoManagerWeb/docs/backlog/features-implemented.md first

At the end of the `## Feature List` table in `JPPhotoManagerWeb/docs/backlog/features-implemented.md`, insert the table row(s) extracted in step 4.

Under `## Dependencies (Historical)` → `### Implementation notes`, append the note blocks extracted in step 5 (including any multi-feature-heading block that also stays in `JPPhotoManagerWeb/docs/backlog/features-planned.md` per step 8 — duplication across the two files is fine).

For each migration row extracted in step 6, append it to the **Deployment (migration) dependencies (applied)** table in `JPPhotoManagerWeb/docs/backlog/features-implemented.md`.

Save `JPPhotoManagerWeb/docs/backlog/features-implemented.md`. **Confirm this save succeeded before proceeding to step 8** — per the Guardrails, `JPPhotoManagerWeb/docs/backlog/features-planned.md` must never be mutated until this is confirmed.

### 8. Now mutate JPPhotoManagerWeb/docs/backlog/features-planned.md — a single edit, a single save

With the destination safely written, make all of the following changes to `JPPhotoManagerWeb/docs/backlog/features-planned.md` and save it **once**:

1. Delete each selected feature's table row entirely from the `## Feature List` table (the `✅ Implemented` version of it already landed in `JPPhotoManagerWeb/docs/backlog/features-implemented.md` in step 7 — this row, still showing `⬜ Pending`, is simply removed, not flipped).
2. Delete the dependency/note blocks extracted in step 5 — but **only the ones whose heading numbers are a full subset of the features being archived this run**. If a block's heading references any feature number that is *not* being archived now (e.g. archiving #46 alone out of a `**Features 46, 50, 53 …**` block), leave that block in `JPPhotoManagerWeb/docs/backlog/features-planned.md` untouched — the features still pending there need it.
3. Remove the migration rows extracted in step 6 from the **Deployment (migration) dependencies** table.
4. Update the **Recommended implementation order** block if it lists the archived features — remove them from the ordered list.
5. Remove the archived features from the **no hard dependencies** list in the Dependencies section if they appear there.

Save the updated `JPPhotoManagerWeb/docs/backlog/features-planned.md`.

### 9. Display summary

```
## Archive Complete

Archived N feature(s):
- #<n> `<name>` — moved to JPPhotoManagerWeb/docs/backlog/features-implemented.md
- …

Notes / dependency blocks moved: <count>
Migration rows moved: <count>
```

---

## Guardrails

- **Destination before source, always.** Never edit or save `JPPhotoManagerWeb/docs/backlog/features-planned.md` (flipping its Implementation column, deleting rows, or removing note/migration content) until the corresponding write to `JPPhotoManagerWeb/docs/backlog/features-implemented.md` in step 7 has been made and confirmed saved. This ordering (steps 4–6 extract read-only, step 7 writes the destination, step 8 is the sole mutation of the source) exists specifically so an interruption mid-run leaves data duplicated rather than lost — do not collapse or reorder it, and do not perform two separate saves to `JPPhotoManagerWeb/docs/backlog/features-planned.md` (one flipping the column, one removing content) — step 8 is one edit, one save.
- **The flip happens in step 4's in-memory copy, not in `JPPhotoManagerWeb/docs/backlog/features-planned.md`.** Step 4 must produce a copy that already reads `✅ Implemented` before step 7 writes it to `JPPhotoManagerWeb/docs/backlog/features-implemented.md` — flipping later (e.g. deferring it to step 8, against the actual `JPPhotoManagerWeb/docs/backlog/features-planned.md` row) would only mutate a copy that step 8 deletes moments later, leaving the row permanently archived as `⬜ Pending` in `JPPhotoManagerWeb/docs/backlog/features-implemented.md`. The row that stays behind in `JPPhotoManagerWeb/docs/backlog/features-planned.md` at step 8 is expected to still show `⬜ Pending` right up until it's deleted — it is never itself flipped, only removed.
- If a selected feature number does not exist in `JPPhotoManagerWeb/docs/backlog/features-planned.md`, report an error for that entry and continue with the rest.
- Preserve the exact Markdown table formatting (column widths, pipe characters) in both files.
- The **Priority**, **Schema Change**, **Effort**, and **Area** columns are part of the row copied in step 4 — no special handling needed, they carry over as-is. Older rows already in `features-implemented.md` show `—` for these (they predate this tracking); that's expected and not something to backfill while archiving.
- Keep the section headers and structure of both files intact — only add/remove rows and note blocks, never rewrite entire sections.
- If the **Dependencies** section becomes empty after removals, leave the section header in place rather than deleting it.
- The auto-detection in step 1 is a convenience heuristic — the user always has the final say on which features to archive.
