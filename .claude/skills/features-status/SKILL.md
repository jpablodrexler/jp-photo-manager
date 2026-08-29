---
name: features-status
description: Reports feature tracking progress by counting rows in JPPhotoManagerWeb/docs/backlog/features-planned.md and JPPhotoManagerWeb/docs/backlog/features-implemented.md. Returns total, implemented, pending, and percent-complete counts, plus a priority-tier, effort, and artifacts-readiness breakdown of pending features, the pending feature names themselves, and any data-integrity issues found (duplicate feature numbers, stale dependency notes claiming a feature is still pending when it's actually already implemented). TRIGGER when the user asks for a feature status report, progress report, "features vs implemented features", how many features are done/pending, or similar summary requests about the feature backlog.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.5"
---

Report feature-tracking progress by counting rows in `JPPhotoManagerWeb/docs/backlog/features-planned.md`
(pending) and `JPPhotoManagerWeb/docs/backlog/features-implemented.md` (implemented).

**Input**: None required. The report always includes the priority-tier,
effort, and artifacts-readiness breakdown of pending features, the pending
list itself, and any data-integrity issues found.

---

## Steps

### 1. Count pending features

Read `JPPhotoManagerWeb/docs/backlog/features-planned.md`. Count the rows in the `## Feature List` table —
each row starts with `| <number> ` where `<number>` is the `#` column value.
Every row in this file is expected to show `⬜ Pending` or `🔶 In Progress`
in its Implementation column (`feature-development` flips a row to `🔶 In
Progress` once it starts work — see that skill's Step 1.6 — and
`features-archive` moves a row to `features-implemented.md` in the same pass
it flips the column to `✅ Implemented`, so a row should never sit here
already showing that). Don't just assume this — read the Implementation
column value for each row and produce two counts: `pending_strict` (rows
showing `⬜ Pending`) and `in_progress` (rows showing `🔶 In Progress`).
Define `pending = pending_strict + in_progress` — this combined figure is
what step 3's totals/percent math uses (neither state is implemented yet),
while step 6 displays `pending_strict` and `in_progress` as separate table
rows, since "in progress" is a materially different state worth surfacing
on its own. If any row shows `✅ Implemented` here, it means an archive pass
was interrupted before the row was moved, so report it separately rather
than folding it into any of the above.

### 2. Count implemented features

Read `JPPhotoManagerWeb/docs/backlog/features-implemented.md`. Count the rows in the `## Feature
List` table the same way as step 1 (`| <number> ` prefix).

Unlike `JPPhotoManagerWeb/docs/backlog/features-planned.md`, this file's table has **no Implementation column** — compare its header row against `JPPhotoManagerWeb/docs/backlog/features-planned.md`'s to confirm rather than assuming either way, since the two schemas could in principle change. As of this file's current schema, a row's mere presence in `JPPhotoManagerWeb/docs/backlog/features-implemented.md` *is* the implemented signal — `features-archive` only ever writes a row here once it's flipped, and there's no column value left to check. If a future version of the file *does* carry an Implementation column, then (and only then) verify each row shows `✅ Implemented` and flag any that don't, the same interrupted-archive case as step 1.

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

Either way, `percent` above is the exact, display-ready string for the **Progress** row in step 6 — it already includes the `%` sign (or the "no features tracked yet" note), so don't append another `%` when displaying it.

### 4. Priority and effort breakdown, and the pending list itself

This step covers every row from step 1 — both `⬜ Pending` and `🔶 In
Progress` — not just the strictly-pending ones; "how much is left to do"
includes work already underway.

Read the **Priority** column value (`P0`/`P1`/`P2`/`P3`) directly from each
such row in the `## Feature List` table. Tally how many fall under each
tier, and how many have the column blank or missing (treat as "No explicit
tier" rather than erroring). Also count how many have `SDD Artifacts` =
`✅ Created` (SDD artifacts already exist, ready to implement immediately)
vs `⬜ Pending`.

Read the **Effort** column value (`S`/`M`/`L`) the same way and tally per
size, plus "No explicit effort" for blank/missing — a bare pending *count*
doesn't say how much work is actually left, and the column is already being
read for nothing else.

Also record, for each such row, its `#`, `Change name`, `Priority`, and
whether it's `🔶 In Progress` — step 6's report lists these by name, not
just a count, since "3 pending" on its own isn't actionable without opening
the file to see which ones, and flags the in-progress one(s) distinctly so
they read as "already underway," not just next in line.

### 5. Data-integrity checks

These are read-only checks — per the Guardrails, this skill never edits
either file, only reports what it finds.

**Duplicate feature numbers.** Collect every `#` value from both files'
`## Feature List` tables into one combined list. If any number appears more
than once (whether twice within one file or once in each), flag it — this
usually means a manual-editing mistake (e.g. a copy-pasted row whose number
was never updated). Report the duplicated number(s) and which file(s) they
appear in.

**Stale "still pending" dependency notes.** Scan the **Dependencies**
section of `JPPhotoManagerWeb/docs/backlog/features-planned.md` and the
**Dependencies (Historical)** section of `JPPhotoManagerWeb/docs/backlog/features-implemented.md`
for anything asserting that a specific referenced feature is not yet
implemented. This project's dependency notes use a richer vocabulary than a
plain pending/resolved toggle, so check both forms rather than one fixed
string:

- A block heading annotation naming a still-pending state (e.g. `(prerequisite
  still pending)`) — most headings here instead only annotate the *resolved*
  case (`(prerequisite already implemented)`/`(prerequisite implemented)`),
  leaving an unresolved one unannotated or marked `(soft)`/`(hard)` for
  dependency strength rather than status, so this form is rare but still
  worth checking for.
- A **prose aside** stating a specific feature number is still pending — this
  is the actual common pattern here, e.g. "*(Note: this block is duplicated
  from `features-planned.md`, where it remains because #50 is still
  pending.)*". These asides exist specifically to explain why a block is
  duplicated across both files, and are exactly the kind of note that goes
  stale silently once the referenced feature is archived and nobody thinks
  to revisit the sentence.

For each such claim, check the referenced feature number against the
implemented set built in steps 1–2 (every `#` present in
`JPPhotoManagerWeb/docs/backlog/features-implemented.md`). If it's actually
already implemented, the claim is stale — flag it, quoting the claim and
naming which file(s) it appears in. Don't flag `(soft)`/`(hard)` labels or
an *absence* of a resolved-annotation — those describe dependency strength
or simply haven't been proactively updated yet, neither of which is an
assertion this check can prove false the way a specific "#N is still
pending" claim can.

This is exactly the kind of drift `features-archive`'s dependency-block
handling is supposed to prevent (see that skill's own step for the fix at
the source) — this check exists as a read-only safety net for cases the fix
doesn't catch, such as notes already left stale by past archive runs, or
the file having been edited by hand.

### 6. Display the report

Always display the summary table, the detail breakdowns, and the pending
list. If step 3 could not compute totals (a file was unavailable), replace
the `Total features`/`Progress` rows with a one-line note naming which file
was unavailable, and still show whichever of `Implemented`/`Pending` came
from the readable file. Only include the **Data integrity** section if step
5 actually found something — an empty "0 issues found" section on every
report would just be noise; omit the section entirely when clean.

```
## Feature Tracker Status

| Metric              | Count |
| -------------------- | ----- |
| Total features        | <total> |
| ✅ Implemented         | <implemented> |
| ⬜ Pending             | <pending_strict> |
| 🔶 In Progress         | <in_progress> |
| **Progress**           | **<percent>** |

### Pending breakdown by priority

| Tier | Count |
| ---- | ----- |
| P0 — production-safety gaps | <n> |
| P1 — high-impact             | <n> |
| P2 — scalability              | <n> |
| P3 — operational convenience  | <n> |
| No explicit tier              | <n> |

### Pending breakdown by effort

| Effort | Count |
| ------ | ----- |
| S      | <n> |
| M      | <n> |
| L      | <n> |
| No explicit effort | <n> |

### SDD Artifacts readiness

<n> of <pending> pending features already have SDD artifacts created and can
be implemented immediately without a propose step.

### Pending features

- #<n> `<change-name>` (<priority>) [append " — 🔶 In Progress" for a row flagged in_progress in step 4]
- …

[only if step 5 found something:]
### Data integrity

- ⚠ Duplicate feature number <n>: appears in <file(s)>.
- ⚠ Stale dependency note: "<quoted claim>" asserts feature #<n> is still
  pending, but it's already implemented. Found in <file(s)>.
```

---

## Guardrails

- Read-only skill — never modify `JPPhotoManagerWeb/docs/backlog/features-planned.md` or
  `JPPhotoManagerWeb/docs/backlog/features-implemented.md`, including the
  data-integrity issues step 5 finds — this skill only reports them. Fixing
  a stale dependency note or a duplicate number is a manual edit, or in the
  dependency-note case, `features-archive`'s job at the source (see that
  skill).
- Always recount from the current file contents; never reuse cached or
  remembered counts from a prior invocation.
- If either file is missing or has no `## Feature List` table, report that
  file as unavailable rather than guessing a count.
