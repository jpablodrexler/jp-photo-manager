---
name: bug-fix
description: >-
  Runs the full lifecycle of fixing one bug from the backlog: picks the bug,
  creates a fix/ branch, reproduces it with a failing regression test FIRST,
  implements the fix, runs the code review and fixes its findings, then runs
  the phases that apply to the project — database and security reviews and a
  viewport check when the fix touches schema, sensitive code or templates,
  tests, an end-to-end check when a user-facing flow changed, a local build —
  skipping any that do not apply, syncs docs if behaviour changed, then closes
  the bug via bugs-archive. No git commits unless asked with --commit (then one
  commit, made by gitflow) — otherwise hands off for review and PR.
  TRIGGER when asked to fix a bug, work through the bug backlog one at a time,
  or address a specific BUG-NNN — including a bug only just described (file it
  with bug-report first, then run this skill). "Plan and fix a bug" also
  triggers it. Bug-family counterpart to feature-development.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "0.1"
---

Orchestrate one bug fix from selection to close, using dedicated subagents
per phase. This is a leaner cousin of `feature-development`: no SDD
artifacts, no `openspec-*`, no `spec-compliance-check` — but the same
branch discipline, the same review/test/build gates, and the same
never-commit rule. The one thing it adds that `feature-development`
doesn't have: **a failing regression test is written before the fix**, and
it's the durable artifact the fix leaves behind.

**Input**: Optional Bug ID (e.g. `BUG-004`), optionally followed by
`--skip-branch-setup` (e.g. `BUG-004 --skip-branch-setup`) and/or `--commit`.
The Bug ID, if
given, is passed to `bugs-next` to skip recommendation and jump straight
to confirmation. `--skip-branch-setup` is for orchestrators — the
`bugs-batch-fix` skill — that have already checked out a shared branch and
run multiple fixes on it without committing between them; omit it for
normal single-bug use. `--commit` is an explicit opt-in that the user (or an
orchestrator acting on their request) passes to get one commit for this bug
after it closes (Phase 7); without it nothing is committed.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`; `<reports>` is the `reports` folder next to the backlog's parent (`docs/reports` by default).

**The user talks, the skill reads flags.** `--skip-branch-setup` and `--commit` are shorthand for orchestrators; a user never needs to type them or to phrase a request in any special way. "Fix BUG-004", "fix BUG-004, stay on this branch", "fix BUG-004 and commit as you go" are complete requests: map what the user said to the behaviour below, state in one line how it was understood (branch handling, commit cadence), and run every phase regardless of how short the request was. Every phase runs by default; only a skip condition written in this skill may skip one.

**Working on a branch the user is already on.** When the user is on a topic branch (not `main`/`develop`) and says to stay on it, treat the run as `--skip-branch-setup` even if the flag was not typed: Phase 1 Step 1 only confirms the branch and never creates or syncs one. Say so in the first line of the run.

**Committing as you go.** `--commit` means one commit, at Phase 7. When the user asks instead to commit as the work goes, that request is the opt-in for a commit after each phase that changed files (Phase 1, Phase 2, the fixes of each review gate, Phases 3 to 5, Phase 6), each one made by `gitflow`'s commit action with the message `Fix <bug-id>: <phase> — <one-line summary>`; Phase 7 then commits only what is left, or records `COMMIT: nothing left`. The user's request stands in for the per-commit confirmation of the file list and message, but `gitflow` still makes every commit: no phase or subagent runs `git commit` itself. The review gate's scope follows the commit mode (see `reference/review-gate.md`, G0).

**Phase ledger.** Right after Phase 0, create a task list with one entry per phase and gate that applies (1, 2, 2R: G1 to G5, 3, 4, 5, 6, 7) and keep it current: an entry is completed when its signal was verified, or when it was skipped under its own stated condition with the reason written on the entry. The Final Summary is built from this list and is not shown while an entry is open.

**Skills run as written, by whoever performs the phase.** A phase that names a skill (`code-reviewer`, `database-reviewer`, `security-reviewer`, `docs-sync`, `bugs-archive`) invokes that skill; doing the same checks by hand, or writing its report by hand, is a failed phase even when the content looks right. Reports carry the stamp the reviewer skills write (`<!-- backlog-kit: <skill> -->`), and the orchestrator checks it (review gate G6).

**Never run `git commit`, `git push` or open a PR.**

The one exception is a commit the user asked for: `--commit` (Phase 7 asks
`gitflow`'s commit action for a single local commit) or a request to commit
as the work goes (one `gitflow` commit after each phase that changed files).
Never push, and never commit any other way.

**Every phase runs, or is skipped under its own stated skip condition and
the reason recorded.** Running a phase inline in the orchestrator's context
instead of in a subagent is fine; merging phases, dropping one, or calling
one "optional" when its skip condition is not met is not. The Final Summary
lists a status for every phase (see its table) and a bug with a missing row
is not closed.

---

## Overview

Phase 0 runs in the orchestrator's own
context (never a subagent — the confirmation must reliably reach the
user); Phases 1–6 each run as a dedicated `run_in_background: false`
subagent, each gated on the prior phase's signal. **Phases 3, 4 and 5 are
optional**, and so are the database review, the security review and the
viewport check inside Phase 2R: the orchestrator (never the fix subagent) checks whether each applies to the
project and to this fix, and when it does not it is skipped with a one-line
reason that appears in the Final Summary.

| Phase | Skills / actions |
| ----- | ---------------- |
| 0 — Select & Confirm *(orchestrator's own context)* | `bugs-next` (recommend + mandatory `AskUserQuestion`) |
| 1 — Branch & Reproduce | `git fetch --prune` → `gitflow` (start/resume/sync `fix/<slug>`) → mark bug `🔶 In Progress` → write a **failing** regression test that reproduces the bug |
| 2 — Fix | implement the fix until the regression test passes; reviews are not its job |
| 2R — Review gate *(orchestrator + one subagent per review)* | the orchestrator computes `CHANGED_FILES`, decides which conditional reviews apply and verifies every report; separate subagents run `code-reviewer`, then — only when the fix touches them — `database-reviewer`, `security-reviewer` and the mobile/desktop viewport check; findings fixed (`reference/review-gate.md`) |
| 3 — Tests *(optional)* | the project's tests (and its fast smoke tier, if CI runs one) until they pass; skipped if the project has no automated tests |
| 4 — E2E verification *(optional, conditional)* | the project's end-to-end/browser checks when the fix touches a user-facing flow — test account decided by the orchestrator first; skipped otherwise |
| 5 — Build verification *(optional)* | the project's build, locally; skipped if there is no build step |
| 6 — Close | docs sync (scoped, best-effort, only if behaviour changed) → `bugs-archive <BUG-ID> fixed "<resolution>"` |
| 7 — Commit *(only with `--commit`)* | `gitflow` commit action: one local commit holding the fix, the regression test and the backlog move |

---

## Phase 0 — Select & Confirm (orchestrator's own context — never a subagent)

**Never delegate this to a spawned subagent**, including under an "Auto
Mode" or similar autonomous-operation instruction. Which bug gets fixed is
the user's decision; a spawned/backgrounded subagent can't reliably
surface `AskUserQuestion`.

1. Use the Skill tool to invoke `bugs-next` directly (not wrapped in an
   Agent call), passing the Bug ID given to this skill, if any. Let it
   present its recommendation and ask for confirmation in this same
   context.
2. If `bugs-next` returns without a `BUG_ID:` line (user cancelled, or it
   couldn't obtain confirmation), stop the whole workflow here and tell
   the user. Never proceed to Phase 1 without a confirmed Bug ID.
3. Otherwise capture the confirmed Bug ID from the `BUG_ID:` line. This is
   `<bug-id>` in every phase below. Read that bug's row from
   `<backlog>/bugs-open.md` and its details (steps to reproduce /
   expected / actual / environment / notes) from
   `<backlog>/bugs/<bug-id>.md` now, in the orchestrator context, so
   the repro steps / expected / actual can be pasted verbatim into
   Subagent 1's prompt (a subagent shouldn't have to re-hunt for them).

**Derive `<slug>`** for the branch: `<bug-id lowercased>-<2–4 kebab words
from the bug title>`, e.g. `bug-004-expense-chart-dark-mode`. The branch
will be `fix/<slug>`.

---

## Placeholder substitution (Phases 1–6)

Before spawning any subagent, replace every `<bug-id>`, `<slug>`, and
`<repro-details>` (the pasted Steps / Expected / Actual / Notes from Phase
0) in its prompt with the actual values. From Phase 2 onward, also
substitute `<repro-test-path>` with the path captured from Phase 1's
`REPRO: DONE — test: <path>` line.

Also append, unchanged, to every one of those subagent prompts:

> **Hand-back.** Your hand-back is the final text of your response, ending with exactly the signal line(s) this prompt asks for. Do not send it as a message through a messaging or hand-back tool, and never send placeholder text: the orchestrator reads only your final response, and a response without its signal is treated as blocked.

---

## Phase 1 — Branch & Reproduce (Subagent 1)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug to fix has already been selected and confirmed with the user:
> `<bug-id>`. Do not invoke `bugs-next` or re-select a bug. Its recorded
> repro:
>
> <repro-details>
>
> **Step 1 — Set up the fix branch**
>
> **Batch mode (`--skip-branch-setup` was passed):** run `git branch
> --show-current` once; if it is `main` or `develop`, end with
> `FIX_BLOCKED — batch mode requires an existing fix branch already
> checked out` and stop. Otherwise continue on the current
> branch. Do not check for uncommitted changes, and do not create, switch,
> or sync any branch — the caller owns branch state. Then go to Step 1.5.
>
> **Normal mode (no flag):** check resume status *before* checking for
> uncommitted changes (a resumed run is typically already on
> `fix/<slug>` with the in-progress fix sitting there uncommitted — that's
> expected, not a blocker).
>
> B0. `git fetch origin --prune`.
> B1. `git branch --show-current`.
>    - If it is already exactly `fix/<slug>`: this is a resume. Run
>      `git rev-list HEAD..origin/fix/<slug> --count` (treat a missing
>      `origin/fix/<slug>` as `0`). If `0`, skip to Step 1.5. If `>0`, end
>      with `FIX_BLOCKED — origin/fix/<slug> has <N> commit(s) not present
>      locally; reconcile manually` and stop.
>    - Otherwise continue to B2.
> B2. `git status --porcelain`. Ignore every entry whose path is under
>    `<backlog>/`: `bug-report` leaves the new row and details file
>    uncommitted on purpose, and the new branch carries them along (create
>    it with `git checkout -b` when `gitflow` stops on them, and say so in
>    your hand-back). If it prints anything else, end with
>    `FIX_BLOCKED — uncommitted changes present, cannot create fix branch`
>    and stop.
> B3. `git rev-parse --verify --quiet fix/<slug>`:
>    - Prints a hash → the branch exists locally but isn't checked out:
>      `git checkout fix/<slug>`, then if `origin/fix/<slug>` exists
>      `git merge origin/fix/<slug>` (already-pushed history of this same
>      fix — exempt from the no-commits rule, like a sync merge; if it
>      doesn't merge cleanly, end with `FIX_BLOCKED — fix branch diverged
>      from origin, needs manual resolution` and stop). Then invoke
>      `gitflow` with "sync fix `<slug>`" to bring it up to date with
>      `develop`. On an unresolvable conflict, end with `FIX_BLOCKED — fix
>      branch sync conflict with develop` and stop.
>    - Fails locally but `git rev-parse --verify --quiet origin/fix/<slug>`
>      succeeds → another device pushed it first: `git checkout -b
>      fix/<slug> origin/fix/<slug>`, then `gitflow` "sync fix `<slug>`",
>      same conflict handling.
>    - Fails both → invoke `gitflow` with "start fix `<slug>`". If it
>      reports a blocker, end with `FIX_BLOCKED — <the gitflow blocker>`
>      and stop.
> B4. `git branch --show-current` must now print exactly `fix/<slug>`. If
>    not, end with `FIX_BLOCKED — could not switch to fix branch` and stop.
>
> **Step 1.5 — Mark the bug In Progress**
> Open `<backlog>/bugs-open.md`, find the `<bug-id>` row, and if its
> **Status** is `⬜ Open` change it to `🔶 In Progress` and save. Leave it
> unchanged if it's already `🔶 In Progress`. If there's no matching row
> (bug tracked ad hoc), skip silently — not an error.
>
> **Step 2 — Reproduce the bug with a FAILING regression test**
> Write the smallest test that fails *because of this bug* and will pass
> once it's fixed. Pick the layer the bug actually lives at, following the
> project's test conventions (see its CLAUDE.md and existing tests), and
> prefer extending an existing test file over creating a new one.
>
> Run just that test and confirm it **fails**, and that the failure is the
> bug described in `<repro-details>` — not an unrelated error, a typo in
> the test, or a missing stub. **Quote the failing assertion or error
> output verbatim in your hand-back** (a compile error is acceptable only
> when the fix adds the API the test uses, and say so). A test you never
> saw fail is not a regression test; the hand-back without that quote is
> treated as blocked.
>
> **If you cannot make it fail** — the behaviour looks correct, or the
> recorded steps are too vague to reproduce — do not invent a fix. End
> with `FIX_BLOCKED — cannot reproduce <bug-id>: <what you tried and what
> you observed>` and stop. Do not change the bug's Status beyond the
> `🔶 In Progress` set in Step 1.5.
>
> **Step 3 — Hand back**
> End with exactly:
> `RED: <the verbatim failing assertion or error line>`
> `REPRO: DONE — test: <path to the test file>`
> `BUG_ID: <bug-id>`

After the subagent completes:
- Contains `FIX_BLOCKED — cannot reproduce`: surface it to the user and
  ask (**AskUserQuestion**, in the orchestrator's own context): "Mark
  `<bug-id>` as ❓ Cannot reproduce?" — options: "Yes — mark it Cannot
  reproduce" (invoke `bugs-archive <bug-id> cannot-repro "<subagent's
  note>"` and stop the workflow), "Keep it open — I'll add repro detail"
  (stop the workflow, leave the row `🔶 In Progress`), "Cancel". Do not
  proceed to Phase 2.
- Contains any other `FIX_BLOCKED`: surface the reason and stop.
- No `RED:` line quoting real failing output → treat as blocked; do not proceed to Phase 2.
- Otherwise: capture the `REPRO: DONE — test: <path>` and `RED:` lines and proceed.

---

## Phase 2 — Fix (Subagent 2) and Phase 2R — Review gate

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> The bug `<bug-id>` has a failing regression test at `<repro-test-path>`.
> Recorded repro:
>
> <repro-details>
>
> **Step 1 — Implement the fix**
> Fix the root cause in the source. Prefer the
> narrowest change that addresses the cause, not the symptom. Then run the
> regression test at `<repro-test-path>` and confirm it now **passes**. If
> you hit a blocker needing user input, end with `FIX_BLOCKED — <reason>`
> and stop.
>
> Never materialize a generated secret's raw value anywhere in a response,
> report, or file (same rule as `feature-development` Phase 2) — leave any
> task that would require it unchecked with a note naming the manual
> command.
>
> **Step 2 — Signal completion**
> List what you changed in plain words (no verdict on whether any review applies; reviews are run by the orchestrator's review gate, each in its own subagent — do not run `code-reviewer`, `database-reviewer`, `security-reviewer` or the viewport check yourself). End with exactly:
> `FIX_IMPLEMENTED: DONE`

Do not start Phase 2R until this subagent returns `FIX_IMPLEMENTED: DONE`.

### Phase 2R — Review gate (orchestrator)

Read `reference/review-gate.md` in full and run it, with `<work-id>` = `<bug-id>`: the orchestrator computes `CHANGED_FILES` itself, spawns one subagent each for the code review, the database review, the security review and the viewport check (deciding itself, from `git status --porcelain`, which of the last three apply), verifies every report file and its checkboxes before accepting a signal, and ends with `REVIEW_GATE: DONE`. Its ledger lines feed the Final Summary.

Do not start Phase 3 until the gate returns `REVIEW_GATE: DONE`.
On any `*_BLOCKED`, surface the details and wait for user guidance.

---

## Phase 3 — Tests (Subagent 3, optional)

Skip this phase when the project has no automated tests: find out yourself
(orchestrator) from its `CLAUDE.md`, `README` and build manifest which command
runs them — and a faster end-to-end smoke tier, if CI runs one. If you cannot
tell, ask the user once (**AskUserQuestion**, with "skip this phase" as an
option); if there are no tests, record `TESTS: SKIPPED — no automated tests`
and go to Phase 4.

Runs unconditionally otherwise, regardless of what the fix touched — a tier CI
gates every PR on can still be broken by a fix that looks unrelated.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the commands you found; delete Step 2 if there is no smoke tier):

> Validate the fix for `<bug-id>`. Both suites are local-only.
>
> **Step 1 — Tests**
> `<TEST_COMMAND>`. On failure: diagnose (type error, missing stub,
> assertion mismatch, …), fix — preferring test code over production source;
> if you must touch production source note it `PROD_CODE_FIXED: <file> —
> <reason>` — re-run until green.
>
> **Step 2 — Smoke tier**
> `<SMOKE_COMMAND>` (allow ~5 min). Most failures here are a stale assertion in
> the spec itself (a hardcoded string / fixture that no longer matches what the
> component renders), not a real regression — check the rendered output before
> changing any production file. Fix, preferring the spec over production source
> (same `PROD_CODE_FIXED:` rule), re-run until green.
>
> End with one of:
> - `TESTS: PASS (<n> tests, <m> smoke tests)`
> - `TESTS: BLOCKED — <reason, noting which tier>`

On `BLOCKED`, stop and surface the failure. On one or more
`PROD_CODE_FIXED:` lines, surface them to the user (those production
changes weren't covered by the Phase 2R review gate) and ask: **Proceed** (record
`UNREVIEWED_PROD_FIXES:` for the Final Summary) or **Re-run the review gate** (Phase 2R over the fixes, then new Subagent 3).

---

## Phase 4 — E2E Verification (Subagent 4, optional, conditional)

Skip this phase — recording `E2E: SKIPPED — <reason>` — when the project has no
end-to-end or browser suite (its `CLAUDE.md`, `README` and build manifest say),
or when the fix touches no user-facing flow.

**Before spawning — resolve the test account** (orchestrator step; needs
`AskUserQuestion`):

1. Run `git status --porcelain` yourself. If no changed path falls under a
   user-facing flow the suite covers (screens, routes, endpoints, or a schema
   change that changes what they render) — skip straight to spawning Subagent
   4; its own Step 1 will land on `E2E: SKIPPED`.
2. If a flow was touched and the suite needs a test account or live services,
   ask the user (**AskUserQuestion**) whether to run it and how the account is
   provided (a credentials file they maintain, environment variables, or a
   fresh registration if the app allows it). Never read a credentials file
   yourself and never put credentials in a prompt; set
   `CREDENTIAL_SOURCE: <the answer>`. If they decline, record
   `E2E: SKIPPED — user chose not to run it` and go to Phase 5.
3. Substitute `<CREDENTIAL_SOURCE>` into the prompt.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the suite's command for `<E2E_COMMAND>`):

> **Step 1 — Does E2E verification apply?**
> `git status --porcelain` → parse all paths (incl. `??`) into
> `CHANGED_FILES`. If none fall under a user-facing flow the suite covers,
> end with `E2E: SKIPPED — no user-facing flow changed` and stop.
>
> **Step 2 — Environment** — confirm what the suite needs is reachable (the
> backend or database it talks to, required configuration, migrations applied
> if one was touched). On failure → `E2E_BLOCKED — <reason>` and stop.
>
> **Step 3 — Dev server** — if the suite needs the application running, kill
> any stale instance, start a fresh one, confirm it answers. On failure →
> `E2E_BLOCKED — dev server did not start`.
>
> **Step 4 — Browser checks** — credential source: `<CREDENTIAL_SOURCE>`
> (already decided; do not re-ask, do not decide yourself; never log or echo
> credentials; if the source is unusable now, `E2E_BLOCKED — <source> unusable
> at read time`, never silently fall back).
> 1. Run `<E2E_COMMAND>` — the narrowest command that covers the changed flow.
> 2. If the fix touched a specific flow, exercise the specific flow the bug was
>    about, once, through the real UI against the real backend — confirm the
>    bug is actually gone here, not just in the unit test — with no console
>    errors.
> 3. A cross-user data-scoping check — only if the fix touched data access
>    **and** a real second account is available; otherwise skip it and say so
>    via `PASS_PARTIAL`.
> 4. Screenshot the final state and read it back for layout breaks.
>
> **Step 5 — Tear down** — stop any server you started.
>
> End with one of:
> - `E2E: PASS — <one-line summary>`
> - `E2E: PASS_PARTIAL — <what passed> — NOT COVERED: <what was skipped and why>`
> - `E2E_BLOCKED — <reason>`

Do not start Phase 5 until `E2E: PASS`, `PASS_PARTIAL`, or `SKIPPED`. On
`E2E_BLOCKED`, surface and wait — a failure against a live environment may be
environment/data, not a code defect; don't assume it belongs back in Phase 2
without the user confirming.

---

## Phase 5 — Build Verification (Subagent 5, optional)

Skip this phase — recording `BUILD: SKIPPED — no build step` — when the project
has no build, compile or type-check step (find out yourself from its
`CLAUDE.md`, `README` and build manifest; if unsure, ask once with "skip" as an
option). This skill never deploys anything; the build runs locally only so a
failure is caught before CI or the host would.

Spawn a **general-purpose subagent** (`run_in_background: false`) with
(substitute the command for `<BUILD_COMMAND>`):

> **Step 1** — `<BUILD_COMMAND>` (allow up to 5 min; if it can take longer, poll
> with repeated foreground calls in this same turn — never end your response
> while it still runs in the background).
> **Step 2** — On failure: read the error (it names the file and cause), fix
> prioritising files related to the fix, re-run until it succeeds or you hit a
> blocker needing human input.
> **Step 3** — If the build produces an output directory or artifact, confirm
> it exists and is non-empty — empty or missing after a "successful" build is
> itself a failure.
>
> If you must modify a production source file (not a test or build
> configuration file), note it `PROD_CODE_FIXED: <file> — <reason>`.
>
> End with `BUILD: PASS` (after any `PROD_CODE_FIXED:` lines) or `BUILD: BLOCKED — <reason>`.

Do not start Phase 6 until `BUILD: PASS` (or the phase was skipped). On one or
more `PROD_CODE_FIXED:` lines, handle them as Phase 3's: ask the user to
**proceed** (record `UNREVIEWED_PROD_FIXES:` for the Final Summary) or to
**re-run the review gate** (Phase 2R over the fixes, then Phases 3 and 5 again).

---

## Phase 6 — Close (Subagent 6)

Spawn a **general-purpose subagent** (`run_in_background: false`) with:

> **Step 1** — Invoke `docs-sync` in its scoped-sync mode for the
> current branch's changes (it diffs against the branch base itself).
> **Best-effort, not blocking**: a bug fix usually changes no documented
> behaviour, so "nothing to sync" is the common and fine outcome. Only
> stop and surface details if the skill errors out leaving files broken.
>
> **Step 2** — Invoke `bugs-archive <bug-id> fixed "<one-line resolution:
> what the root cause was and what changed; include the regression test
> path>"`. Wait for it to move the row to `<backlog>/bugs-fixed.md` and append the
> Resolution line to `<backlog>/bugs/<bug-id>.md`.
>
> End with exactly:
> `CLOSE: DONE`

Do not display the Final Summary until this subagent returns `CLOSE:
DONE`.

---

## Phase 7 — Commit (only when `--commit` was passed)

Skip with `COMMIT: SKIPPED — not requested` when the flag is absent and the user did not ask to commit as the work goes.
Otherwise invoke `gitflow` with "commit `Fix <bug-id>: <one-line resolution>`"
(a direct Skill call in the orchestrator's context). It shows what will be
committed, confirms with the user, makes one local commit on the current fix
branch and does not push. On a refusal or a failure, surface it and stop;
the bug stays closed in the backlog, uncommitted.

---

## Final Summary

```
## Bug Fix Complete

**Bug:** <bug-id> — <title>
**Regression test:** <path> (failed before the fix, passes now)
**Root cause / fix:** <one or two sentences>
**Code review:** ✓ All findings resolved
[if the viewport check ran:]
**Viewport check:** ✓ <each configured phone viewport and the desktop viewport: verified, or ⚠ skipped — reason> — <what was checked/caught>
**Tests:** ✓ <summary> (or N/A — <reason> if skipped)
**E2E:** ✓ <summary> (or N/A — <reason> if skipped, or ⚠ Partial: <what wasn't covered>)
**Build:** ✓ <summary> (or N/A — <reason> if skipped)
[if UNREVIEWED_PROD_FIXES was recorded:]
**⚠ Unreviewed production fixes:** <the PROD_CODE_FIXED lines> — fixed while chasing test or build failures; the user chose to proceed without routing them back through the Phase 2R review gate.
**Docs sync:** ✓ <one-line summary, or "Nothing to sync">
**Bug:** ✓ Closed — moved to bugs-fixed.md
**Commit:** ✓ <short hash> (or N/A — not requested)

| Phase | Status |
| ----- | ------ |
| 1 Reproduce (red test, quoted failure) | ✓ / ✗ |
| 2 Fix | ✓ / ✗ |
| 2R Code review (report path) | ✓ / ✗ |
| 2R DB review / security review / viewport check (report paths) | ✓ / N/A — <the orchestrator's reason> |
| 3 Tests | ✓ / N/A — <reason> |
| 4 E2E | ✓ / N/A — <reason> |
| 5 Build | ✓ / N/A — <reason> |
| 6 Docs sync + archive | ✓ / ✗ |
| 7 Commit | ✓ / N/A — not requested |

If no commit was requested: nothing was committed. The fix (and its regression test) is on `fix/<slug>`, entirely uncommitted. Review with `git status` / `git diff`, then use `gitflow`'s finish-fix action to commit and open a PR.
```

---

## Guardrails

- **Never collapse the phases to save effort.** Written-in-one-go fix and
  test, "just the touched specs", no review, no build and a lean summary
  are exactly what this skill exists to prevent. A skip needs its stated
  condition and a recorded reason.
- Always capture the Bug ID confirmed in Phase 0 and propagate it to every
  later phase. Substitute `<bug-id>` / `<slug>` / `<repro-details>` before
  spawning each subagent.
- **Cancellation**: if `bugs-next` returns without a `BUG_ID:` line, stop
  immediately, before Subagent 1 is spawned.
- **The regression test is written before the fix, in Phase 1, and must
  fail for the right reason before Phase 2 starts.** A fix with no failing
  test to justify it is not this skill's workflow — if Phase 1 can't
  reproduce the bug, that's a `FIX_BLOCKED — cannot reproduce`, resolved
  through the AskUserQuestion decision point, never worked around by
  skipping straight to a speculative fix.
- Do not start a phase until the prior phase's subagent returns its
  expected signal (`TESTS`/`E2E`/`BUILD`: `PASS` or `SKIPPED` for the optional phases). On any `*_BLOCKED`, surface details and wait for user
  guidance — never retry automatically.
- **Missing signal fallback**: a subagent that returns without its
  expected signal is treated as `BLOCKED`; surface its raw response and
  wait.
  Before re-running a phase whose signal is missing, look at the working tree
  for what the subagent actually did (for example `git status`, the backlog
  row, the test file) and tell the user; whether to accept that evidence in
  place of the signal or to re-run the phase is the user's decision.
- **The reviews are never run inside the fix subagent.** Phase 2R runs each in its own subagent; the orchestrator decides which conditional reviews apply (from its own `git status --porcelain`) and verifies each report file and its checkboxes before accepting a signal (`reference/review-gate.md`, G6).
- **`code-reviewer` (and the DB/security reviewers when they run) always uses its
  Review workflow, scoped to `CHANGED_FILES`, never the interactive Fix
  Workflow and never a full-codebase sweep.**
- **No git commits unless `--commit` was passed or the user asked to commit as the work goes, and then only through
  `gitflow`'s commit action (in Phase 7, or after each phase when the user asked to commit as the work goes).** Neither this skill nor any subagent
  may run `git commit` / `git push` / any git write itself. The sole other
  exceptions are
  Phase 1 Step 1's branch creation and sync (`git fetch`, `git checkout`,
  `git checkout -b`, `git merge origin/develop` via `gitflow`'s
  start-fix/sync-fix, and `git merge origin/fix/<slug>` to pick up
  another device's already-pushed commits on this same fix).
- **The fix branch is synced from `develop` only, never `main`** — via
  `gitflow`'s sync-fix action, which merges `origin/develop` and nothing
  else. This holds even right after a release merges into both.
- **`--skip-branch-setup` (batch mode) only affects Phase 1 Step 1.** No
  other phase changes — Phases 2–6 already operate on "whatever branch is
  checked out". This flag is only ever passed by `bugs-batch-fix`.
- **Auto Mode never overrides a required confirmation** — Phase 0's bug
  selection, the cannot-reproduce decision point, the
  `PROD_CODE_FIXED` choice, the E2E credential-source question, and any post-`*_BLOCKED` guidance prompt all still stop and wait for a real
  answer.
- **Never let a subagent print a generated secret's raw value** anywhere
  in a response, report, or file (same standard as `feature-development`).
