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

`<integration>` is `develop` and `<production>` is `main` unless `.backlog-kit.toml` sets `[git] integration_branch` / `production_branch`.

**The user talks, the skill reads flags.** `--skip-branch-setup` and `--commit` are shorthand for orchestrators; a user never needs to type them or to phrase a request in any special way. "Fix BUG-004", "fix BUG-004, stay on this branch", "fix BUG-004 and commit as you go" are complete requests: map what the user said to the behaviour below, state in one line how it was understood (branch handling, commit cadence), and run every phase regardless of how short the request was. Every phase runs by default; only a skip condition written in this skill may skip one.

**Working on a branch the user is already on.** When the user is on a topic branch (not `<production>`/`<integration>`) and says to stay on it, treat the run as `--skip-branch-setup` even if the flag was not typed: Phase 1 Step 1 only confirms the branch and never creates or syncs one. Say so in the first line of the run.

**Committing as you go.** `--commit` means one commit, at Phase 7. When the user asks instead to commit as the work goes, that request is the opt-in for a commit after each phase that changed files (Phase 1, Phase 2, the fixes of each review gate, Phases 3 to 5, Phase 6), each one made by `gitflow`'s commit action with the message `Fix <bug-id>: <phase> — <one-line summary>`;
Phase 7 then commits only what is left, or records `COMMIT: nothing left`. The user's request stands in for the per-commit confirmation of the file list and message, but `gitflow` still makes every commit: no phase or subagent runs `git commit` itself.
The review gate's scope follows the commit mode (see `reference/review-gate.md`, G0).

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

Before spawning any subagent, replace every `<bug-id>`, `<slug>`, `<integration>`, `<production>` and
`<repro-details>` (the pasted Steps / Expected / Actual / Notes from Phase
0) in its prompt with the actual values. From Phase 2 onward, also
substitute `<repro-test-path>` with the path captured from Phase 1's
`REPRO: DONE — test: <path>` line.

Also append, unchanged, to every one of those subagent prompts:

> **Hand-back.** Your hand-back is the final text of your response, ending with exactly the signal line(s) this prompt asks for. Do not send it as a message through a messaging or hand-back tool, and never send placeholder text: the orchestrator reads only your final response, and a response without its signal is treated as blocked.

---

## Phase reference index

Phases 1–6 run as dedicated subagents. Each phase's full text — the exact
prompt to hand its subagent, plus the orchestrator's own steps before and
after the call — lives in its own file under `reference/`, so a run loads only
the phase it is about to execute instead of the whole lifecycle at once.
**Before starting Phase N, read its file in full**, then follow it exactly:
apply the placeholder substitution above, spawn the subagent with the prompt it
contains, and handle the result as the file says. Never reconstruct a prompt
from memory or from this index. The guardrails at the end of this file apply
across every phase, and other skills cite these phases by number
("`bug-fix` Phase 2"), so the numbering is stable.

| Phase | Subagent | Read before… | File |
| ----- | -------- | ------------ | ---- |
| 1 — Branch & Reproduce | Subagent 1 | choosing the branch/resume mode and writing the failing regression test | `reference/phase-1-reproduce.md` |
| 2 — Fix | Subagent 2 | implementing the fix | `reference/phase-2-fix.md` |
| 2R — Review gate | orchestrator + review subagents | the code review and the conditional database/security/viewport steps, with the orchestrator's own checks; Phase 3 starts only after it ends with `REVIEW_GATE: DONE` | `reference/review-gate.md` (started from `reference/phase-2-fix.md`) |
| 3 — Tests *(optional)* | Subagent 3 | running the project's tests | `reference/phase-3-tests.md` |
| 4 — E2E Verification *(optional)* | Subagent 4 (conditional) | deciding whether the end-to-end check applies and running it | `reference/phase-4-e2e-verification.md` |
| 5 — Build Verification *(optional)* | Subagent 5 | the local build | `reference/phase-5-build-verification.md` |
| 6 — Close | Subagent 6 | docs sync and archiving the bug | `reference/phase-6-close.md` |

Phase 7 (Commit) is short and stays below.

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
  `git checkout -b`, `git merge origin/<integration>` via `gitflow`'s
  start-fix/sync-fix, and `git merge origin/fix/<slug>` to pick up
  another device's already-pushed commits on this same fix).
- **The fix branch is synced from `<integration>` only, never `<production>`** — via
  `gitflow`'s sync-fix action, which merges `origin/<integration>` and nothing
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
