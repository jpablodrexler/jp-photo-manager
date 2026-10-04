---
name: features-status
description: Reports feature tracking progress by counting rows in JPPhotoManagerWeb/docs/backlog/features-planned.md and JPPhotoManagerWeb/docs/backlog/features-implemented.md. Returns total, implemented, pending, and percent-complete counts, plus a priority-tier, effort, and artifacts-readiness breakdown of pending features, the pending feature names themselves, and any data-integrity issues found (duplicate feature numbers, stale dependency notes claiming a feature is still pending when it's actually already implemented, and brief-file problems: a row without its features/NNN-<name>.md file, a file without a row, a wrong H1, a bad planned Summary, a missing [brief] link, a dangling [spec] link). TRIGGER when the user asks for a feature status report, progress report, "features vs implemented features", how many features are done/pending, or similar summary requests about the feature backlog.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.7"
---

Report feature-tracking progress by counting rows in `JPPhotoManagerWeb/docs/backlog/features-planned.md`
(pending) and `JPPhotoManagerWeb/docs/backlog/features-implemented.md` (implemented).

**Input**: None required. The report always includes the priority-tier,
effort, and artifacts-readiness breakdown of pending features, the pending
list itself, and any data-integrity issues found.

---

## Script

`scripts/status_report.py` owns every part of this skill that is pure
table-counting and text-pattern arithmetic — reading both backlog files'
`## Feature List` tables, computing the counts/percent breakdowns
(priority tier, effort, SDD-artifacts readiness, over the pending +
in-progress rows), and finding the data-integrity issues: duplicate
feature numbers across both files, and a stale dependency-note claim —
covering both this project's annotation forms (a rare heading annotation
naming an explicit still-pending state, and the actually-common prose
aside — "#N is still pending", "#N and #M are still pending", "(#N,
still pending)" — used to explain why a Dependencies block is duplicated
across both backlog files) — whose referenced feature is actually already
implemented; plus six checks of the rows against the one-file-per-feature
briefs under `JPPhotoManagerWeb/docs/backlog/features/` (rows are read by
header name, since the planned and implemented tables have different
column sets): a table row with no `features/NNN-<name>.md` file; a
`features/*.md` file with no row in either table; a file whose first line
is not `# Feature N — <name>`; a planned `Summary` that is empty or longer
than 250 characters; a row missing its `[brief](features/…)` link (the
`Brief` cell on planned rows, the `Details` cell on implemented rows); and
an implemented row whose `[spec](…)` target does not exist (reported as a
warning — e.g. the SDD change was never archived). Run it and display its
output directly rather than re-deriving any of this by hand:

```
python3 .claude/skills/features-status/scripts/status_report.py <repo-root>
```

Prints the finished `## Feature Tracker Status` report (summary table,
priority/effort/SDD-readiness breakdowns, the pending-feature list, and a
**Data integrity** section only when it found something) straight to
stdout — display it as-is. Add `--json` for the raw structured data
instead, if you need to reason about a specific field rather than just
show the report. If either backlog file is missing or has no `## Feature
List` table, the script itself reports the `Total`/`Progress` rows as
unavailable and names which file caused it — don't fall back to reading
either file by hand. See the script's own docstring/comments (and its
`find_stale_notes`/regex definitions specifically, for the dependency-note
check's exact patterns) for what it checks; it's the source of truth for
this skill's behavior now, not this file.

---

## Guardrails

- Read-only skill — never modify `JPPhotoManagerWeb/docs/backlog/features-planned.md` or
  `JPPhotoManagerWeb/docs/backlog/features-implemented.md`, including the
  data-integrity issues the script finds — this skill only reports them.
  Fixing a stale dependency note or a duplicate number is a manual edit,
  or in the dependency-note case, `features-archive`'s job at the source
  (see that skill).
- Always re-run the script against the current file contents; never
  reuse cached or remembered counts from a prior invocation.
- If either file is missing or has no `## Feature List` table, the
  script reports that file as unavailable rather than guessing a count —
  don't override that by reading the file by hand instead.
