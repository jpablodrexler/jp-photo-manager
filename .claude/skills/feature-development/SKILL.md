---
name: feature-development
description: >
  Orchestrates the full feature lifecycle end-to-end: selects the next
  feature, creates its branch, implements the feature from its brief (or its
  OpenSpec artifacts), reviews the change and fixes the findings, then runs
  the phases that apply to the project — database and security reviews when
  schema or sensitive files changed, tests, an end-to-end check when a
  user-facing flow changed, a local build — skipping any that do not apply,
  verifies the implementation actually satisfies the feature's requirements
  (not just that tasks are checked off) before archiving, syncs the
  documentation, and marks the feature as implemented. Works with or without
  OpenSpec. Use when you want a fully automated feature development cycle
  with minimal manual steps. No git commits unless asked with --commit (then one
  commit, made by gitflow). TRIGGER when the user asks to develop a feature.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Orchestrate the full feature lifecycle from selection to archive using
dedicated subagents for each phase.

**Input**: Optional feature name or number, optionally followed by
`--skip-branch-setup` (e.g. `task-postpone --skip-branch-setup`) and/or
`--commit`. The
feature name/number, if provided, is passed to `features-next` to skip the
recommendation step and jump straight to confirmation for that feature.
`--skip-branch-setup` is for orchestrators — e.g. the `features-batch-development`
skill — that have already checked out a shared branch and are running
multiple features on it without committing between them; see Phase 1 Step
1.5's batch-mode branch below. Omit it for normal single-feature use —
default behavior (create/resume `feature/<change-name>` from `<integration>`) is
unchanged. `--commit` is an explicit opt-in that the user (or an orchestrator
acting on their request) passes to get one commit for this feature after it
is archived (Phase 7); without it nothing is committed.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`; `<reports>` is the `reports` folder next to the backlog's parent (`docs/reports` by default).

`<integration>` is `develop` and `<production>` is `main` unless `.backlog-kit.toml` sets `[git] integration_branch` / `production_branch`.

**The user talks, the skill reads flags.** `--skip-branch-setup` and `--commit` are shorthand for orchestrators;
a user never needs to type them or to phrase a request in any special way. "Develop the feature X", "develop X, stay on this branch", "develop X and commit as you go" are complete requests: map what the user said to the behaviour below, state in one line how it was understood (branch handling, commit cadence), and run every phase regardless of how short the request was.
Every phase runs by default; nothing in the request needs to ask for it, and only a skip condition written in this skill may skip one.

**Working on a branch the user is already on.** When the user is on a topic branch (not `<production>`/`<integration>`) and says to stay on it, treat the run as `--skip-branch-setup` even if the flag was not typed: Phase 1 Step 1.5 only confirms the branch and never creates or syncs one. Say so in the first line of the run.

**Committing as you go.** `--commit` means one commit, at Phase 7. When the user asks instead to commit as the work goes, that request is the opt-in for a commit after each phase that changed files (Phase 1, Phase 2, the fixes of each review gate, Phases 3 to 6S, Phase 6), each one made by `gitflow`'s commit action with the message `Feature <change-name>: <phase> — <one-line summary>`;
Phase 7 then commits only what is left, or records `COMMIT: nothing left`. The user's request stands in for the per-commit confirmation of the file list and message, but `gitflow` still makes every commit: no phase or subagent runs `git commit` itself.

**Phase ledger.** Right after Phase 0, create a task list with one entry per phase and gate that applies (1, 2, 2R: G1 to G5, 3, 4, 5, 6S, 6, 7) and keep it current: an entry is completed when its signal was verified, or when it was skipped under its own stated condition with the reason written on the entry.
The Final Summary is built from this list and is not shown while an entry is open. The order is fixed: archiving (Phase 6, including `openspec-sync-specs` and `openspec-archive-change`) comes after the review gate, the tests and the spec proof, never before;
a change archived early, or archived with the `openspec` command instead of those skills, is a deviation to report, not to hide.

**Never run `git commit`, `git push` or open a PR.**

The one exception is `--commit`: Phase 7 asks `gitflow`'s commit action to
make a single local commit, and only when the flag was passed. Never push,
and never commit any other way.

**Every phase runs, or is skipped under its own stated skip condition and
the reason recorded.** Running a phase inline in the orchestrator's context
instead of in a subagent is fine; merging phases, dropping one, or calling
one "optional" when its skip condition is not met is not. The Final Summary
ends with a status table for every phase and a feature with a missing row is
not done.

---

## Overview

Phase 0 runs directly in the orchestrator's own context (never a subagent —
see that phase's own rationale below), Phases 1–6 each run as a dedicated
subagent. **Phases 3, 4 and 5 are optional**, and so are the database review,
the security review and the viewport check inside Phase 2R: each one starts by
the orchestrator checking whether it applies to the project and to this change
(never the implementation subagent), and when it does
not it is skipped with a one-line reason that appears in the Final Summary.
A project with a test command and nothing else therefore runs one phase; a
project with a schema, an end-to-end suite and a build runs them all.

| Phase                        | Subagent   | Skills / actions                                                                                                                                  |
| ----------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0 — Select & Confirm          | *(orchestrator's own context — never a subagent)* | `features-next` (recommend + mandatory `AskUserQuestion` confirmation) |
| 1 — Propose                   | Subagent 1 | `git fetch origin --prune` to detect a branch/commits pushed from another device → `gitflow` (start feature to create `feature/<change-name>`, resume + merge in another device's pushed commits, or sync feature to catch up an existing one with `<integration>`) → mark the feature `🔶 In Progress` in `<backlog>/features-planned.md` → `openspec-propose` (if artifacts missing) |
| 2 — Implement                 | Subagent 2 | implement from the brief (`openspec-apply-change <name>` in an OpenSpec project); reviews are not its job |
| 2R — Review gate              | orchestrator + one subagent per review | the orchestrator computes `CHANGED_FILES`, decides which conditional reviews apply and verifies every report; separate subagents run `code-reviewer`, then — only when the change touches them — `database-reviewer`, `security-reviewer` and the mobile/desktop viewport check; findings fixed before done (`reference/review-gate.md`) |
| 3 — Tests *(optional)*        | Subagent 3 | runs the project's tests (and its fast smoke tier, if CI runs one) until they pass; skipped if the project has no automated tests |
| 4 — E2E verification *(optional, conditional)* | Subagent 4 | runs the project's end-to-end/browser checks when the change touches a user-facing flow — skipped otherwise, or if the project has no such suite; how the test account is provided is decided by the orchestrator itself, before spawning Subagent 4, since that decision needs `AskUserQuestion` |
| 5 — Build verification *(optional)* | Subagent 5 | runs the project's build locally to catch build failures before CI or the host would; skipped if the project has no build step |
| 6S — Spec proof               | Subagent 6 | `spec-compliance-check` (or a manual proof), unverified scenarios closed with tests; the orchestrator opens the report and checks `Failing` and `Unverified` are both 0 |
| 6 — Archive                   | Subagent 7 | syncs the docs, `openspec-sync-specs <name>` then `openspec-archive-change <name>` (OpenSpec projects), `features-archive <name>` |
| 7 — Commit *(only with `--commit`)* | orchestrator | `gitflow` commit action: one local commit holding the implementation, its tests and the backlog move |

---

## Phase 0 — Select & Confirm (orchestrator's own context — never a subagent)

**Never delegate this phase to a subagent, even under "Auto Mode" or a similar autonomous-operation
instruction.** Which feature gets built next is the user's decision, not a default to pick in order to avoid
stopping. A spawned or backgrounded subagent may lack reliable `AskUserQuestion` access, or its prompt may not
reach the user in time, so only running the phase in the context that executes this skill (the main conversation,
or `features-batch-development`'s inline loop) guarantees the confirmation happens.

1. **OpenSpec is optional.** If the repo has an `openspec/` folder, run
   `openspec --version` yourself; if it fails or is not found, tell the
   user `PROPOSE_BLOCKED — openspec CLI not found` and stop the whole
   workflow here — do not proceed to feature selection with a broken
   toolchain. With no `openspec/` folder, skip OpenSpec entirely: the
   feature brief `<backlog>/features/NNN-<name>.md` is the spec.
2. Use the Skill tool to invoke the `features-next` skill directly — not
   wrapped in an Agent call, not inside any subagent — passing the argument
   given to this skill (feature name/number), if any, or no argument
   otherwise. Let it present its recommendation and ask for confirmation via
   `AskUserQuestion` in this same context, exactly as `features-next` is
   designed to; do not pre-answer or skip that question yourself.
3. If `features-next` returns without a `CHANGE_NAME:` line (the user
   cancelled, or `features-next` itself couldn't obtain confirmation — see
   its own guardrails), stop the entire workflow here and tell the user.
   Never proceed to Phase 1 without a confirmed change name in hand.
4. Otherwise, capture the confirmed change name from the `CHANGE_NAME:` line.
   This is the value substituted for `<change-name>` in every phase below,
   including Subagent 1's prompt — Subagent 1 is given this name directly
   and never re-selects or re-confirms a feature on its own.

---
## Phase reference index

Phases 1–6 run as dedicated subagents. Each phase's full text — the exact
prompt to hand its subagent, plus the orchestrator's own steps before and
after the call — lives in its own file under `reference/`, so a run loads only
the phase it is about to execute instead of the whole lifecycle at once.
**Before starting Phase N, read its file in full**, then follow it exactly:
apply the placeholder substitution below, spawn the subagent with the prompt it
contains, and handle the result as the file says. Never reconstruct a prompt
from memory or from this index. The guardrails at the end of this file apply
across every phase, and other skills cite these phases by number
("`feature-development` Phase 2"), so the numbering is stable.

| Phase | Subagent | Read before… | File |
| ----- | -------- | ------------ | ---- |
| 1 — Propose | Subagent 1 | selecting the branch/resume mode, the spec step (OpenSpec projects), the dependency check | `reference/phase-1-propose.md` |
| 2 — Implement | Subagent 2 | implementing | `reference/phase-2-implement-review.md` |
| 2R — Review gate | orchestrator + review subagents | the code review and the conditional database/security/viewport steps, with the orchestrator's own checks | `reference/review-gate.md` |
| 3 — Tests *(optional)* | Subagent 3 | running the project's tests | `reference/phase-3-tests.md` |
| 4 — E2E Verification *(optional)* | Subagent 4 (conditional) | deciding whether the end-to-end check applies and running it | `reference/phase-4-e2e-verification.md` |
| 5 — Build Verification *(optional)* | Subagent 5 | the local build | `reference/phase-5-build-verification.md` |
| 6S — Spec proof | Subagent 6 | proving each requirement, with the orchestrator's report check | `reference/phase-6-archive.md` |
| 6 — Archive | Subagent 7 | docs sync, archiving the change and marking the feature implemented | `reference/phase-6-archive.md` |

---
## Placeholder substitution (Phases 1–6)

Before spawning any subagent in Phases 1–6, replace every `<integration>` / `<production>` with the branch names above, and every occurrence of
`<change-name>` in that subagent's prompt with the actual change name
confirmed in Phase 0, and every `<backlog>` / `<reports>` with the folder
named above.

Also append, unchanged, to every one of those subagent prompts:

> **Hand-back.** Your hand-back is the final text of your response, ending with exactly the signal line(s) this prompt asks for. Do not send it as a message through a messaging or hand-back tool, and never send placeholder text: the orchestrator reads only your final response, and a response without its signal is treated as blocked.

---

## Final Summary

After all phases complete, display:

```
## Feature Development Complete

**Change:** <change-name>
**Spec:** ✓ Created (or N/A — no OpenSpec in this project)
**Implementation:** ✓ All tasks complete
**Code review:** ✓ All findings resolved
**Database review:** ✓ All findings resolved (or N/A — no schema changes)
**Security review:** ✓ All findings resolved (or N/A — no security-sensitive changes)
**Mobile viewport check:** ✓ <each configured phone viewport: verified, or ⚠ skipped — reason> — <what was checked/caught> (or N/A — no template/CSS changes)
**Desktop viewport check:** ✓ <the configured desktop viewport: verified, or ⚠ skipped — reason> — <what was checked/caught> (or N/A — no template/CSS changes)
**Tests:** ✓ All passing (or N/A — the project has no automated tests)
[if UNREVIEWED_PROD_FIXES was recorded in Phase 3 or Phase 5, insert this line here:]
**⚠ Unreviewed production fixes:** <the PROD_CODE_FIXED lines> — fixed while
chasing test or build failures in Phase 3 or 5; the user chose to proceed without routing
them back through the Phase 2R review gate.
**E2E verification:** ✓ <one-line summary from Phase 4's `E2E: PASS`> (or
"N/A — <reason>" if `E2E: SKIPPED`, or **⚠ Partial:** <what wasn't covered> if `E2E: PASS_PARTIAL`)
**Build:** ✓ the build succeeded locally (or N/A — the project has no build step)
**Definition of Done:** ✓ <each item of `<backlog>/definition-of-done.md` and of the brief's own `## Definition of Done` section, met with the phase or report that shows it, or ⚠ not met — why> (or N/A — the project defines none)
**Spec compliance:** ✓ All scenarios verified (any Unverified scenario `spec-compliance-check` found was closed with new test coverage during Phase 6S, before archiving — never left as a caveat)
**Docs sync:** ✓ <one-line summary from `docs-sync`, or "Nothing to sync">
**SDD change:** ✓ Archived (OpenSpec projects)
**Feature:** ✓ Marked as implemented
**Commit:** ✓ <short hash> (or N/A — not requested)

| Phase | Status |
| ----- | ------ |
| 1 Propose / branch | ✓ / ✗ |
| 2 Implement | ✓ / ✗ |
| 2R Code review (report path) | ✓ / ✗ |
| 2R DB review / security review / viewport check (report paths) | ✓ / N/A — <the orchestrator's reason> |
| 3 Tests | ✓ / N/A — <reason> |
| 4 E2E | ✓ / N/A — <reason> |
| 5 Build | ✓ / N/A — <reason> |
| 6S Spec proof (report path) | ✓ / ✗ |
| 6 Docs sync, archive | ✓ / ✗ |
| 7 Commit | ✓ / N/A — not requested |
```

If `--commit` was not passed, say plainly that nothing was committed and that the work is on `feature/<change-name>` for review.

---

## Guardrails

**Order and signals**
- Substitute the change name confirmed in Phase 0 for every `<change-name>` in every subagent prompt of Phases 1–6.
- If `features-next` (Phase 0) returns without a `CHANGE_NAME:` line, treat it as cancellation: stop before Subagent 1 is spawned and tell the user. Subagent 1 never selects or cancels a feature.
- Do not start a phase until the one before it returned its signal:
  - Phase 2: Phase 1 confirmed the branch is ready (in an OpenSpec project, all `applyRequires` artifacts `done`). `PROPOSE_BLOCKED` is surfaced and stops the run. `PROPOSE_NEEDS_DEPENDENCY_DECISION` is not a failure but also blocks Phase 2: resolve it through the decision point under Phase 1; only an explicit "proceed anyway" continues. This catches a missing dependency right after design instead of mid-implementation.
  - Phase 2R: `IMPLEMENT: DONE`. Phase 3: the gate returned `REVIEW_GATE: DONE`. An `IMPLEMENT_BLOCKED`, `REVIEW_BLOCKED`, `DATABASE_BLOCKED` or `SECURITY_BLOCKED` is surfaced, and the run waits for the user.
  - Phase 4: Phase 3 reported `PASS` (both tiers, the tests and the smoke tier if the project has one) or `TESTS: SKIPPED`. `BLOCKED` is surfaced and waits. `PROD_CODE_FIXED:` lines are surfaced with the choice to proceed (carry `UNREVIEWED_PROD_FIXES` into the Final Summary) or re-run the Phase 2R gate.
  - Phase 5: Phase 4 reported `E2E: PASS`, `E2E: PASS_PARTIAL` (not blocking; carry the caveat into the Final Summary) or `E2E: SKIPPED`. On `E2E_BLOCKED`, surface and wait: the cause may be environment or data, so do not send it back to the review gate without the user confirming.
  - Phase 6S: Phase 5 reported `BUILD: PASS` or `BUILD: SKIPPED`. `BUILD: BLOCKED` is surfaced and waits. `PROD_CODE_FIXED:` lines are handled as in Phase 3 (proceed, or re-run the gate and then Phases 3 and 5).
  - Phase 6: Phase 6S returned `SPEC_PROOF: PASS` **and the orchestrator opened its report** (`Failing` = 0, `Unverified` = 0). The proof runs in its own subagent, never in the archiving one.
  - Final Summary: Phase 6 returned `ARCHIVE: DONE`. If Phase 6S or 6 returns `ARCHIVE_BLOCKED` (a Failing scenario, a test added for an Unverified scenario that itself failed, or an Unverified scenario left after 3 add-test-and-re-check rounds), the change stays unarchived: surface the scenarios and wait; never retry Phase 6 on your own.
- **Phase 7 (only with `--commit`):** after `ARCHIVE: DONE`, invoke `gitflow` with "commit `Feature <change-name>: <one-line summary>`" directly in the orchestrator's context. It shows what will be committed, confirms with the user, makes one local commit and does not push. Without the flag (and without a request to commit as the work goes) record `COMMIT: SKIPPED — not requested`. A refusal or failure is surfaced and stops there; the feature stays archived, uncommitted.
- **Never collapse the phases to save effort.** Implementing and testing in one go, running only the touched specs, skipping review or build and giving a lean summary are what this skill exists to prevent. A skip needs its stated condition and a recorded reason.
- **Never archive a change with a known spec-compliance gap.** Phase 6S closes every Unverified scenario with real, passing test coverage, or escalates with `ARCHIVE_BLOCKED`, before Phase 6 runs. There is no caveat path: a scenario is Verified before archiving, or the archive does not happen.
- **Missing signal fallback:** a subagent that returns without its expected signal is treated as `BLOCKED`: surface its raw response and wait. Before re-running a phase whose signal is missing, look at the working tree for what the subagent actually did (for example `git status`, the backlog row, the test file) and tell the user; whether to accept that evidence in place of the signal or to re-run the phase is the user's decision.

**Subagents and confirmations**
- **Foreground:** pass `run_in_background: false` on every Agent call (Subagents 1–7) where the tool supports it. Phases are gated on the previous subagent finishing; if the tool spawns every call asynchronously anyway, wait for the real completion notification before advancing, and never assume or fabricate a result. Never delegate an `AskUserQuestion` decision to a subagent to work around this: Phase 0 and Phase 4's credential-source pre-step run in the orchestrator's own context for exactly that reason.
- **Nested backgrounding:** a subagent that backgrounds a shell command (`run_in_background: true`) and then ends its response is wrong in every phase: nothing wakes a finished subagent when its background child completes, so the hand-back would carry the response text, not the result. A step that can outlast the Bash tool's 10-minute cap must poll with repeated **foreground** Bash calls within the same uninterrupted turn.
- **Auto Mode (or any autonomous-operation instruction) never overrides a required confirmation:** Phase 0's feature selection (`features-next`), Phase 3's `PROD_CODE_FIXED` choice, Phase 4's test-account choice, a question about which command runs a phase, and any guidance prompt after a `*_BLOCKED` result. "Make the reasonable call" applies to implementation judgment (which helper, how to phrase something), never to a point this skill designed as a human decision. A confirmation skipped for this reason is a bug in how the skill was invoked.
- **Phase 4's test account is decided once, by the orchestrator, before Subagent 4 is spawned.** The orchestrator asks (`AskUserQuestion`) in its own context (main conversation, or `features-batch-development`'s inline loop) and passes the answer in as `<CREDENTIAL_SOURCE>`. A second account for a cross-user check is used only if one exists; otherwise the check is reported as not covered via `E2E: PASS_PARTIAL`. Credentials files are never read by the orchestrator and their contents never appear in a prompt, response or report.
- **Skills run as written, by whoever performs the phase.** A phase that names a skill (`code-reviewer`, `database-reviewer`, `security-reviewer`, `spec-compliance-check`, `docs-sync`, `openspec-sync-specs`, `openspec-archive-change`, `features-archive`) invokes that skill; doing the same checks by hand, or writing its report by hand, is a failed phase even when the content looks right. Reports carry the stamp the reviewer skills write (`<!-- backlog-kit: <skill> -->`), and the orchestrator checks it (review gate G6, Phase 6S check).

**Reviews**
- **The reviews are never run inside the implementation subagent.** Phase 2R runs each in its own subagent. The orchestrator, not Subagent 2, decides which conditional reviews apply (from its own `git status --porcelain`) and verifies each report file and its checkboxes before accepting a signal (`reference/review-gate.md`, G6). A skip is recorded with the orchestrator's reason; a review signal without its report is treated as `BLOCKED`.
- **Phase 2R uses each reviewer's Review workflow, scoped to the change, never its interactive Fix workflow and never a full-codebase sweep.** The Fix workflow asks the user which finding to work on next and does not fit an unattended subagent;
  a full sweep splits `code-reviewer` into one report per layer. The review subagent fixes findings itself and updates the report's checkboxes directly.
  `database-reviewer` runs only when gate G2 detects a migration or schema file change, and fixes an already-applied migration with a new migration, never by editing it.
  A reviewer skill that is not installed is replaced by the review subagent's own review of the same concerns, flagged `SKILL_USED: none — fallback` and carried into the Final Summary.

**What the phases may touch**
- **Phase 5 never deploys anything:** the build is a local check only. The skill never invokes a deployment tool, never pushes to trigger a deploy and never touches the host's settings.
- **Phase 3's smoke tier always runs.** A smoke tier that CI runs on every PR runs on every invocation of Phase 3, whatever `<change-name>` touched: unlike Phase 4's end-to-end check, which depends on a user-facing flow, CI does not predict which changes could break it.
- **Phase 4 only starts the application locally and reads/writes no repository files.** Its one write to a shared database is applying this change's own pending migrations, and only after a dry run shows nothing else pending (Phase 4 Step 2); it never asks the user to do that. It starts a stopped container runtime (Docker) itself. A schema fix needed to make Phase 4 pass goes back to the Phase 2R database review (re-run the gate after fixing it).
- **Docker is started by the workflow, never reported as a blocker.** A phase whose tooling needs a local container runtime (a local database test suite in Phase 3 or 6, Phase 4's environment) starts it and waits, and blocks only if it never comes up.
- **Step 1.6 marks the backlog row `🔶 In Progress` as soon as the branch is ready, in normal and batch mode,** before Step 2's artifact check, so a change that still needs `openspec-propose` is marked too. This is what lets `features-next` recommend resuming an interrupted feature. It only flips `⬜ Pending` → `🔶 In Progress`, never touches a row already `🔶 In Progress` or `✅ Implemented`, and never blocks when the row is missing (an ad hoc change). `features-archive` moves the row in Phase 6.

**Branches and git**
- **Work happens on `feature/<change-name>`, except in batch mode, where it is the branch the orchestrator already set up.** In normal mode Phase 1 Step 1.5 is the only place a branch is created, switched or synced, before any file is created or modified (the SDD artifacts of `openspec-propose` included).
  It invokes `gitflow` (start feature) to create the branch, or resumes an existing one: checkout (from the local branch, or tracking `origin/feature/<change-name>` when only the remote copy exists), merging commits pushed from another device, and `gitflow`'s sync feature action to catch up with `<integration>`.
  In batch mode (`--skip-branch-setup`) Step 1.5 only confirms the current branch is not `<production>` or `<integration>` and leaves branch state to the caller;
  that flag affects nothing else, and a normal single-feature invocation never passes it. Phases 2–6 never re-derive or assume `feature/<change-name>` as the literal branch name; they use whatever is checked out. Phases 2–6 stay on the branch current when Phase 1 finished: none may run `git checkout`, `git switch` or `git merge`, invoke `gitflow` or create a branch.
  A subagent found on another branch is a workflow bug: surface it, never switch silently.
- **The feature branch is synced from `<integration>` only, never `<production>`.** The resume path uses `gitflow`'s sync feature action, which merges `origin/<integration>` and nothing else, even right after a release or hotfix merged into both. No phase may merge, rebase or pull `<production>` into the feature branch.
- **No git commits unless `--commit` was passed or the user asked to commit as the work goes, and then only through `gitflow`'s commit action** (in Phase 7, or after each phase when the user asked to commit as the work goes).
  Neither this skill nor any subagent runs `git commit`, `git push` or another git write itself. The sole exceptions are Phase 1 Step 1.5's branch creation and sync (`git fetch`, `git checkout`, `git checkout -b`, `git merge origin/<integration>`, and `git merge origin/feature/<change-name>` to pick up commits pushed from another device, directly or via `gitflow`'s start-feature and sync-feature actions).
  Those merges bring in already-reviewed or already-pushed work, not new uncommitted implementation.

**Secrets**
- **Never let a subagent, or this orchestrator, print, echo or materialize a generated secret's raw value** (a private key, API token, password, signing key) in a response, report or file.
  A Phase 2 subagent once generated a signing keypair and printed the live private key in its hand-back. Phase 2's prompt tells the subagent to leave any task that needs secret disclosure unchecked, with a note naming the manual command, but the orchestrator does not rely on that alone, in any phase.
  If a hand-back appears to contain a live secret, treat it as compromised even if it never reached a committed file: tell the user which value it is, why it is burned and that it must be regenerated out-of-band by the user (never by re-running the generation step through this workflow), and do not repeat it in any later response, file or summary.
