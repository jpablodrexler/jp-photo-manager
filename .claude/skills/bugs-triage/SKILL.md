---
name: bugs-triage
description: >-
  Triages the open bugs in the backlog: finds the ones bug-fix could not start
  from (no usable steps to reproduce, no Expected or Actual), possible
  duplicates of each other, open bugs that look like one already fixed, vague
  summaries, and bugs missing from the recommended fix order, then proposes
  what to do about each. Read-only: it never edits the backlog; the fixes go
  through bug-report, bugs-archive and bugs-next once the user agrees.
  TRIGGER when asked to triage, clean up, refine or review the bug backlog, to
  find duplicate bugs, or to check which bugs are ready to be fixed. For
  counts and progress use bugs-status instead.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Triage the open bugs in `<backlog>/bugs-open.md` against their files in `<backlog>/bugs/` and `<backlog>/bugs-fixed.md`, and propose what to fix in the backlog itself. Read-only.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: None required. Optionally a looser or stricter duplicate match ("be more aggressive finding duplicates"): map it to `--threshold` (default 0.5, between 0 and 1).

---

## Script

`scripts/triage.py` does the checking. Run it from this skill's base directory:

```
python3 scripts/triage.py <repo-root> [--threshold 0.5] [--json]
```

It prints the `## Bug Triage` report: one list per finding. The duplicate and regression matches compare the words of the summaries, so they are candidates to look at, never a verdict.

## Proposing actions

After showing the report, read the files of the bugs it names and propose, per finding:

- **No usable repro / no Expected or Actual** — ask the user for the missing detail, or offer to try to reproduce it. The detail goes into `<backlog>/bugs/BUG-NNN.md` as the user confirms it; `bug-fix` stops on `FIX_BLOCKED — cannot reproduce` without it.
- **Possible duplicate** — read both files and say whether they describe the same symptom and cause. If they do, offer to close the newer one with `bugs-archive <BUG-ID> wont-fix "duplicate of BUG-NNN"`.
- **Looks like a fixed bug** — read the fixed bug's Resolution line and say whether this is a regression, a bug the fix missed, or a coincidence. Offer a note in the open bug's file pointing to the earlier one.
- **Vague summary** — propose a one-sentence rewrite naming the route or component and the visible symptom (see `bug-report`).
- **Not in the fix order** — suggest running `bugs-next`, which re-derives the order.

Ask before every change (**AskUserQuestion**, one decision at a time when there are few findings; a printed list and a "which ones?" question when there are many). Do the change through the skill that owns it; do not edit the backlog files from here.

---

## Guardrails

- Read-only: this skill never edits a backlog file. Closing, completing or reordering a bug is the job of `bugs-archive`, `bug-report` and `bugs-next`, and only after the user agrees.
- Say plainly when a finding is a guess (a similarity score is not proof) and never close a bug as a duplicate without reading both files.
- If `bugs-open.md` is missing or has no table, report that and stop.
- **Never run `git commit`, `git push` or open a PR.**
