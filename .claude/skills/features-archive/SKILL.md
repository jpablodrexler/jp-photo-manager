---
name: features-archive
description: Mark features as implemented in the backlog's features-planned.md and move them to features-implemented.md (rewriting the row to the implemented-table shape, with a Details cell linking the feature's brief file and archived spec). Use when one or more features have been fully implemented and need to be archived.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Archive one or more implemented features from `<backlog>/features-planned.md` into `<backlog>/features-implemented.md`.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: One or more feature numbers (e.g. `3 7`) or names (e.g. `task-due-dates`). If omitted, auto-detect from archived SDD changes and prompt the user to confirm.

---

## Steps

### 1. Resolve which features to archive

**If the user provided numbers or names**, use them directly — skip to step 2.

**Otherwise, auto-detect from the archived SDD changes** (OpenSpec is optional: with no `openspec/changes/archive/` folder, go straight to the fallback in step d):

This project follows a workflow where each feature in `<backlog>/features-planned.md` is implemented via a corresponding OpenSpec (SDD) change. The full lifecycle is:
1. Feature added to `<backlog>/features-planned.md` with `⬜ Pending`
2. SDD change created under `openspec/changes/<change-name>/` with proposal, specs, design, and tasks artifacts
3. SDD change applied (code implemented and committed)
4. SDD change archived to `openspec/changes/archive/YYYY-MM-DD-<change-name>/` via `/opsx:archive`
5. This skill archives the matching feature in `<backlog>/features-planned.md`

To detect candidates at step 5:

a. **List archived SDD changes**: read all directory names under `openspec/changes/archive/`. Strip the leading `YYYY-MM-DD-` date prefix from each directory name to get the raw change names (e.g. `2026-06-21-task-due-dates` → `task-due-dates`).

b. **Cross-reference with <backlog>/features-planned.md**: read `<backlog>/features-planned.md`. For each archived change name, look for a row in the `## Feature List` table whose `Change name` column (the backtick-wrapped value, e.g. `` `task-due-dates` ``) matches.
Collect only the rows still showing `⬜ Pending` or `🔶 In Progress` in the Implementation column — rows already showing `✅ Implemented` are excluded.
The common case is `🔶 In Progress`, since `feature-development` marks a feature that way as soon as it starts (see that skill's Step 1.6) and this skill is normally invoked at the end of that same run;
`⬜ Pending` only shows up here for a row archived some other way (e.g. manually, or via an ad hoc change never marked in progress).

c. **Present candidates to the user**: `AskUserQuestion` only accepts 2–4 options per question (even with `multiSelect`), so how to present candidates depends on the count:
   - **4 or fewer matches**: use the **AskUserQuestion tool** with `multiSelect: true`, one option per candidate (number + name), and ask which to archive.
   - **More than 4 matches**: don't try to cram them into `AskUserQuestion`. Print the full numbered list as plain text first, then use **AskUserQuestion** with a small fixed set of options — "Archive all listed", "Archive specific ones (I'll list the numbers)", "Cancel" — and resolve "specific ones" from the user's free-text follow-up (matched against the printed list) rather than a second attempt to enumerate them through the tool.

d. **Fallback**: if no matches are found (all archived SDD changes either have no corresponding feature row or are already marked `✅ Implemented`), fall back to reading `docs/backlog/features-planned.md` and listing all `⬜ Pending` features. If the backlog is large, do not attempt to present dozens of pending features as `AskUserQuestion` options — print the full list as plain text and let the user pick by naming number(s)/name(s) directly, using **AskUserQuestion** only for the surrounding yes/no/cancel framing if needed.

### 2. Read both files

Read:
- `<backlog>/features-planned.md` (source)
- `<backlog>/features-implemented.md` (destination)

If either file doesn't exist yet, there is nothing to archive — report that to the user and stop (`feature-plan` creates both files' initial skeleton on first use; if neither has ever run, there's no backlog yet).

### 3. For each selected feature — validate it exists and is pending

In the `<backlog>/features-planned.md` table find the matching row: for a numeric selection, match the `#` column; for a name (e.g. `task-due-dates`, including how `feature-development`'s Phase 5 always calls this skill — with a name, never a number), match the `Change name` column (the backtick-wrapped value), the same way step 1b's auto-detection already does. If the row already shows `✅ Implemented` warn the user and skip it.

Also check the feature's brief file: `<backlog>/features/NNN-<change-name>.md` (`NNN` = the number zero-padded to three digits; the `[brief](…)` link in the row's `Brief` cell names it). It must exist; if it doesn't, warn the user — it is never created or edited here — and still archive the row.

Steps 4–8 are ordered for data safety: extract everything read-only, write the **destination** (`<backlog>/features-implemented.md`), and only after that save succeeds mutate the **source** (`<backlog>/features-planned.md`), once, in a single save. If the run is interrupted in between, content is duplicated, never lost. Do not reorder.

### 4. Extract the row from <backlog>/features-planned.md (read-only) and rewrite it in that copy

Copy the full table row (the `|` delimited line) for each selected feature. **In that copy** — not in the actual file — rewrite it from the planned shape (`# | Change name | Priority | Schema Change | Effort | Area | SDD Artifacts | Implementation | Summary | Brief`) to the implemented shape (`# | Change name | Priority | Schema Change | Effort | Area | Details`), reading the planned cells by header name, never by position:

- keep `#`, `Change name`, `Priority`, `Schema Change`, `Effort`, `Area` exactly as they are — plain text, the name still backtick-wrapped, no links;
- **drop** the `SDD Artifacts`, `Implementation`, `Summary` and `Brief` cells (an implemented row has no status columns and no summary — "implemented" is decided only by which table holds the row, so there is no `✅ Implemented` text to write);
- add the `Details` cell: `[brief](features/NNN-<change-name>.md) · [spec](<path to openspec/changes/archive/<archive-dir>/proposal.md>)`, paths relative to `<backlog>/` (so `../../openspec/changes/archive/…` when the backlog is `docs/backlog`;
  `NNN` = zero-padded feature number). `<archive-dir>` is the directory under the repo-root `openspec/changes/archive/` named `YYYY-MM-DD-<change-name>` — find it by listing that folder and matching the slug after the date prefix (if several match, take the most recent date).
  If no archive directory exists (the SDD change was never archived — e.g. an ad hoc feature), warn the user and write only the `[brief]` link, with no `[spec]` part.
  The `[brief]` link is the same one as the planned row's `Brief` cell;
- end the `Details` cell with `· archived YYYY-MM-DD`: the date the feature is archived, today's date unless the user says it was done on another day. It is written for every feature, with or without OpenSpec, and it is the date `backlog-report` and `backlog-forecast` use first, so a backlog without OpenSpec, a table loaded in one go or a shallow clone still gets a real date. Never guess it from the git history; if the date is not known, ask.

The finished row reads `| N | `name` | P | Schema | Effort | Area | [brief](features/NNN-<name>.md) · [spec](<path to openspec/changes/archive/<archive-dir>/proposal.md>) · archived YYYY-MM-DD |` (with no `[spec]` part when there is no archived change). This copy is what step 7 writes into `<backlog>/features-implemented.md`; do not defer the rewrite to step 8, since the planned row is about to be deleted there regardless. The feature's brief file under `<backlog>/features/` is never moved, copied or changed. Do not edit the actual `<backlog>/features-planned.md` file yet.

### 5. Extract associated dependency / implementation notes (read-only)

Scan the **Dependencies** and **Implementation notes** sections of `<backlog>/features-planned.md` for any blocks that reference the selected feature numbers. A block is:

- A top-level heading or bold label that mentions the feature number (e.g. `**Feature 3 — …**`, `**Feature 3 → …**`)
- Everything after that heading up to (but not including) the **next top-level `**Feature N …**`-style heading** — not simply the next blank line.

Some blocks span many paragraphs separated by blank lines and use nested italic sub-headers as internal structure rather than starting a new block — stopping at the first blank line would truncate the block and strand its remaining paragraphs behind in `<backlog>/features-planned.md` with no parent heading. Only a line matching the `**Feature N ...**` heading pattern ends the current block.

**Some headings reference more than one feature number** (e.g. `**Features 4, 5, 6 — no schema changes**`). For each block, record every feature number its heading mentions, not just the one that led you to it — this determines how it's handled in step 8.

**For a hard-dependency heading of the form `**Feature A → Feature B**`** (exactly one A and one B), also record which side is the *prerequisite*: B, the number after the arrow. The block's resolved/pending annotation says whether A is unblocked, which depends only on whether B has shipped, not on whether A has. Step 7 applies this.

Collect these blocks in memory. Do not edit `<backlog>/features-planned.md` yet.

**Compute `IMPLEMENTED_SET`** (needed by steps 7 and 8): every `#` value already present in `<backlog>/features-implemented.md`'s `## Feature List` table (read in step 2), plus the feature numbers selected for archiving this run. It is the full set of features that will be implemented after this run completes, not just this run's batch: a block's prerequisites are often satisfied across separate runs (e.g. `**Feature 4 → Feature 3**` when 3 was archived weeks before 4), so checking only this run's selection would never recognize such a block as resolved.

### 6. Extract migration table rows (read-only)

In the **Deployment (migration) dependencies** table, find the row(s) mapping a migration to a selected feature and copy them in memory. Not every feature has one — some are noted "no schema change" instead. Do not edit `<backlog>/features-planned.md` yet.

### 7. Write everything to <backlog>/features-implemented.md first

At the end of the `## Feature List` table in `<backlog>/features-implemented.md`, insert the implemented-shape row(s) built in step 4 (right after its last row — rows are grouped by archive batch, not sorted by number), keeping the table contiguous (no blank line inside it).

Under `## Dependencies (Historical)` → `### Implementation notes`, add the note blocks extracted in step 5 (a multi-feature-heading block that also stays in `<backlog>/features-planned.md` per step 8 — because not everything its heading references is in `IMPLEMENTED_SET` yet — is still added here too; duplication across the two files is fine, that's not what this step guards against). For each block:

- **If an equivalent block (same heading) doesn't already exist here** (i.e. this is the first archive run to touch it): append it. Decide whether it's resolved:
  - For a hard-dependency (`A → B`) block, check **only the prerequisite side, B**: if B is in `IMPLEMENTED_SET`, the dependency is resolved *regardless of whether A has shipped yet*.
  - For a non-directional multi-feature heading with no prerequisite/dependent distinction (e.g. `**Features 4, 5, 6 — no schema changes**`), it's resolved once every feature number in the heading is in `IMPLEMENTED_SET`.

  When resolved, mark it `(prerequisite already implemented)` (as on existing blocks in this file), preserving any existing `(soft)`/`(hard)` dependency-strength qualifier — that's a separate axis from resolved status and isn't touched by this — rather than leaving an unresolved-looking heading or copying in stale wording. Note this means an `A → B` block can be marked resolved while A's own row is still sitting `⬜ Pending`/`🔶 In Progress` in `features-planned.md` — that's correct: "resolved" here means "no longer blocking," not "both features are done."
- **If an equivalent block already exists here** (a prior run copied it over while it was unresolved): do not append a second copy.
  - If it is now resolved by the same per-kind test (B in `IMPLEMENTED_SET` for `A → B`; every heading number for a non-directional block), update its heading annotation to `(prerequisite already implemented)`.
  - Check its prose for an aside naming a feature as still pending (e.g. "*(Note: ... where it remains because #N is still pending.)*"). If that feature is now in `IMPLEMENTED_SET`, update or remove the sentence: a dependency note describes whether the prerequisite has shipped, and goes stale the moment it is archived.

For each migration row extracted in step 6, append it to the **Deployment (migration) dependencies (applied)** table in `<backlog>/features-implemented.md`.

Save `<backlog>/features-implemented.md`. **Confirm this save succeeded before proceeding to step 8** — per the Guardrails, `<backlog>/features-planned.md` must never be mutated until this is confirmed.

### 8. Now mutate <backlog>/features-planned.md — a single edit, a single save

With the destination safely written, make all of the following changes to `<backlog>/features-planned.md` and save it **once**:

1. Delete each selected feature's table row entirely from the `## Feature List` table (the implemented-shape version of it already landed in `<backlog>/features-implemented.md` in step 7 — this row, still showing `⬜ Pending` or `🔶 In Progress`, is simply removed, not rewritten; its brief file stays in `features/`).
2. Delete the dependency/note blocks extracted in step 5 — but **only the ones whose heading numbers are a full subset of `IMPLEMENTED_SET`** (not just this run's selection — see step 5's definition of `IMPLEMENTED_SET`, which also includes features already archived in earlier runs). If a block's heading references any feature number that is in neither `IMPLEMENTED_SET` nor being archived now (e.g. archiving #4 alone out of a `**Features 4, 5, 6 …**` block where 5 and 6 are still pending), leave that block in `<backlog>/features-planned.md` untouched — the features still pending there need it.
2a. For a hard-dependency (`A → B`) block left behind by rule 2 (A still pending) whose prerequisite B *is* in `IMPLEMENTED_SET`: update its annotation in place to `(prerequisite already implemented)`, as step 7 does for the copy in `features-implemented.md`. A's row may stay `⬜ Pending`/`🔶 In Progress` for a long time after B ships; the annotation must say "no longer blocking" the whole time.
3. Remove the migration rows extracted in step 6 from the **Deployment (migration) dependencies** table.
4. Update the **Recommended implementation order** block if it lists the archived features — remove them from the ordered list.
5. Remove the archived features from the **no hard dependencies** list in the Dependencies section if they appear there.

Save the updated `<backlog>/features-planned.md`.

### 9. Display summary

```
## Archive Complete

Archived N feature(s):
- #<n> `<name>` — moved to <backlog>/features-implemented.md
- …

Notes / dependency blocks moved: <count>
Migration rows moved: <count>
```

---

## Guardrails

- **Destination before source, always.** Never edit or save `<backlog>/features-planned.md` until the step 7 write to `<backlog>/features-implemented.md` is confirmed saved. Steps 4–6 extract read-only, step 7 writes the destination, step 8 is the sole mutation of the source: one edit, one save (never a separate save to flip a column and another to remove content).
- **The rewrite to the implemented shape happens in step 4's in-memory copy**, before step 7 writes it. The row left in `<backlog>/features-planned.md` still shows `⬜ Pending` or `🔶 In Progress` until step 8 deletes it; it is never rewritten there, only removed.
- **The feature's brief file never moves or changes.** `<backlog>/features/NNN-<change-name>.md` stays exactly where it is; archiving only moves the table row and links to it from the `Details` cell.
- If a selected feature number does not exist in `<backlog>/features-planned.md`, report an error for that entry and continue with the rest.
- Preserve the exact Markdown table formatting (column widths, pipe characters) in both files.
- The **Priority**, **Schema Change**, **Effort**, and **Area** columns carry over to the implemented row as-is; the planned-only `SDD Artifacts`, `Implementation`, `Summary` and `Brief` cells are dropped and replaced by one `Details` cell (step 4).
- Keep the section headers and structure of both files intact — only add/remove rows and note blocks, never rewrite entire sections.
- If the **Dependencies** section becomes empty after removals, leave the section header in place rather than deleting it.
- The auto-detection in step 1 is a convenience heuristic — the user always has the final say on which features to archive.
- **Judge a dependency block's resolved/pending status against `IMPLEMENTED_SET`** (this run's selection plus every feature already in `<backlog>/features-implemented.md`), never against this run's selection alone. Prerequisites are usually met across separate archive runs, so checking only the current batch leaves "still pending" claims stale in both files. Step 5 defines `IMPLEMENTED_SET`; step 7 says how an already-resolved block is updated instead of duplicated.
- **For a hard-dependency (`A → B`) block, "resolved" tracks only B (the prerequisite), never A.** A is unblocked the moment B ships, so a later `features-next` run must not see "still pending" next to it. Update the annotation as soon as B lands, in both files: step 7 for the copy in `features-implemented.md`, step 8 rule 2a for a copy left in `features-planned.md`, whether or not A has shipped.
- **Write the backlog files as UTF-8, and check it.** They hold characters outside ASCII (`✅`, `⬜`, `—`, `·`). Whatever writes them (editor tool, shell, script) must write UTF-8 explicitly, never the system's default encoding: a single-byte codepage turns one such character into an invalid byte and stops every script that reads the file. Prefer the editor tool to an ad-hoc script. After writing, read each changed file back as UTF-8 (e.g. `python3 -c "open(p, encoding='utf-8').read()"`); a decoding error is a failed write, to fix before going on.
- **Never run `git commit`, `git push` or open a PR.**
