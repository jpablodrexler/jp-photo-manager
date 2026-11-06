---
name: features-triage
description: >-
  Triages the planned features in the backlog: finds the ones without story
  points, the large ones that should be split, briefs that are not a user
  story or have no acceptance criteria, features whose hard dependency has
  been implemented (ready to start) or points at a feature that does not
  exist, features marked In Progress with no branch, possible duplicates, and
  features missing from the implementation order, then proposes what to do
  about each. Read-only: changes go through feature-plan, features-next and
  features-archive once the user agrees. TRIGGER when asked to triage,
  refine or review the feature backlog, to find features that are not ready
  to build, to split big features, or to check what has been unblocked. For
  counts and progress use features-status instead.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Triage the pending and in-progress features in `<backlog>/features-planned.md` against their briefs in `<backlog>/features/`, `<backlog>/features-implemented.md` and the git branches, and propose what to fix in the backlog itself. Read-only.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: None required. Optionally a looser or stricter duplicate match: map it to `--threshold` (default 0.6, between 0 and 1).

---

## Script

`scripts/triage.py` does the checking. Run it from this skill's base directory:

```
python3 scripts/triage.py <repo-root> [--threshold 0.6] [--json]
```

It prints the `## Feature Triage` report, one list per finding. The duplicate matches compare the words of the summaries, so they are candidates to look at, never a verdict. The "no branch found" check looks at local and remote branch names only; it says so when the directory is not a git repository.

## Proposing actions

After showing the report, read the briefs of the features it names and propose, per finding:

- **No story points / t-shirt size** — offer to estimate with the rubric `feature-plan` uses, and ask for the value; never write one silently.
- **Large (8 or 13)** — read the brief and propose how to split it into stories of 5 or less, each with its own acceptance criteria, as separate `feature-plan` runs.
- **Not a user story / no acceptance criteria** — draft the missing part from the brief and the summary and ask before it is written into the brief.
- **Now unblocked** — say so; `features-next` will rank it normally. Suggest `features-archive`'s dependency-note update if the note still reads "still pending".
- **Blocked on a feature that does not exist** — ask whether the prerequisite was renamed, never planned or archived without a row, and propose the correction.
- **In Progress without a branch** — ask whether the work was abandoned or lives on another machine; offer to set the row back to `⬜ Pending` only if the user says it was abandoned.
- **Possible duplicate** — read both briefs and say whether they describe the same behaviour; if they do, propose merging them into one story and dropping the other.
- **Not in the order** — suggest re-deriving the order with `features-next`.

Ask before every change (**AskUserQuestion**, one decision at a time when there are few findings; a printed list and a "which ones?" question when there are many). Make the change through the skill that owns it, or as a plain edit to the brief or row the user approved; never edit anything the user has not seen.

---

## Guardrails

- Read-only until the user approves a specific change. Closing, splitting, estimating or reordering is the job of `feature-plan`, `features-next` and `features-archive`.
- Say plainly when a finding is a guess (a similarity score is not proof) and never merge or drop a feature without reading both briefs.
- If `features-planned.md` is missing or has no table, report that and stop.
- **Never run `git commit`, `git push` or open a PR.**
