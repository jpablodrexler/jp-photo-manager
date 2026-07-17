---
name: features-status
description: Reports feature tracking progress by counting rows in openspec/features.md and openspec/features-implemented.md. Returns total, implemented, pending, and percent-complete counts. TRIGGER when the user asks for a feature status report, progress report, "features vs implemented features", how many features are done/pending, or similar summary requests about the feature backlog.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Report feature-tracking progress by counting rows in `openspec/features.md`
(pending) and `openspec/features-implemented.md` (implemented).

**Input**: None required. Optional flag `--detail` to also break down pending
features by priority tier (P0–P3) from the `## Dependencies` section of
`openspec/features.md`.

---

## Steps

### 1. Count pending features

Read `openspec/features.md`. Count the rows in the `## Feature List` table —
each row starts with `| <number> ` where `<number>` is the `#` column value.
Every row in this file represents a pending feature (its Implementation
column always shows `⬜ Pending`), so the row count is the pending count.

### 2. Count implemented features

Read `openspec/features-implemented.md`. Count the rows in the `## Feature
List` table the same way. Every row here is `✅ Implemented`, so the row
count is the implemented count.

### 3. Compute totals

```
total     = pending + implemented
percent   = round(100 * implemented / total, 1)
```

### 4. (Optional) Priority breakdown — only if `--detail` was passed

Scan the `### Hard implementation dependencies` and the `P0`/`P1`/`P2`/`P3`
labeled subsections under `## Dependencies` in `openspec/features.md`. Tally
how many pending features fall under each explicit priority tier, and how
many have no explicit tier. Also count how many pending features have
`Artifacts` = `✅ Created` (SDD artifacts already exist, ready to implement
immediately) vs `⬜ Pending`.

### 5. Display the report

```
## Feature Tracker Status

| Metric              | Count |
| -------------------- | ----- |
| Total features        | <total> |
| ✅ Implemented         | <implemented> |
| ⬜ Pending             | <pending> |
| **Progress**           | **<percent>%** |
```

If `--detail` was requested, append:

```
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
