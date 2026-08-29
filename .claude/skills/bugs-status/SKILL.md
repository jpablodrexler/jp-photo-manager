---
name: bugs-status
description: Reports bug-backlog progress by counting rows in JPPhotoManagerWeb/docs/backlog/bugs-open.md and JPPhotoManagerWeb/docs/backlog/bugs-fixed.md. Returns total, fixed, open, in-progress, and percent-closed counts, plus a severity, area, and environment breakdown of open bugs, the open bug list itself, and any data-integrity issues (duplicate Bug IDs, orphaned Details blocks, stale statuses). TRIGGER when the user asks for a bug status report, how many bugs are open/fixed, a bug backlog summary, or similar. This is the bug-family counterpart to features-status.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Report bug-tracking progress by counting rows in
`JPPhotoManagerWeb/docs/backlog/bugs-open.md` (open) and
`JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` (fixed). Read-only — this
skill never edits either file.

**Input**: None required.

---

## Steps

### 1. Count open bugs

Read `JPPhotoManagerWeb/docs/backlog/bugs-open.md`. If it doesn't exist,
treat it as unavailable in step 3 (nothing has been filed via `bug-report`
yet) rather than erroring. From the `## Bug List` table, read each row's
**Status** column and produce:

- `open_strict` — rows showing `⬜ Open`
- `in_progress` — rows showing `🔶 In Progress`
- `wont_fix` — rows showing `🚫 Won't fix`
- `cannot_repro` — rows showing `❓ Cannot reproduce`
- `fixed_here` — rows still showing `✅ Fixed` (an archive pass was
  interrupted before `bugs-archive` moved the row — report separately)

Define `active = open_strict + in_progress` — the count still needing
work. `wont_fix` and `cannot_repro` are resolved-without-a-fix and are
reported on their own line, not folded into `active`.

### 2. Count fixed bugs

Read `JPPhotoManagerWeb/docs/backlog/bugs-fixed.md`. If it doesn't exist,
treat it the same way as step 1's missing-file case. Count the rows in its
`## Bug List` table. A row's mere presence in this file is the fixed
signal — `bugs-archive` only ever writes a row here once it's resolved.

### 3. Compute totals

If either file was unavailable in step 1/2, do not compute
`total`/`percent` — report that line as unavailable, name the file that
caused it, and still show whatever counts are available from the readable
file.

Otherwise:

```
total   = active + wont_fix + cannot_repro + fixed
resolved = wont_fix + cannot_repro + fixed
if total == 0:
    percent = "0% — no bugs tracked yet"
else:
    percent = "<round(100 * resolved / total, 1)>% resolved"
```

### 4. Breakdowns and the open list

Over the `active` rows (both `⬜ Open` and `🔶 In Progress`):

- **By severity**: tally S1 / S2 / S3 / S4, plus "No explicit severity"
  for a blank/invalid cell.
- **By area**: tally per distinct `Area` value.
- **By environment**: tally `local` / `deployed` / `both`.

Also record, per active row, its `Bug ID`, `Summary`, `Severity`,
`Environment`, and whether it's `🔶 In Progress` — step 6 lists these by
ID, since a bare count isn't actionable.

### 5. Data-integrity checks (read-only)

- **Duplicate Bug IDs.** Collect every `Bug ID` from both files' tables.
  Flag any that appears more than once, naming the file(s).
- **Orphaned / missing Details blocks.** Every `⬜ Open` / `🔶 In
  Progress` row in `bugs-open.md` should have a matching
  `### BUG-NNN — …` block under `## Details`; every Details block should
  have a matching table row. Flag either mismatch.
- **Stale `✅ Fixed` in bugs-open.md** — any row from step 1's
  `fixed_here` count: report it as an interrupted archive that
  `bugs-archive <id>` should finish.

### 6. Display the report

Always show the summary table, the breakdowns, and the open list. If step
3 could not compute totals, replace the `Total`/`Progress` rows with a
one-line note naming the unavailable file. Only include the **Data
integrity** section if step 5 found something.

```
## Bug Tracker Status

| Metric           | Count |
| ---------------- | ----- |
| Total bugs        | <total> |
| ✅ Fixed           | <fixed> |
| 🚫 Won't fix       | <wont_fix> |
| ❓ Cannot reproduce | <cannot_repro> |
| ⬜ Open            | <open_strict> |
| 🔶 In Progress     | <in_progress> |
| **Progress**       | **<percent>** |

### Open bugs by severity

| Severity | Count |
| -------- | ----- |
| S1 — blocker  | <n> |
| S2 — major    | <n> |
| S3 — minor    | <n> |
| S4 — trivial  | <n> |
| No explicit severity | <n> |

### Open bugs by area

| Area | Count |
| ---- | ----- |
| <area> | <n> |
| …      | … |

### Open bugs by environment

| Environment | Count |
| ----------- | ----- |
| local    | <n> |
| deployed | <n> |
| both     | <n> |

### Open bugs

- BUG-NNN `<severity>` (<env>) — <summary> [append " — 🔶 In Progress" where applicable]
- …

[only if step 5 found something:]
### Data integrity

- ⚠ Duplicate Bug ID <id>: appears in <file(s)>.
- ⚠ BUG-NNN has a table row but no Details block (or vice versa).
- ⚠ BUG-NNN still shows ✅ Fixed in bugs-open.md — run `bugs-archive BUG-NNN` to finish the move.
```

---

## Guardrails

- Read-only skill — never modify `bugs-open.md` or `bugs-fixed.md`,
  including the data-integrity issues step 5 finds. This skill only
  reports them.
- Always recount from the current file contents; never reuse remembered
  counts from a prior run.
- If either file is missing or has no `## Bug List` table, report that
  file as unavailable rather than guessing a count.
