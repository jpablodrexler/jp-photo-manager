---
name: features-status
description: Reports feature tracking progress by counting rows in openspec/features.md and openspec/features-implemented.md. Returns total, implemented, pending, and percent-complete counts, plus a priority-tier and artifacts-readiness breakdown of pending features. TRIGGER when the user asks for a feature status report, progress report, "features vs implemented features", how many features are done/pending, or similar summary requests about the feature backlog.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Report feature-tracking progress by counting rows in `openspec/features.md`
(pending) and `openspec/features-implemented.md` (implemented).

**Input**: None required. The report always includes the priority-tier and
artifacts-readiness breakdown of pending features.

---

## Steps

### 1. Count pending features

Read `openspec/features.md`. Count the rows in the `## Feature List` table —
each row starts with `| <number> ` where `<number>` is the `#` column value.
Every row in this file is expected to be pending (its Implementation column
should always show `⬜ Pending`, since `features-archive` moves a row to
`features-implemented.md` in the same pass it flips the column). Don't just
assume this — read the Implementation column value for each row and count
only rows actually showing `⬜ Pending`; if any row shows `✅ Implemented`
here, it means that pass was interrupted before the row was moved, so report
it separately rather than silently folding it into the pending count.

### 2. Count implemented features

Read `openspec/features-implemented.md`. Count the rows in the `## Feature
List` table the same way, checking the Implementation column value rather
than assuming — every row here is expected to be `✅ Implemented`. Flag (but
still count) any row that isn't, for the same interrupted-archive reason as
step 1.

### 3. Compute totals

```
total     = pending + implemented
percent   = round(100 * implemented / total, 1)
```

### 4. Priority breakdown

Scan the `P0`/`P1`/`P2`/`P3` labeled subsections under `## Dependencies` in
`openspec/features.md` (the `### Hard implementation dependencies`
subsection is unrelated to priority — it lists blocking prerequisites
between features, not tier labels; don't scan it here). Tally how many
pending features fall under each explicit priority tier, and how many have
no explicit tier. Also count how many pending features have `Artifacts` =
`✅ Created` (SDD artifacts already exist, ready to implement immediately)
vs `⬜ Pending`.

### 5. Display the report

Always display both the summary table and the detail breakdown:

```
## Feature Tracker Status

| Metric              | Count |
| -------------------- | ----- |
| Total features        | <total> |
| ✅ Implemented         | <implemented> |
| ⬜ Pending             | <pending> |
| **Progress**           | **<percent>%** |

### Pending breakdown by priority

| Tier | Count |
| ---- | ----- |
| P0 — production-safety gaps | <n> |
| P1 — high-impact             | <n> |
| P2 — scalability              | <n> |
| P3 — operational convenience  | <n> |
| No explicit tier              | <n> |

### Artifacts readiness

<n> of <pending> pending features already have SDD artifacts created and can
be implemented immediately without a propose step.
```

---

## Guardrails

- Read-only skill — never modify `openspec/features.md` or
  `openspec/features-implemented.md`.
- Always recount from the current file contents; never reuse cached or
  remembered counts from a prior invocation.
- If either file is missing or has no `## Feature List` table, report that
  file as unavailable rather than guessing a count.
