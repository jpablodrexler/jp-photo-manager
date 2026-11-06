---
name: backlog-report
description: >-
  Reports what the backlog delivered over a period: features archived and bugs
  fixed per week, story points per week (velocity), the bugs fixed by
  severity, the list of what was delivered, and how many weeks the pending
  points would take at the recent pace. Feature dates come from the git
  history of features-implemented.md and bug dates from the Fixed column of
  bugs-fixed.md. TRIGGER when asked for a velocity, throughput or burndown
  report, "what did we ship this month", how fast the team is going, when the
  planned backlog could be done, or release-note material for a period. For
  the current state of the backlog (counts, breakdowns, integrity) use
  features-status / bugs-status instead.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Report what the backlog delivered over a period, from `<backlog>/features-implemented.md`, `<backlog>/features-planned.md` and `<backlog>/bugs-fixed.md`. Read-only: this skill never edits a backlog file.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: Optionally a period: a number of weeks ("the last 12 weeks"; default 8, the current week included) or a start date ("since 1 October"). Map what the user said to `--weeks N` or `--since YYYY-MM-DD`.

---

## Script

`scripts/backlog_report.py` does all the counting. Run it from this skill's base directory and show its output as it is, instead of re-deriving numbers by hand:

```
python3 scripts/backlog_report.py <repo-root> [--weeks N | --since YYYY-MM-DD]
```

It prints the finished `## Backlog Report`: a table per week (features, points, bugs fixed), the totals, the velocity (the average points per week over the last four complete weeks, with those four numbers and how many of the weeks had deliveries), the bug fixes per week over the same weeks, the pending features and points with the weeks they would take at that pace, a **Reliability** section when the data is doubtful, and the list of what was delivered.
Add `--json` for the structured data when you need one field, for example to draft release notes; its `warnings` list holds the same reliability notes with a `code` each.

How the dates are found, so you can explain a number when asked:

- **Bugs** use the `Fixed` date written by `bugs-archive`.
- **Features** have no date column in `features-implemented.md`. The report takes the date from, in this order:
  1. **The archived SDD change** the row links: the date in the folder name of `[spec](…/openspec/changes/archive/YYYY-MM-DD-<name>/…)` in the `Details` cell, which `features-archive` writes in OpenSpec projects. It is written in the file, so a table loaded in one go or a shallow clone cannot change it, and the change is archived at the end of `feature-development`, close to the end of the work. It is the local date of the machine that ran the archive, so it can be a day off.
  2. **The first commit that added the row** to `features-implemented.md`: the day it was archived into the file, not the day the work ended. Rows loaded in one go, archived in one batch or squashed together share a date, and a shallow clone gives every row the date of its only commit.
  3. **Today,** for a row not committed yet that links no spec.

  The "Feature dates" line says how many came from each source. The reliability checks about a shared date and a shallow clone concern only the dates taken from git.
- Weeks start on Monday; the current week is shown but left out of the velocity.
- A feature without story points is listed under "Without points" and counts as zero points; suggest estimating it with `feature-plan` rather than guessing a value.
- Bugs have no story points, so the velocity leaves their work out; the report gives them as a separate rate.

If a file is missing or has no table, the report names it as unavailable. Do not fall back to reading the file by hand, and do not invent dates.

## Reading the result

Say what the numbers do and do not support.

- **Say where the dates come from.** Figures built on archived-SDD dates are as good as those dates; figures built on git dates inherit every limit above. When both sources are mixed, the line says how many of each.
- **Start with the Reliability section when there is one.** Each note names a reason the weekly figures may not mean what they seem: a date shared by most features (the pace before it is unknown), features without points (the points understate the work), pending features without points (the projection is optimistic), a shallow clone, or features not committed yet. Say which apply before quoting any velocity, and do not present a number the notes undermine as a measurement.
- **A velocity of "none" is an answer, not a gap to fill.** It means no feature was archived in the last four complete weeks, and the report gives the date of the last one. Do not replace it with an average over a longer span, and do not read it as a slow pace: the work may have continued without being archived.
- **A projection from one or two complete weeks is a guess,** and the story-point scale is ordinal, not proportional; present "about N weeks" as an order of magnitude. When the period has fewer than two complete weeks, say there is not enough history instead of quoting a velocity.
- **Look at the four weekly numbers, not only their average.** One batch week and three empty ones average to the same figure as four steady weeks.
- **Mention the bug fixes** when they are a large share of the work, since the velocity does not count them.
- **For a shallow clone,** suggest `git fetch --unshallow` and a second run before trusting the dates.

---

## Guardrails

- Read-only. Never edit `features-implemented.md`, `features-planned.md`, `bugs-fixed.md` or any other file.
- Always run the script against the current files; never reuse numbers from an earlier run.
- **Never run `git commit`, `git push` or open a PR.**
