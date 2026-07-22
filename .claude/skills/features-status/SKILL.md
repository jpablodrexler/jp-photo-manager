---
name: features-status
description: Reports feature tracking progress by counting rows in JPPhotoManagerWeb/docs/backlog/features-planned.md and JPPhotoManagerWeb/docs/backlog/features-implemented.md. Returns total, implemented, pending, and percent-complete counts, plus a priority-tier and artifacts-readiness breakdown of pending features. TRIGGER when the user asks for a feature status report, progress report, "features vs implemented features", how many features are done/pending, or similar summary requests about the feature backlog.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.3"
---

Report feature-tracking progress by counting rows in `JPPhotoManagerWeb/docs/backlog/features-planned.md`
(pending) and `JPPhotoManagerWeb/docs/backlog/features-implemented.md` (implemented).

**Input**: None required. The report always includes the priority-tier and
artifacts-readiness breakdown of pending features.

---

## Steps

### 1. Count pending features

Read `JPPhotoManagerWeb/docs/backlog/features-planned.md`. Count the rows in the `## Feature List` table —
each row starts with `| <number> ` where `<number>` is the `#` column value.
Every row in this file is expected to be pending (its Implementation column
should always show `⬜ Pending`, since `features-archive` moves a row to
`features-implemented.md` in the same pass it flips the column). Don't just
assume this — read the Implementation column value for each row and count
only rows actually showing `⬜ Pending`; if any row shows `✅ Implemented`
here, it means that pass was interrupted before the row was moved, so report
it separately rather than silently folding it into the pending count.

### 2. Count implemented features

Read `JPPhotoManagerWeb/docs/backlog/features-implemented.md`. Count the rows in the `## Feature
List` table the same way, checking the Implementation column value rather
than assuming — every row here is expected to be `✅ Implemented`. Flag (but
still count) any row that isn't, for the same interrupted-archive reason as
step 1.

### 3. Compute totals

If either `JPPhotoManagerWeb/docs/backlog/features-planned.md` or `JPPhotoManagerWeb/docs/backlog/features-implemented.md` was reported unavailable in step 1/2 (missing file, or no `## Feature List` table), do not compute `total`/`percent` at all — one of the two counts is unknown, so a number here would misrepresent it as zero rather than "unknown." Report the total/progress line as unavailable too, and say which file caused it, then still display whatever counts and breakdowns *are* available from the readable file.

Otherwise:

```
total = pending + implemented
if total == 0:
    percent = "0% — no features tracked yet"  (both files are legitimately empty; do not divide by zero)
else:
    percent = "<round(100 * implemented / total, 1)>%"
```

Either way, `percent` above is the exact, display-ready string for the **Progress** row in step 5 — it already includes the `%` sign (or the "no features tracked yet" note), so don't append another `%` when displaying it.

### 4. Priority breakdown

Read the **Priority** column value (`P0`/`P1`/`P2`/`P3`) directly from each
pending row in the `## Feature List` table. Tally how many pending features
fall under each tier, and how many have the column blank or missing (treat
as "No explicit tier" rather than erroring). Also count how many pending
features have `SDD Artifacts` = `✅ Created` (SDD artifacts already exist, ready
to implement immediately) vs `⬜ Pending`.

### 5. Display the report

Always display both the summary table and the detail breakdown. If step 3 could not compute totals (a file was unavailable), replace the `Total features`/`Progress` rows with a one-line note naming which file was unavailable, and still show whichever of `Implemented`/`Pending` came from the readable file:

```
## Feature Tracker Status

| Metric              | Count |
| -------------------- | ----- |
| Total features        | <total> |
| ✅ Implemented         | <implemented> |
| ⬜ Pending             | <pending> |
| **Progress**           | **<percent>** |

### Pending breakdown by priority

| Tier | Count |
| ---- | ----- |
| P0 — production-safety gaps | <n> |
| P1 — high-impact             | <n> |
| P2 — scalability              | <n> |
| P3 — operational convenience  | <n> |
| No explicit tier              | <n> |

### SDD Artifacts readiness

<n> of <pending> pending features already have SDD artifacts created and can
be implemented immediately without a propose step.
```

---

## Guardrails

- Read-only skill — never modify `JPPhotoManagerWeb/docs/backlog/features-planned.md` or
  `JPPhotoManagerWeb/docs/backlog/features-implemented.md`.
- Always recount from the current file contents; never reuse cached or
  remembered counts from a prior invocation.
- If either file is missing or has no `## Feature List` table, report that
  file as unavailable rather than guessing a count.
