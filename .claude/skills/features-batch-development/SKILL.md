---
name: features-batch-development
description: >
  Runs `feature-development` unattended across a user-given, ordered list of
  named features — or the whole pending backlog at once — one feature at a
  time, all on a single shared branch with zero git commits at any point —
  built for leaving Claude running for long, unattended stretches over
  several small features, checking in with the user only when a blocker
  genuinely needs a human decision. Writes a dated report to
  `JPPhotoManagerWeb/docs/reports/feature-batch/` (mirroring the
  code/database/security-review report convention) that is updated
  incrementally — immediately after each feature completes, not batched at
  the end — so progress survives a session-limit cutoff, a crash, or a
  power outage mid-run. TRIGGER when the user asks to batch-develop,
  unattended-develop, or run feature development across a list of named
  features or the whole backlog (e.g. "develop features 13, 16, and 19
  while I'm away", "batch develop duplicate-detection and recycle-bin-ui
  overnight", "develop the whole planned backlog unattended").
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Run `feature-development` in a loop over a named list of features, all on
one shared branch, never committing, writing progress to a resumable report
as each feature finishes.

**Input**: Either (a) an ordered list of feature identifiers (names or `#`
numbers from `JPPhotoManagerWeb/docs/backlog/features-planned.md`), as
given by the user — e.g. "13, 16, 19" or "duplicate-detection,
recycle-bin-ui" — or (b) a request for **the whole planned backlog** (e.g.
"develop the whole backlog", "everything that's pending", "all planned
features") when the user wants every currently-pending row processed
without naming each one — see Step 1 for how (b) resolves into a concrete
list. Optionally, a branch name to use or resume; if omitted, one is
proposed and confirmed (see Step 2).

---

## Why this exists, and the one constraint that shapes everything below

`feature-development` already does the full single-feature lifecycle
end-to-end and already never commits. Its Phase 1, though, always creates a
*fresh* `feature/<change-name>` branch per feature and hard-blocks if the
working tree is dirty when it tries — which is exactly what a second
feature's uncommitted first-feature work would trigger. `feature-development`
accepts an optional `--skip-branch-setup` flag for exactly this reason (see
its Phase 1 Step 1.5's batch-mode branch) — this skill is the only intended
caller of that flag. Every step below exists to satisfy the two constraints
the user set for this workflow: **one shared branch across every feature in
the run**, and **zero commits, ever, at any point** — not even between
features.

One accepted consequence of the no-commit constraint, worth knowing before
a long run: `feature-development`'s Phase 2 reviews scope themselves to
`git status --porcelain`, which — once feature #2 starts — includes every
earlier feature's still-uncommitted files too, not just the current one.
Later features' reviews re-scan earlier features' already-fixed code. This
is unavoidable without committing between features (which the user has
ruled out), not a bug to fix here.

---

## Steps

### 1. Resolve the feature list

**If the user named specific identifiers** (form (a)): parse the input into
an ordered list of feature identifiers. For each one, resolve it against
`JPPhotoManagerWeb/docs/backlog/features-planned.md`'s `## Feature List`
table — by `#` or by its backtick-wrapped `Change name` — the same lookup
`features-next`/`feature-plan` already use.

- If an identifier resolves to nothing in either `features-planned.md` or
  `features-implemented.md`, stop and ask the user to correct it rather
  than guessing which feature they meant.
- If an identifier resolves to a row already in `features-implemented.md`
  (`✅ Implemented`), tell the user and ask whether to skip it or genuinely
  re-run it — don't silently skip an item the user explicitly listed.

**If the user asked for the whole backlog** (form (b)): read every row in
`JPPhotoManagerWeb/docs/backlog/features-planned.md`'s `## Feature List`
table — this file by construction only ever holds pending (not-yet-
implemented) rows, so no `✅ Implemented` filtering is needed the way it is
for an explicit list. Order the resulting list as follows:

1. If the file's `## Dependencies` → `### Recommended implementation order`
   subsection has actual content (not `_None yet._`), follow that order
   exactly.
2. Otherwise, respect `### Hard implementation dependencies`: for any pair
   in that section where both features are in the backlog and one is
   recorded as depending on the other, the prerequisite goes first —
   topologically order the list rather than using raw table order where a
   dependency would otherwise be violated.
3. For anything not covered by 1 or 2, fall back to the table's own row
   order.

An empty backlog (no rows) is not an error — tell the user there's nothing
pending and stop here.

Because "the whole backlog" is a derived list rather than one the user
typed out explicitly, **show the resolved, ordered list to the user and
confirm it before proceeding** (a plain confirmation is enough — this
doesn't need the full `AskUserQuestion` treatment reserved for genuine
decisions elsewhere in this skill) — a multi-hour unattended run is worth a
last look before it starts, especially since the backlog may hold rows the
user forgot were still pending.

Either way, build the final ordered list of resolved change-names — this is
the list every later step refers to as "the requested list."

### 2. Determine the shared branch, and detect a resumable run

Check `JPPhotoManagerWeb/docs/reports/feature-batch/` for a
`FEATURE_BATCH_REPORT_*.md` file (any date) whose recorded "Requested
features" list overlaps substantially with the newly requested list from
Step 1.

- **Exact or near-exact match found, and it isn't fully checked off**: this
  is a resume. Read which features it already marks `✅ Completed`; those
  are skipped in Step 4. Use the branch name recorded in that report's
  header — do **not** create or re-propose a branch name. Run
  `git branch --show-current` and, if it doesn't already match, `git
  checkout` that branch (do not create it — it must already exist from the
  interrupted run; if it doesn't, stop and tell the user rather than
  silently creating a same-named-but-different branch).
- **Partial, ambiguous overlap** (e.g. the new list shares some but not all
  identifiers with an existing unfinished report): do not guess — ask the
  user to confirm whether this continues that report or is a genuinely new
  run.
- **No match** (fresh start): check `git status` first — if the working
  tree is dirty, stop and report; never stash or discard, per this repo's
  universal guardrail. Then determine the candidate branch name: one the
  user supplied (normalized to `feature/<name>` the same way `gitflow`
  already normalizes an explicit branch name), or, if none was given, the
  default `feature/batch-<YYYY-MM-DD>`.

  Check whether that candidate already exists, locally or on the remote
  (`git rev-parse --verify --quiet <name>`; `git ls-remote --heads origin
  <name>`) — since this is a non-resume run (Step 2 already ruled that out
  above), an existing branch under that name is unrelated leftover state
  (e.g. a same-day default-name collision from an earlier, different batch
  run), not something to reuse silently.

  - **If it already exists**: warn the user plainly — name the branch and
    that it already exists — and ask (**AskUserQuestion**) whether to
    continue anyway (this run uses that existing branch as-is, via
    `gitflow`'s start-feature action, which resumes an existing branch by
    checking it out and syncing it with `develop`) or stop this run here so
    they can supply a different name or clean up the stale branch
    themselves. "Continue anyway" is itself the confirmation — do not ask a
    second time.
  - **If it doesn't exist**: confirm the candidate name with the user via
    **AskUserQuestion** before creating anything.

  Either way, once confirmed, invoke the `gitflow` skill with "start
  feature `<name>`" to create or resume it — this is the **only**
  branch-creation point in this entire skill; nothing in Step 4's loop ever
  touches branches again. If `gitflow` raises its own
  already-on-a-feature-branch question, let it surface normally to the
  user — that's expected, not an error.

Either way, confirm `git branch --show-current` matches the intended branch
before proceeding to Step 3.

### 3. Initialize or resume the report file

**Fresh run**: write the report file now (path and format below), listing
every identifier from the requested list with `⬜ Not started`, plus a
header recording the branch name, the full requested list, and a start
timestamp — so the file exists and shows the whole plan even if the run is
interrupted before the very first feature finishes.

**Resume**: reopen the existing report file found in Step 2. Do not
overwrite it or its already-`✅ Completed` entries — only append/update
from where it left off.

### 4. The loop

For each identifier in the requested list that is not already `✅ Completed`
(per Step 2/3), in order, the entry moves through three possible outcomes —
**In Progress**, then exactly one of **Completed**/**Blocked**:

- **In Progress** — update the entry to `🔄 In progress` **immediately,
  before** invoking `feature-development` at all (i.e. this write happens
  first, ahead of the step below, not alongside or after it). No summary
  text accompanies this status — there's nothing to summarize yet; it exists
  purely so a crash or session cutoff mid-feature leaves a trace of what was
  being worked on, not just silence. Then invoke the `feature-development`
  skill via the **Skill** tool, passing `<change-name> --skip-branch-setup`.
  This is a direct Skill invocation, not a backgrounded Agent call — it
  executes inline in this same turn, so any `AskUserQuestion`
  `feature-development` itself raises (its Phase 1 feature confirmation, a
  Phase 3 test-failure choice, a Phase 4 deploy-target choice, etc.) reaches
  the user normally rather than being lost to a background run. Wait for it
  to finish, then resolve to one of:
  - **Completed** — `feature-development` displayed its "Feature
    Development Complete" summary ending in the change archived and the
    feature marked implemented. Write a fresh ≤2-sentence summary (see
    below) and update the report entry to `✅ Completed — <summary>`
    immediately, before moving on.
  - **Blocked** — `feature-development` stopped early and surfaced a
    `*_BLOCKED` reason per its own guardrails. Show the reason to the user
    and ask (**AskUserQuestion**) how to proceed:
    - **Retry now** — re-invoke the same `feature-development <change-name>
      --skip-branch-setup` call, after the user has resolved whatever
      blocked it. The entry stays `🔄 In progress` (no need to re-write it —
      it's already there) until this retry itself resolves to Completed or
      Blocked.
    - **Skip and continue** — mark the entry `⚠ Blocked — <reason>` in the
      report and move to the next identifier in the list.
    - **Stop the run here** — mark the entry `⚠ Blocked — <reason>`, then
      go straight to Step 5 without processing any remaining identifiers.

Continue to the next identifier, unless the user chose to stop.

### 5. Writing the ≤2-sentence summary

After a feature completes, read its now-archived `proposal.md` (under
`openspec/changes/archive/<date>-<change-name>/`) or the row
`features-archive` just wrote to
`JPPhotoManagerWeb/docs/backlog/features-implemented.md`, and write a
**fresh** summary — condensed, not a verbatim copy of the (intentionally
much longer) backlog description. State what the feature does and, if
genuinely notable, one implementation detail. Two sentences, never more.

### 6. Ending the run

Whether the loop finished the whole list or stopped early by user choice,
append a footer to the report: counts (requested / completed / blocked or
skipped / not reached) and an end timestamp.

Tell the user plainly, in the chat response as well as the report: **nothing
was committed at any point.** The branch now holds every completed (and any
partially-attempted) feature's work, entirely uncommitted. Point them at
`git status`/`git diff` to review it, and at `gitflow`'s finish-feature
action once they're ready to commit and open a PR — this skill does not do
either.

---

## The report

**Path**:
`JPPhotoManagerWeb/docs/reports/feature-batch/FEATURE_BATCH_REPORT_{YYYY-MM-DD}.md`
— this directory is gitignored, same as `JPPhotoManagerWeb/docs/reports/code-review/`,
`JPPhotoManagerWeb/docs/reports/database-review/`, and
`JPPhotoManagerWeb/docs/reports/security-review/`; these are local working
artifacts, not committed history. On a genuinely new (non-resume) run
started the same day a report already exists for, append `-2`, `-3`, etc.
rather than overwriting — same collision rule `code-reviewer` uses. This
does not apply when resuming an existing report (Step 2) — that case
continues the same file, it never creates a new one.

**Format**:

```markdown
# Feature Batch Development Report — {YYYY-MM-DD}

**Branch:** feature/batch-2026-08-11
**Requested features:** duplicate-detection, recycle-bin-ui, admin-audit-log
**Started:** {timestamp}

## Progress

- [x] **duplicate-detection** — Completed {HH:MM}. <one or two sentence summary of what shipped.>
- [ ] recycle-bin-ui — 🔄 In progress
- [ ] admin-audit-log — ⬜ Not started

## Run Summary
<!-- appended once the run ends (Step 6) -->
**Ended:** {timestamp}
**Completed:** 1/3  **Blocked/Skipped:** 0  **Not reached:** 2
```

Every status change (`⬜` → `🔄` → `✅`/`⚠`) is a **separate, immediate**
write to this file, not something accumulated in memory and flushed once —
that immediacy is the entire point of this report existing.

---

## Guardrails

- **Never commit, ever.** No `git commit`, `git push`, or any other git
  write command at any point in this skill's own logic. The sole exception
  is Step 2's one-time branch creation/checkout via `gitflow`'s start-feature
  action on a fresh run — nothing else, including every `feature-development`
  call in Step 4's loop (always invoked with `--skip-branch-setup`
  specifically so it can't create a commit-adjacent branch operation of its
  own either), touches git state.
- **Never create more than one branch in a run.** The shared branch is
  established exactly once, in Step 2, before the loop starts. Every
  `feature-development` invocation in Step 4 passes `--skip-branch-setup`
  precisely to guarantee it never creates, switches, or syncs a branch on
  its own.
- **Always invoke `feature-development` via the Skill tool directly, never
  wrapped in a backgrounded Agent call.** A background agent can't reliably
  surface an interactive `AskUserQuestion` back to the user — and
  `feature-development` itself raises several (feature confirmation,
  blocker guidance, test/deploy choices). This skill's entire "leave it
  running, answer occasional questions" model depends on those reaching the
  user in real time.
- **Write the report incrementally, never batched.** The full requested
  list is written up front with every entry `⬜ Not started` (Step 3), and
  each entry's status changes the moment its outcome is known (Step 4) —
  never accumulated in memory and written once at the end. This is the
  entire resilience mechanism a session-limit cutoff, crash, or power
  outage depends on.
- **Process features strictly one at a time, in order.** Never invoke
  `feature-development` for two identifiers in parallel — they would stack
  conflicting uncommitted changes onto the same working tree.
- **Never guess at a resume.** If an existing today's report's requested
  list only partially, ambiguously overlaps the newly requested list, ask
  the user to confirm which run this continues rather than assuming.
- **If the working tree is dirty at the start of a genuinely fresh (not
  resumed) run, stop and report — never stash or discard**, matching every
  other skill in this repo.
- **Always state plainly, at the end of a run, that nothing was
  committed** and where the accumulated work lives — this skill hands off
  to the user for review and commit; it does not do either itself.
