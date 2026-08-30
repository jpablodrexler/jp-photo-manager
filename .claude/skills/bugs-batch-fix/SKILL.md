---
name: bugs-batch-fix
description: >
  Runs bug-fix unattended across a user-given, ordered list of Bug IDs —
  or the whole open bug backlog — one bug at a time, all on a single
  shared fix/ branch with zero git commits at any point. Built for
  leaving Claude grinding through the S3/S4 tail (or a triaged batch)
  over a long unattended stretch, checking in only when a bug genuinely
  needs a human decision. Writes a dated, incrementally-updated report to
  JPPhotoManagerWeb/docs/reports/bug-batch/ so progress survives a
  session cutoff or crash. TRIGGER when the user asks to batch-fix bugs,
  work through the bug backlog unattended, or fix a list of named
  BUG-NNNs in one run (e.g. "fix BUG-004, BUG-007 and BUG-009 while I'm
  out", "grind the whole open bug backlog overnight"). Also TRIGGER when
  the user hands over several bugs at once and wants them all fixed in
  one session (e.g. "plan and fix these few bugs", "here are 3 bugs, sort
  them out") — file each with bug-report first, then run this skill (or
  bug-fix per bug) over the new ids rather than hand-rolling a multi-bug
  fix. This is the bug-family counterpart to features-batch-development.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Run `bug-fix` in a loop over a list of Bug IDs, all on one shared `fix/`
branch, never committing, writing progress to a resumable report as each
bug closes. Mirrors `features-batch-development` exactly, one file and one
prefix over.

**Input**: Either (a) an ordered list of Bug IDs (e.g. "BUG-004, BUG-007,
BUG-009"), or (b) a request for **the whole open backlog** ("fix every
open bug", "the whole bug backlog"). Optionally a branch name to use or
resume; if omitted, one is proposed and confirmed.

---

## Why this exists, and the one constraint that shapes everything

`bug-fix` already does the full single-bug lifecycle end to end and
already never commits. Its Phase 1, though, creates a *fresh* `fix/<slug>`
branch per bug and hard-blocks on a dirty tree — which the second bug's
uncommitted first-bug work would trigger. `bug-fix` accepts
`--skip-branch-setup` for exactly this; this skill is its only intended
caller. Every step below satisfies the two constraints: **one shared
branch across the whole run**, and **zero commits, ever**.

One accepted consequence of no-commit: `bug-fix`'s Phase 2 reviews scope
to `git status --porcelain`, which — from bug #2 onward — includes every
earlier bug's still-uncommitted files. Later bugs' reviews re-scan earlier
bugs' already-fixed code. Unavoidable without committing between bugs
(which the user has ruled out), not a defect to fix here.

---

## Steps

### 1. Resolve the bug list

**Named IDs** (form (a)): parse into an ordered list. Resolve each against
`JPPhotoManagerWeb/docs/backlog/bugs-open.md`'s `## Bug List` table by
`Bug ID`.
- Resolves to nothing (not in `bugs-open.md` or `bugs-fixed.md`) → stop
  and ask the user to correct it.
- Resolves to a row in `bugs-fixed.md` (already closed) → tell the user
  and ask whether to skip or genuinely re-open+re-run it.
- Resolves to a `🚫 Won't fix` / `❓ Cannot reproduce` row → tell the user
  and ask whether to skip or override.

**Whole backlog** (form (b)): take every row in `bugs-open.md`'s table
whose Status is `⬜ Open` or `🔶 In Progress`. Order them:
1. If `## Recommended fix order` has real content (not `_No open bugs to
   order._`), follow it exactly.
2. Otherwise order by severity tier (S1 → S4), then bug number.

An empty open backlog is not an error — say there's nothing to fix and
stop.

Because "the whole backlog" is derived, **show the resolved, ordered list
and confirm it** before proceeding (a plain confirmation is enough).

Build the final ordered list of Bug IDs — "the requested list".

### 2. Determine the shared branch, and detect a resumable run

Check `JPPhotoManagerWeb/docs/reports/bug-batch/` for a
`BUG_BATCH_REPORT_*.md` whose recorded "Requested bugs" list overlaps
substantially with the new one.

- **Exact / near-exact match, not fully checked off** → resume. Read which
  bugs it marks `✅ Closed`; skip those in Step 4. Use the branch name in
  its header — do not create or re-propose one. `git checkout` that branch
  if not already on it (do not create it — it must exist from the
  interrupted run; if it doesn't, stop and tell the user).
- **Partial / ambiguous overlap** → don't guess; ask the user whether this
  continues that report or is a new run.
- **No match (fresh start)** → `git status` first; dirty tree → stop and
  report (never stash/discard). Then pick the candidate branch name: one
  the user supplied (normalized to `fix/<name>` the way `gitflow`
  normalizes), or the default `fix/bug-batch-<YYYY-MM-DD>`.
  Check whether it already exists locally or on the remote
  (`git rev-parse --verify --quiet <name>`; `git ls-remote --heads origin
  <name>`).
  - **Exists** → warn the user by name and ask (**AskUserQuestion**)
    whether to continue on it as-is (via `gitflow`'s start-fix action,
    which resumes + syncs an existing branch) or stop so they can pick a
    different name / clean up.
  - **Doesn't exist** → confirm the candidate name via **AskUserQuestion**
    before creating anything.
  Once confirmed, invoke `gitflow` with "start fix `<name>`" — the **only**
  branch-creation point in this skill. If `gitflow` raises its own
  already-on-a-topic-branch question, let it surface to the user.

Confirm `git branch --show-current` matches the intended branch before
Step 3.

### 3. Initialize or resume the report

**Fresh run**: write the report now (path/format below), listing every
requested Bug ID as `⬜ Not started`, plus a header with the branch name,
the full requested list, and a start timestamp.

**Resume**: reopen the existing report. Never overwrite it or its
already-`✅ Closed` entries — only append/update from where it left off.

### 4. The loop

For each Bug ID in the requested list not already `✅ Closed`, in order:

- **In Progress** — update the entry to `🔄 In progress` **immediately,
  before** invoking `bug-fix` (this write happens first). No summary text
  yet — it exists purely so a crash leaves a trace of what was underway.
  Then invoke `bug-fix` via the **Skill** tool, passing `<BUG-ID>
  --skip-branch-setup`. A direct Skill invocation, executing inline this
  turn — never a backgrounded Agent call — so any `AskUserQuestion`
  `bug-fix` raises (Phase 0 confirmation, the cannot-reproduce decision,
  a `PROD_CODE_FIXED` choice, blocker guidance) reaches the user. Wait for
  it to finish, then:
  - **Closed** — `bug-fix` displayed its "Bug Fix Complete" summary ending
    in the bug closed via `bugs-archive`. Write a ≤2-sentence summary (root
    cause + what changed) and update the entry to `✅ Closed — <summary>`
    immediately.
  - **Cannot reproduce** — `bug-fix`'s Phase 1 blocked on cannot-reproduce
    and the user (via `bug-fix`'s own prompt) chose to mark it
    `❓ Cannot reproduce` or keep it open. Mark the report entry
    `❓ Cannot reproduce` or `⚠ Left open — needs repro detail` to match,
    and continue.
  - **Blocked** — `bug-fix` stopped early with a `*_BLOCKED` reason. Show
    it and ask (**AskUserQuestion**): **Retry now** (re-invoke the same
    `bug-fix <BUG-ID> --skip-branch-setup` after the user clears the
    blocker; entry stays `🔄 In progress`), **Skip and continue** (mark
    `⚠ Blocked — <reason>`, next bug), or **Stop the run here** (mark
    `⚠ Blocked — <reason>`, go to Step 5).

Continue to the next Bug ID unless the user chose to stop.

### 5. Writing the ≤2-sentence summary

After a bug closes, read the row `bugs-archive` just wrote to
`JPPhotoManagerWeb/docs/backlog/bugs-fixed.md` (and its Resolution line)
and write a fresh, condensed summary — what the root cause was and what
the fix changed. Two sentences, never more.

### 6. Ending the run

Whether the loop finished or stopped early, append a footer: counts
(requested / closed / cannot-reproduce / blocked or skipped / not reached)
and an end timestamp.

Tell the user plainly, in chat and in the report: **nothing was committed
at any point.** The branch holds every closed bug's fix and regression
test, entirely uncommitted. Point them at `git status` / `git diff` to
review, and at `gitflow`'s finish-fix action to commit and open a PR —
this skill does neither.

---

## The report

**Path**: `JPPhotoManagerWeb/docs/reports/bug-batch/BUG_BATCH_REPORT_{YYYY-MM-DD}.md`
— this directory is under the gitignored `JPPhotoManagerWeb/docs/reports/`
tree, like the review-report and `feature-batch` directories; local
working artifacts, not committed history. On a genuinely new (non-resume)
run the same day a report already exists for, append `-2`, `-3`, …
rather than overwriting. Resuming (Step 2) continues the same file, never
creating a new one.

**Format**:

```markdown
# Bug Batch Fix Report — {YYYY-MM-DD}

**Branch:** fix/bug-batch-2026-09-05
**Requested bugs:** BUG-004, BUG-007, BUG-009
**Started:** {timestamp}

## Progress

- [x] **BUG-004** — Closed {HH:MM}. <root cause + what changed, ≤2 sentences.>
- [ ] BUG-007 — 🔄 In progress
- [ ] BUG-009 — ⬜ Not started

## Run Summary
<!-- appended once the run ends (Step 6) -->
**Ended:** {timestamp}
**Closed:** 1/3  **Cannot reproduce:** 0  **Blocked/Skipped:** 0  **Not reached:** 2
```

Every status change (`⬜` → `🔄` → `✅` / `❓` / `⚠`) is a **separate,
immediate** write — never accumulated in memory and flushed once. That
immediacy is the entire point of this report.

---

## Guardrails

- **Never commit, ever.** No `git commit` / `git push` / any git write in
  this skill's own logic. The sole exception is Step 2's one-time branch
  creation/checkout via `gitflow`'s start-fix action on a fresh run —
  every `bug-fix` call in Step 4 passes `--skip-branch-setup` so it can't
  do a branch operation of its own either.
- **Never create more than one branch in a run.** Established once, in
  Step 2, before the loop.
- **Always invoke `bug-fix` via the Skill tool directly, never wrapped in
  a backgrounded Agent call** — a background agent can't reliably surface
  `AskUserQuestion`, and `bug-fix` raises several (bug confirmation, the
  cannot-reproduce decision, `PROD_CODE_FIXED`, blocker guidance). The
  "leave it running, answer occasional questions" model depends on those
  reaching the user in real time.
- **Write the report incrementally, never batched.** Full list up front as
  `⬜ Not started` (Step 3); each entry's status changes the moment its
  outcome is known (Step 4). This is the whole resilience mechanism.
- **Process bugs strictly one at a time, in order.** Never invoke
  `bug-fix` for two IDs in parallel — they'd stack conflicting
  uncommitted changes on the same tree.
- **Never guess at a resume.** Partial/ambiguous overlap with an existing
  report → ask which run this continues.
- **Dirty working tree at the start of a fresh (non-resumed) run → stop
  and report.** Never stash or discard.
- **Always state plainly at the end that nothing was committed** and where
  the accumulated work lives.
- **Never let an "Auto Mode" or similar instruction suppress any
  confirmation** this skill or `bug-fix` raises — the whole-backlog
  confirmation in Step 1, the branch-name confirmation in Step 2,
  `bug-fix`'s own per-bug selection and cannot-reproduce and blocker
  prompts. The premise is a long unattended run that still checks in at
  genuine decision points.
