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
  version: "0.1"
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
default behavior (create/resume `feature/<change-name>` from `develop`) is
unchanged. `--commit` is an explicit opt-in that the user (or an orchestrator
acting on their request) passes to get one commit for this feature after it
is archived (Phase 7); without it nothing is committed.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`; `<reports>` is the `reports` folder next to the backlog's parent (`docs/reports` by default).

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
| 1 — Propose                   | Subagent 1 | `git fetch origin --prune` to detect a branch/commits pushed from another device → `gitflow` (start feature to create `feature/<change-name>`, resume + merge in another device's pushed commits, or sync feature to catch up an existing one with `develop`) → mark the feature `🔶 In Progress` in `<backlog>/features-planned.md` → `openspec-propose` (if artifacts missing) |
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

**This phase must never be delegated to a spawned subagent, under any
circumstances — including when an "Auto Mode" or similar autonomous-operation
instruction is active.** Selecting which feature gets built next is always
the user's decision, not a "reasonable default" the model makes on their
behalf to avoid stopping — that bias, wherever it comes from, applies to
implementation judgment calls, never to this decision. A spawned subagent
(via the Agent tool) may run without reliable access to `AskUserQuestion`
depending on the environment, and even where it nominally has access, a
backgrounded/async agent's confirmation prompt may not reach the user in
time for the workflow to actually wait on it. Running this phase directly in
whatever context is currently executing this skill (the main conversation,
or `features-batch-development`'s inline loop) is the only way to
*guarantee* the confirmation happens — it sidesteps the question of what a
given subagent call can or can't surface entirely, rather than relying on a
flag or an assumption about subagent behavior.

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
| 6S — Spec proof | Subagent 6 | proving each requirement, with the orchestrator's report check |
| 6 — Archive | Subagent 7 | docs sync, archiving the change and marking the feature implemented | `reference/phase-6-archive.md` |

---
## Placeholder substitution (Phases 1–6)

Before spawning any subagent in Phases 1–6, replace every occurrence of
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
[if UNREVIEWED_PROD_FIXES was recorded in Phase 3, insert this line here:]
**⚠ Unreviewed production fixes:** <the PROD_CODE_FIXED lines> — fixed while
chasing test failures in Phase 3; the user chose to proceed without routing
them back through the Phase 2R review gate.
**E2E verification:** ✓ <one-line summary from Phase 4's `E2E: PASS`> (or
"N/A — <reason>" if `E2E: SKIPPED`, or **⚠ Partial:** <what wasn't covered> if `E2E: PASS_PARTIAL`)
**Build:** ✓ the build succeeded locally (or N/A — the project has no build step)
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

- Always capture and propagate the change name confirmed in Phase 0 to all
  later phases. Before spawning any subagent in Phases 1–6, substitute every
  `<change-name>` occurrence in its prompt with that confirmed value.
- **Cancellation detection**: if `features-next` (invoked directly in Phase 0)
  returns without a `CHANGE_NAME:` line, treat it as user cancellation —
  stop the workflow immediately, before Subagent 1 is ever spawned, and
  inform the user. Subagent 1 itself is never given the opportunity to
  cancel feature selection — by the time it runs, the choice is final.
- Do not start Phase 2 until Phase 1 confirms the branch is ready and, in an
  OpenSpec project, that all `applyRequires` artifacts are `done`. If Phase 1 returns `PROPOSE_BLOCKED`, surface the
  details to the user and stop.
- **Do not start Phase 2 if Phase 1 returns `PROPOSE_NEEDS_DEPENDENCY_DECISION`
  either**, even though it isn't a failure — this workflow must never barrel
  into implementing a feature that Step 3.5 just discovered depends on
  something not yet built. Resolve it through the AskUserQuestion decision
  point described under Phase 1 first; only an explicit "proceed anyway"
  answer continues to Phase 2. This is what closes the gap that previously
  let a feature's missing dependency surface as a surprise mid-implementation
  instead of being caught, recorded, and decided on right after design.
- **The reviews are never run inside the implementation subagent.** Phase 2R runs each in its own subagent, and the orchestrator — not Subagent 2 — decides which conditional reviews apply (from its own `git status --porcelain`) and verifies each report file and its checkboxes before accepting a signal (`reference/review-gate.md`, G6). A skip is recorded with the orchestrator's reason; a review signal without its report is treated as `BLOCKED`.
- Do not start Phase 2R until Phase 2 returns `IMPLEMENT: DONE`, and do not start Phase 3 until the gate returns `REVIEW_GATE: DONE`. If Phase 2 or the gate
  returns `IMPLEMENT_BLOCKED`, `REVIEW_BLOCKED`, `DATABASE_BLOCKED`, or
  `SECURITY_BLOCKED`, surface the details to the user and wait for guidance
  before continuing.
- Do not start Phase 4 until Phase 3 reports `PASS` — meaning **both** tiers
  passed (the tests and, if the project has one, the smoke tier), not just the
  first of the two — or was skipped (`TESTS: SKIPPED`). If it reports `BLOCKED`,
  surface the details to the user and wait for guidance before continuing. If
  it reports `PROD_CODE_FIXED:` lines, surface them to the user and confirm
  whether to proceed or re-run the Phase 2R review gate.
- **A smoke tier that CI runs on every PR always runs in Phase 3, on every
  invocation of the phase — it is never conditional on what `<change-name>`
  touched.** Unlike Phase 4's end-to-end check, which only runs when the change
  touches a user-facing flow, CI runs that tier unconditionally, so this
  workflow must mirror that rather than trying to predict which changes could
  affect it — a change that looks entirely unrelated can still break one.
- Do not start Phase 5 until Phase 4 reports `E2E: PASS`, `E2E: PASS_PARTIAL`,
  or `E2E: SKIPPED`. If it reports `E2E_BLOCKED`, surface the details to the
  user and wait for guidance before continuing — do not assume it belongs
  back in Phase 2's code review without the user confirming that (a failure
  here can equally be an environment/data problem, not a code defect). If it
  reports `E2E: PASS_PARTIAL`, that's not blocking — carry the caveat into the
  Final Summary's "E2E verification" field.
- Do not start Phase 6 until Phase 5 reports `BUILD: PASS` or `BUILD: SKIPPED`.
  If it reports `BUILD: BLOCKED`, surface the details to the user and wait for
  guidance before continuing.
- Do not start Phase 6 until Phase 6S returned `SPEC_PROOF: PASS` **and the orchestrator opened its report** (`Failing` = 0, `Unverified` = 0). The proof runs in its own subagent, never in the archiving one.
- Do not display the Final Summary until Phase 6 subagent returns `ARCHIVE: DONE`.
- **Phase 7 (only with `--commit`):** after `ARCHIVE: DONE`, invoke `gitflow` with "commit `Feature <change-name>: <one-line summary>`" directly in the orchestrator's context. It shows what will be committed, confirms with the user, makes one local commit and does not push. Without the flag record `COMMIT: SKIPPED — not requested`. A refusal or failure is surfaced and stops there; the feature stays archived, uncommitted.
- **Never collapse the phases to save effort.** Implementing and testing in one go, running only the touched specs, skipping review or build, and giving a lean summary are what this skill exists to prevent. A skip needs its stated condition and a recorded reason.
  If it returns `ARCHIVE_BLOCKED` instead — Phase 6S's `spec-compliance-check`
  found a Failing scenario, a test added to close an Unverified scenario
  itself failed (revealing a real bug), or an Unverified scenario survived
  3 rounds of add-test-and-re-check — the change stays unarchived. Surface
  the scenario(s) to the user and wait for guidance instead of retrying
  Phase 6 automatically.
- **This workflow never archives a change with a known spec-compliance
  gap.** Phase 6S closes every Unverified scenario with real,
  passing test coverage (or escalates via `ARCHIVE_BLOCKED` when it
  genuinely can't) before Step 2 ever runs. There is no
  `UNVERIFIED_SCENARIOS` caveat path anymore — a scenario is either
  Verified before archiving, or the archive doesn't happen.
- **Foreground guardrail**: every Agent tool call in this skill (Subagents
  1–6) must pass `run_in_background: false` where the tool implementation
  in use actually supports that parameter. Every phase in this workflow is
  gated on the prior phase's subagent actually finishing ("do not start
  Phase N until Subagent M returns ..."); some Agent tool implementations
  spawn every call asynchronously regardless of any such flag (in which
  case there is no synchronous option to request — the orchestrator must
  instead genuinely wait for that subagent's completion notification before
  advancing, never assume or fabricate a result), so don't treat the
  presence of a `run_in_background: false`-equivalent parameter as
  something to rely on for correctness — always wait for a real result
  either way. **Never delegate an `AskUserQuestion` decision point to a
  spawned subagent as a workaround for this** — that is what Phase 0 (and
  Phase 4's credential-source pre-step below) exist to avoid entirely, by
  running the confirmation directly in the orchestrator's own context
  instead of trying to make a subagent's confirmation reliable.
- **Auto Mode (or any other autonomous-operation instruction) never
  overrides a required user confirmation anywhere in this workflow.** This
  applies to every `AskUserQuestion` decision point this skill or a skill it
  invokes raises: Phase 0's feature selection (`features-next`), Phase 3's
  `PROD_CODE_FIXED` proceed-or-re-review choice, Phase 4's test-account
  choice, a question about which command runs a phase, and any
  blocker-guidance prompt
  after a `*_BLOCKED` result. "Make the reasonable call instead of stopping
  to ask" is guidance for implementation judgment calls (which helper to
  use, how to phrase something) — it is never license to pick an
  unambiguous-looking top option and proceed past a point this skill
  designed to be a genuine human decision. If a confirmation step is ever
  skipped for this reason, that is a bug in how the skill was invoked, not
  acceptable behavior.
- **Missing signal fallback**: if any subagent returns without its expected
  signal, treat it as `BLOCKED`, surface the subagent's raw response to the
  user, and wait for guidance before proceeding to the next phase.
  Before re-running a phase whose signal is missing, look at the working tree
  for what the subagent actually did (for example `git status`, the backlog
  row, the test file) and tell the user; whether to accept that evidence in
  place of the signal or to re-run the phase is the user's decision.
- **Nested backgrounding guardrail**: any subagent in this workflow that
  backgrounds a long-running shell command (via the Bash tool's
  `run_in_background: true`) and then ends its response is making a mistake,
  regardless of which phase it's in. Ending a turn after backgrounding work
  resolves that subagent's own `run_in_background: false` Agent-tool call back
  to whichever agent spawned it — with the response text as-is, not with the
  eventual result — because nothing automatically wakes an already-finished
  subagent back up when its background child completes; only an explicit
  `SendMessage` from a still-live parent can resume it, and nothing in this
  workflow does that automatically. Any subagent step whose real duration can
  exceed the Bash tool's 10-minute blocking cap must poll for completion with
  repeated **foreground** Bash calls within the same, uninterrupted turn —
  never end the response and rely on being notified later.
- **`code-reviewer` has two workflows; Phase 2R always uses Review, never
  Fix.** The skill's interactive Fix Workflow asks the user which
  category/finding to work on next and is meant to be steered turn-by-turn
  across a conversation — it does not fit an unattended subagent. Phase 2R
  always invokes the plain Review workflow and fixes findings itself,
  updating the resulting report's checkboxes directly.
- **Phase 2R's code review is always scoped, never a full-codebase sweep.**
  `code-reviewer` splits into one report per architecture layer only when
  asked to review the entire codebase. Phase 2R reviews a single change's
  files, so it must stay on the single-report path.
- **`database-reviewer` follows the same conditional-trigger, scoped-Review,
  never-Fix pattern as `code-reviewer`.** It only runs when the orchestrator's gate G2 detects a
  migration or schema file changed; its own review subagent fixes findings itself (a new
  migration for any fix to an already-applied one, never an edit of the existing
  file) and updates the resulting report's checkboxes directly. **`security-reviewer`**
  has the same two-workflow and scoped-vs-sweep split; Phase 2R always uses
  Review, scoped, never Fix or a full sweep. Either skill, if not installed, is
  replaced by the review subagent's own review of the same concerns, flagged `SKILL_USED: none — fallback` and carried into the Final Summary.
- **Phase 5 never deploys anything.** The build is run purely as a local check;
  this skill never invokes a deployment tool, never pushes to trigger a deploy,
  and never touches the host's settings.
- **Phase 4's test-account choice is decided once, by the orchestrator, before
  Subagent 4 is ever spawned — never by Subagent 4 itself.** The orchestrator
  asks the user (`AskUserQuestion`) in its own context (main conversation, or
  `features-batch-development`'s inline loop) and passes the answer into
  Subagent 4 as `<CREDENTIAL_SOURCE>`; Subagent 4 cannot reliably access
  `AskUserQuestion`. A second account for a cross-user check is used only if one
  genuinely exists; otherwise the check is reported as not covered via
  `E2E: PASS_PARTIAL`. Credentials files are never read by the orchestrator and
  their contents never appear in a prompt, a response or a report.
- **Phase 4 only starts the application locally and reads/writes no repository
  files.** The one write it makes to a shared database is applying *this
  change's own* pending migrations, and only after a dry run shows nothing else
  pending (see Phase 4 Step 2) — it never stops to ask the user to do that
  themselves. It starts a stopped container runtime (Docker) itself too. It
  makes no other write against a shared database. Any schema fix needed to
  make Phase 4 pass belongs back in Phase 2's conditional database review.
- **Docker is started by the workflow, never reported as a blocker.** Any
  phase whose tooling needs a local container runtime (a local database test
  suite in Phase 3 or 6, Phase 4's environment) starts it and waits for it
  before running, and only blocks if it never comes up.
- **Step 1.6 marks the backlog row `🔶 In Progress` as soon as the branch to
  work on is ready, in both normal and batch mode.** This is what lets
  `features-next` recommend resuming an interrupted feature instead of
  starting a new one, and it runs before Step 2's artifact check so a
  change that still needs `openspec-propose` is marked in progress too, not
  only one that already has artifacts. It only ever flips `⬜ Pending` →
  `🔶 In Progress`; it never touches a row already showing `🔶 In Progress`
  or `✅ Implemented`, and it never blocks the workflow if the row is
  missing (ad hoc changes outside the tracked backlog). Phase 6's
  `features-archive` is what eventually moves the row to the implemented table.
- **`--skip-branch-setup` (batch mode) only ever affects Phase 1 Step 1.5.**
  No other phase's behavior changes — Phases 2–6 already just operate on
  "whatever branch is currently checked out" and never re-derive or assume
  `feature/<change-name>` as the literal branch name outside that one step.
  This flag exists solely for orchestrators like `features-batch-development`
  that run several features on one shared branch without committing between
  them; it is never passed by a normal, single-feature invocation of this
  skill.
- **Work always happens on a `feature/<change-name>` branch — except in
  batch mode, where it's whatever branch the orchestrator already
  established.** In normal mode, Phase 1's Step 1.5 is the only place a
  branch is created, switched, or synced — it invokes the `gitflow` skill
  (start feature) to create `feature/<change-name>`, or, if it already
  exists from a prior run (locally, or only on the remote because another
  device pushed it first), resumes it (checking it out — from the local
  branch, or tracking `origin/feature/<change-name>` directly if only the
  remote copy exists — merging in any commits pushed from another device,
  and, via `gitflow`'s sync feature action, bringing it up to date with
  `develop`), before any file in the repository is created or modified,
  including the SDD artifacts written by `openspec-propose`. In batch mode
  (`--skip-branch-setup`), Step 1.5 performs none of that — it only
  confirms the current branch isn't `main`/`develop` and otherwise leaves
  branch state entirely to the caller. Either way, Phases 2–6 must stay on
  whatever branch was current when Phase 1 finished; none of them may run
  `git checkout`, `git switch`, `git merge`, invoke `gitflow`, or create
  another branch. If a subagent finds itself on a different branch, that is
  a bug in the workflow — surface it to the user rather than silently
  switching.
- **The feature branch is never synced from `main`, only `develop`.**
  Step 1.5's resume path uses `gitflow`'s sync feature action exclusively,
  which merges `origin/develop` and nothing else — matching `gitflow`'s own
  guardrail that `main` is never a valid source for a feature branch's
  content, even right after a release/hotfix has merged into both `main`
  and `develop` and the two look momentarily interchangeable. No phase in
  this workflow may merge, rebase, or pull `main` into the feature branch
  under any circumstance.
- **No git commits unless `--commit` was passed, and then only through
  `gitflow`'s commit action in Phase 7.** Neither this skill nor any
  subagent it spawns may run `git commit`, `git push`, or any other git
  write command itself at any point in the workflow. Branch creation and branch sync
  (`git fetch`, `git checkout`, `git checkout -b`, `git merge
  origin/develop`, and `git merge origin/feature/<change-name>` to pick up
  commits pushed from another device — all only in Phase 1's Step 1.5, the
  latter two either directly or via `gitflow`'s start-feature/sync-feature
  actions) are the sole exceptions. A sync-feature merge commit brings in
  already-reviewed upstream work from `develop`, and an
  `origin/feature/<change-name>` merge brings in already-pushed work on
  this same change from another device; neither is uncommitted
  implementation of new work, so neither conflicts with the reason this
  guardrail exists.
- **Never let a spawned subagent — or this orchestrator itself — print,
  echo, or otherwise materialize a generated secret's raw value (a private
  key, API token, password, signing key, or similar) anywhere in a
  response, report file, or implementation file.** This has actually
  happened: a Phase 2 subagent generated a signing keypair and printed the
  live private key value in its hand-back summary, landing it in the
  orchestrator's own transcript even though it was never committed to the
  repo. Phase 2's prompt now instructs the subagent to leave any task
  requiring secret disclosure unchecked with a note naming the manual
  command instead (see that phase's own guidance) — but the orchestrator
  must not rely on the subagent alone getting this right. If a subagent's
  hand-back response is ever found to contain what looks like a live secret
  value, treat it as compromised regardless of whether it reached a
  committed file, and surface this to the user explicitly — which value,
  why it's now considered burned, and that it needs regenerating
  (out-of-band, by the user, never by re-running the same generation step
  through this workflow) — rather than silently absorbing it or repeating
  it in any later response, file, or summary. This check applies to every
  phase, not just Phase 2 — any subagent could in principle encounter a
  task that generates a credential.
