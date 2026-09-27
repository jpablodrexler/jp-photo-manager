---
name: bugs-status
description: Reports bug-backlog progress by counting rows in JPPhotoManagerWeb/docs/backlog/bugs-open.md and JPPhotoManagerWeb/docs/backlog/bugs-fixed.md. Returns total, fixed, open, in-progress, and percent-closed counts, plus a severity, area, and environment breakdown of open bugs, the open bug list itself, and any data-integrity issues (duplicate Bug IDs, orphaned Details blocks, stale statuses). TRIGGER when the user asks for a bug status report, how many bugs are open/fixed, a bug backlog summary, or similar. This is the bug-family counterpart to features-status.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
---

Report bug-tracking progress by counting rows in
`JPPhotoManagerWeb/docs/backlog/bugs-open.md` (open) and
`JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` (fixed). Read-only — this
skill never edits either file.

**Input**: None required.

---

## Script

`scripts/status_report.py` owns every part of this skill that is pure
table-counting and text-pattern arithmetic — reading both backlog
files' `## Bug List` tables (from `JPPhotoManagerWeb/docs/backlog/bugs-open.md`
and `JPPhotoManagerWeb/docs/backlog/bugs-fixed.md`), computing the
counts/percent/breakdowns (severity/area/environment, over the active —
`⬜ Open` + `🔶 In Progress` — rows), and finding the data-integrity
issues (duplicate Bug IDs across both files, a `## Details` block with
no matching table row or vice versa, a leftover `✅ Fixed` row in
`bugs-open.md` from an interrupted `bugs-archive` run). Run it and
display its output directly rather than re-deriving any of this by hand:

```
python3 .claude/skills/bugs-status/scripts/status_report.py <repo-root>
```

Prints the finished `## Bug Tracker Status` report (summary table,
per-category breakdowns — including the `local`/`deployed`/`both`
environment split — the open-bug list, and a **Data integrity** section
only when it found something) straight to stdout — display it as-is.
Add `--json` for the raw structured data instead of the rendered
markdown. If either backlog file is missing or has no `## Bug List`
table, the script itself reports the `Total`/`Progress` rows as
unavailable and names which file caused it — don't fall back to reading
either file by hand. See the script's own docstring/comments for exactly
what it checks; it's the source of truth for this skill's behavior now,
not this file.

---

## Guardrails

- Read-only skill — never modify `bugs-open.md` or `bugs-fixed.md`,
  including the data-integrity issues the script finds. This skill only
  reports them.
- Always re-run the script against the current file contents; never
  reuse remembered counts from a prior run.
- If either file is missing or has no `## Bug List` table, the script
  reports that file as unavailable rather than guessing a count — don't
  override that by reading the file by hand instead.
