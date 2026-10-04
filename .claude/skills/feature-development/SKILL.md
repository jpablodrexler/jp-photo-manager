---
name: feature-development
description: >
  Orchestrates the full feature lifecycle end-to-end: selects the next
  feature, proposes SDD artifacts if missing, implements all change tasks,
  runs code, security, and (when schema files changed) database reviews
  (findings fixed before continuing), runs backend and frontend tests until
  they pass, runs `e2e-testing`'s browser checks against a locally running
  backend when the change touches auth, the gallery, or a migration, builds
  updated Docker images and deploys them via Kubernetes (if a live cluster
  deployment exists) or Docker Compose (if Docker is running), syncs the web
  app's documentation (CLAUDE.md + docs/*.md) against what actually shipped,
  verifies the implementation actually satisfies the change's spec scenarios
  (not just that tasks are checked off) before archiving, then archives the
  SDD change and marks the feature as implemented. Use when you want a fully
  automated feature development cycle with minimal manual steps. TRIGGER
  when the user asks to develop a feature.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.9"
---

Orchestrate the full feature lifecycle from selection to archive using
dedicated subagents for each phase.

**Input**: Optional feature name or number, optionally followed by
`--skip-branch-setup` (e.g. `duplicate-detection --skip-branch-setup`). The
feature name/number, if provided, is passed to `features-next` to skip the
recommendation step and jump straight to confirmation for that feature.
`--skip-branch-setup` is for orchestrators — e.g. the
`features-batch-development` skill — that have already checked out a
shared branch and are running multiple features on it without committing
between them; see Phase 1 Step 1.5's batch-mode branch below. Omit it for
normal single-feature use — default behavior (create/resume
`feature/<change-name>` from `develop`) is unchanged.

---

## Overview

Seven phases: Phase 0 runs directly in the orchestrator's own context
(never a subagent — see that phase's own rationale below), Phases 1–6 each
run as a dedicated subagent (3a and 3b run in parallel):

| Phase                        | Subagent   | Skills / actions                                                                                                                                  |
| ----------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0 — Select & Confirm          | *(orchestrator's own context — never a subagent)* | `features-next` (recommend + mandatory `AskUserQuestion` confirmation) |
| 1 — Propose                   | Subagent 1 | `git fetch origin --prune` to detect a branch/commits pushed from another device → `gitflow` (start feature to create `feature/<change-name>` from `develop`, resume + merge in another device's pushed commits, or sync feature to catch up an existing one) → mark the feature `🔶 In Progress` in `JPPhotoManagerWeb/docs/backlog/features-planned.md` → `openspec-propose` with the feature's full brief file as its description (if artifacts missing) |
| 2 — Implement & Review        | Subagent 2 | `openspec-apply-change <name>` + `code-reviewer` + `database-reviewer` + `security-reviewer` (conditional, findings fixed before done)          |
| 3a — Backend tests            | Subagent 3 | runs `cd JPPhotoManagerWeb/backend && mvn test` until passing                                                                                     |
| 3b — Frontend tests           | Subagent 4 | runs `cd JPPhotoManagerWeb/frontend && npm test` until passing                                                                                    |
| 4 — E2E verification (conditional) | Subagent 5 | runs `e2e-testing`'s browser checks against a locally running backend when the change touches auth, the gallery, or a migration — skipped otherwise |
| 5 — Build & deploy            | Subagent 6 | deploys via `build-and-deploy-k8s.sh` if a live `photomanager` deployment exists, else Docker Compose/Dockerfiles (skipped if Docker not running) |
| 6 — Archive                   | Subagent 7 | `openspec-archive-change <name>` → `features-archive <name>`                                                                                      |

---

## Phase 0 — Select & Confirm (orchestrator's own context — never a subagent)

**This phase must never be delegated to a spawned subagent, under any
circumstances — including when an "Auto Mode" or similar
autonomous-operation instruction is active.** Selecting which feature gets
built next is always the user's decision, not a "reasonable default" the
model makes on their behalf to avoid stopping. A spawned subagent (via the
Agent tool) may run without reliable access to `AskUserQuestion` depending
on the environment, and even where it nominally has access, a
backgrounded/async agent's confirmation prompt may not reach the user in
time for the workflow to actually wait on it. Running this phase directly
in whatever context is currently executing this skill (the main
conversation, or `features-batch-development`'s inline loop) is the only
way to *guarantee* the confirmation happens.

1. Run `openspec --version` yourself. If it fails or is not found, tell the
   user `PROPOSE_BLOCKED — openspec CLI not found` and stop the whole
   workflow here — do not proceed to feature selection with a broken
   toolchain.
2. Use the Skill tool to invoke the `features-next` skill directly — not
   wrapped in an Agent call, not inside any subagent — passing the argument
   given to this skill (feature name/number), if any, or no argument
   otherwise. Let it present its recommendation and ask for confirmation
   via `AskUserQuestion` in this same context, exactly as `features-next`
   is designed to; do not pre-answer or skip that question yourself.
3. If `features-next` returns without a `CHANGE_NAME:` line (the user
   cancelled, or `features-next` itself couldn't obtain confirmation — see
   its own guardrails), stop the entire workflow here and tell the user.
   Never proceed to Phase 1 without a confirmed change name in hand.
4. Otherwise, capture the confirmed change name from the `CHANGE_NAME:`
   line. This is the value substituted for `<change-name>` in every phase
   below, including Subagent 1's prompt — Subagent 1 is given this name
   directly and never re-selects or re-confirms a feature on its own.

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
| 1 — Propose | Subagent 1 | selecting the branch/resume mode, generating SDD artifacts, the dependency check | `reference/phase-1-propose.md` |
| 2 — Implement & Review | Subagent 2 | implementing tasks and the code/database/security reviews | `reference/phase-2-implement-review.md` |
| 3 — Test | Subagents 3 & 4 (parallel) | running the backend and frontend tests | `reference/phase-3-test.md` |
| 4 — E2E Verification | Subagent 5 (conditional) | deciding whether the real-backend E2E check applies and running it | `reference/phase-4-e2e-verification.md` |
| 5 — Build & Deploy | Subagent 6 | the build, the Docker Compose / Kubernetes deploy and the smoke test | `reference/phase-5-build-and-deploy.md` |
| 6 — Archive | Subagent 7 | spec-compliance check, docs sync, archiving the change and marking the feature implemented | `reference/phase-6-archive.md` |

---
## Placeholder substitution (Phases 1–6)

Before spawning any subagent in Phases 1–6, replace every occurrence of
`<change-name>` in that subagent's prompt with the actual change name
confirmed in Phase 0.

---

## Final Summary

After all phases complete, display:

```
## Feature Development Complete

**Change:** <change-name>
**SDD Artifacts:** ✓ Created
**Implementation:** ✓ All tasks complete
**Code review:** ✓ All findings resolved
**Database review:** ✓ All findings resolved (or N/A — no schema changes)
**Security review:** ✓ All findings resolved (or N/A — no security-sensitive changes)
**Mobile viewport check:** ✓ Verified at Samsung S23 Ultra (384×824) — <what was checked/caught> (or N/A — no template/CSS changes)
**Backend tests:** ✓ All passing
**Frontend tests:** ✓ All passing
[if UNREVIEWED_PROD_FIXES was recorded in Phase 3, insert this line here:]
**⚠ Unreviewed production fixes:** <the PROD_CODE_FIXED lines> — fixed while
chasing test failures in Phase 3; the user chose to proceed without routing
them back through Phase 2's code review.
**E2E verification:** ✓ <one-line summary from Phase 4's `E2E: PASS`> (or
"N/A — no auth/gallery/migration changes in this change" if `E2E: SKIPPED`)
**Docker:** ✓ <value from DOCKER signal, e.g. "Deployed — build-and-deploy-k8s.sh (namespace photomanager)", "Deployed — backend, frontend", or "Skipped — Docker not running">
**Spec compliance:** ✓ All scenarios verified (any Unverified scenario `spec-compliance-check` found was closed with new test coverage during Phase 6, Step 1, before archiving — never left as a caveat)
**Docs sync:** ✓ <one-line summary from `web-docs-sync`, e.g. "Updated docs/backend.md REST API table + CLAUDE.md config pointer" or "Nothing to sync">
**SDD change:** ✓ Archived
**Feature:** ✓ Marked as implemented
```

---

## Guardrails

- Always capture and propagate the change name confirmed in Phase 0 to all
  later phases. Before spawning any subagent in Phases 1–6, substitute
  every `<change-name>` occurrence in its prompt with that confirmed value.
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
  branch is created, switched, or synced. In batch mode
  (`--skip-branch-setup`), Step 1.5 performs none of that — it only
  confirms the current branch isn't `main`/`develop` and otherwise leaves
  branch state entirely to the caller. Either way, Phases 2–6 must stay on
  whatever branch was current when Phase 1 finished; none of them may run
  their own branch operations.
- **Cancellation detection**: if `features-next` (invoked directly in
  Phase 0) returns without a `CHANGE_NAME:` line, treat it as user
  cancellation — stop the workflow immediately, before Subagent 1 is ever
  spawned, and inform the user. Subagent 1 itself is never given the
  opportunity to cancel feature selection — by the time it runs, the
  choice is final.
- **Do not start Phase 2 if Phase 1 returns
  `PROPOSE_NEEDS_DEPENDENCY_DECISION`**, even though it isn't a failure —
  this workflow must never barrel into implementing a feature that Step
  3.5 just discovered depends on something not yet built. Resolve it
  through the AskUserQuestion decision point described after Phase 1 first;
  only an explicit "proceed anyway" answer continues to Phase 2.
- **Step 1.6 marks the backlog row `🔶 In Progress` as soon as the branch to
  work on is ready, in both normal and batch mode.** This is what lets
  `features-next` recommend resuming an interrupted feature instead of
  starting a new one, and it runs before Step 2's artifact check so a
  change that still needs `openspec-propose` is marked in progress too. It
  only ever flips `⬜ Pending` → `🔶 In Progress`; it never touches a row
  already showing `🔶 In Progress` or `✅ Implemented`, and it never blocks
  the workflow if the row is missing (ad hoc changes outside the tracked
  backlog). Phase 6's `features-archive` is what eventually moves the row
  to the implemented table.
- **Auto Mode (or any other autonomous-operation instruction) never
  overrides a required user confirmation anywhere in this workflow.** This
  applies to every `AskUserQuestion` decision point this skill or a skill
  it invokes raises: Phase 0's feature selection (`features-next`), the
  `PROPOSE_NEEDS_DEPENDENCY_DECISION` proceed-or-stop choice after Phase 1,
  Phase 3's `PROD_CODE_FIXED` proceed-or-re-review choice, and any
  blocker-guidance prompt after a `*_BLOCKED` result. "Make the reasonable
  call instead of stopping to ask" is guidance for implementation judgment
  calls — it is never license to pick an unambiguous-looking top option
  and proceed past a point this skill designed to be a genuine human
  decision. If a confirmation step is ever skipped for this reason, that is
  a bug in how the skill was invoked, not acceptable behavior.
- **Never let a spawned subagent — or this orchestrator itself — print,
  echo, or otherwise materialize a generated secret's raw value (a private
  key, API token, password, signing key, or similar) anywhere in a
  response, report file, or implementation file.** Phase 2's prompt
  instructs the subagent to leave any task requiring secret disclosure
  unchecked with a note naming the manual command instead — but the
  orchestrator must not rely on the subagent alone getting this right. If a
  subagent's hand-back response is ever found to contain what looks like a
  live secret value, treat it as compromised regardless of whether it
  reached a committed file, and surface this to the user explicitly —
  which value, why it's now considered burned, and that it needs
  regenerating out-of-band by the user — rather than silently absorbing it
  or repeating it in any later response, file, or summary. This check
  applies to every phase, not just Phase 2.
- Do not start Phase 2 until Phase 1 confirms that all `applyRequires`
  artifacts are `done`. If Phase 1 returns `PROPOSE_BLOCKED`, surface the
  details to the user and stop.
- Do not start Phase 3 until Phase 2 returns `IMPLEMENT: DONE`. If Phase 2
  returns `IMPLEMENT_BLOCKED`, `REVIEW_BLOCKED`, `DATABASE_BLOCKED`, or `SECURITY_BLOCKED`, surface the details to the user and wait for guidance
  before continuing.
- Do not start Phase 4 until both test subagents in Phase 3 report `PASS`. If
  either reports `BLOCKED`, surface the details to the user and wait for
  guidance before continuing. If either reports `PROD_CODE_FIXED:` lines,
  surface them to the user and confirm whether to proceed or re-run Phase 2.
- Do not start Phase 5 until Phase 4 reports `E2E: PASS` or `E2E: SKIPPED`.
  If it reports `E2E_BLOCKED`, surface the details to the user and wait for
  guidance before continuing — do not assume it belongs back in Phase 2's
  code review without the user confirming that (a failure here can equally
  be an environment/infrastructure problem, not a code defect).
- Do not start Phase 6 until Phase 5 subagent reports `DOCKER: DEPLOYED` or
  `DOCKER: SKIPPED`. If it reports `DOCKER: BLOCKED`, surface the details to
  the user and wait for guidance before continuing.
- Do not display the Final Summary until Phase 6 subagent returns `ARCHIVE: DONE`.
  If it returns `ARCHIVE_BLOCKED` instead — Step 1's `spec-compliance-check`
  found a Failing scenario, a test added to close an Unverified scenario
  itself failed (revealing a real bug), or an Unverified scenario survived
  3 rounds of add-test-and-re-check — the change stays unarchived. Surface
  the scenario(s) to the user and wait for guidance instead of retrying
  Phase 6 automatically or treating it as equivalent to `DOCKER: BLOCKED`'s
  deploy-retry framing; a spec/behavior mismatch isn't something a re-run
  fixes by itself.
- **This workflow never archives a change with a known spec-compliance
  gap.** Phase 6 Step 1 closes every Unverified scenario with real,
  passing test coverage (or escalates via `ARCHIVE_BLOCKED` when it
  genuinely can't) before Step 2 ever runs. There is no
  `UNVERIFIED_SCENARIOS` caveat path — a scenario is either Verified
  before archiving, or the archive doesn't happen.
- Subagents 3 and 4 must be launched in the same message (parallel). Do not
  launch one before the other.
- **Phase 4 only starts the backend/frontend locally and reads/writes no
  repository files.** It never runs a Flyway migration or any other write
  against the database beyond what the application itself performs while
  running — Step 2 only confirms prerequisite infrastructure is reachable.
  Any schema fix needed to make Phase 4 pass belongs back in Phase 2's
  conditional `database-reviewer` step, not in this phase.
- **Foreground guardrail**: every Agent tool call in this skill (Subagents
  1–7) must pass `run_in_background: false`. Every phase in this workflow is
  gated on the prior phase's subagent actually finishing ("do not start
  Phase N until Subagent M returns ..."), but the Agent tool defaults to
  background execution, which returns immediately with no result. Spawning
  any of these subagents in the background risks the orchestrator
  advancing to the next phase — or worse, fabricating a phase result —
  before the subagent has actually completed, which the Agent tool's own
  guidance explicitly warns against. This applies even to Subagents 3 and 4:
  issuing both calls with `run_in_background: false` in the same message
  still runs them concurrently: it means this skill waits for both results
  before proceeding, rather than defaulting to background execution.
  **Never delegate an `AskUserQuestion` decision point to a spawned
  subagent as a workaround for background-execution uncertainty** — that is
  what Phase 0 (feature selection) and the `PROPOSE_NEEDS_DEPENDENCY_DECISION`
  handling after Phase 1 exist to avoid entirely, by running the
  confirmation directly in the orchestrator's own context (the main
  conversation, or `features-batch-development`'s inline loop) rather than
  trying to make a subagent's confirmation reliable.
- **Missing signal fallback**: if any subagent returns without its expected
  signal, treat it as `BLOCKED`, surface the subagent's raw response to the
  user, and wait for guidance before proceeding to the next phase.
- **Nested backgrounding guardrail**: this failure mode isn't unique to
  Phase 5 — any subagent in this workflow that backgrounds a long-running
  shell command (via the Bash tool's `run_in_background: true`) and then
  ends its response is making the same mistake, regardless of which phase
  it's in. Ending a turn after backgrounding work resolves that subagent's
  own `run_in_background: false` Agent-tool call back to whichever agent
  spawned it — with the response text as-is, not with the eventual result —
  because nothing automatically wakes an already-finished subagent back up
  when its background child completes; only an explicit `SendMessage` from
  a still-live parent can resume it, and nothing in this workflow does that
  automatically. Concretely, this hit Phase 5's Step 3K in practice: the
  subagent launched `build-and-deploy-k8s.sh` in the background and reported
  "I'll wait for it to complete," ending its turn — twice — before the
  script had actually finished, which the orchestrator only caught by
  directly verifying cluster/image state itself and manually resuming the
  subagent. Any subagent step whose real duration can exceed the Bash
  tool's 10-minute blocking cap (Phase 5's build script chief among them, at
  up to 20 minutes) must poll for completion with repeated **foreground**
  Bash calls within the same, uninterrupted turn — never end the response
  and rely on being notified later.
- **`code-reviewer` has two workflows; Phase 2 always uses Review, never
  Fix.** The skill's interactive Fix Workflow (§17) asks the user which
  category/finding to work on next and is meant to be steered turn-by-turn
  across a conversation — it does not fit an unattended subagent. Phase 2
  always invokes the plain Review workflow and fixes findings itself,
  updating the resulting report's checkboxes directly (see Step 2/3 above).
- **Phase 2's code review is always scoped, never a full-codebase sweep.**
  `code-reviewer` splits into one report per architecture layer (each run by
  its own subagent) only when asked to review the entire web application.
  Phase 2 reviews a single change's files, so it must stay on that single-
  report path — one dated report per invocation, reviewed inline by Subagent
  2 itself, no further subagents spawned.
- **`database-reviewer` follows the same conditional-trigger, scoped-Review,
  never-Fix pattern as `code-reviewer`.** It only runs when Step 3
  detects a Flyway migration, JPA entity, or repository query changed;
  Subagent 2 fixes findings itself (creating a new `V{n+1}__*.sql` migration
  for any fix to already-applied schema, never editing an existing one) and
  updates the resulting report's checkboxes directly (see Step 3 above).
- **`security-reviewer` has the same two-workflow and scoped-vs-sweep split
  as `code-reviewer`; Phase 2 always uses Review, scoped, never Fix or a
  full sweep.** Same rationale as the two bullets above — the interactive
  Fix Workflow doesn't fit an unattended subagent, and this step is
  reviewing one change's files, not the whole app. Subagent 2 fixes
  findings itself and updates the resulting report's checkboxes directly
  (see Step 4 above).
- **Work always happens on a `feature/<change-name>` branch cut from
  `develop`.** Phase 1's Step 1.5 is the only place a branch is created,
  switched, or synced — it invokes the `gitflow` skill (start feature) to
  create `feature/<change-name>` from `develop`, or, if it already exists
  from a prior run (locally, or only on the remote because another device
  pushed it first), resumes it (checking it out — from the local branch,
  or tracking `origin/feature/<change-name>` directly if only the remote
  copy exists — merging in any commits pushed from another device, and,
  via `gitflow`'s sync feature action, bringing it up to date with
  `develop`), before any file in the repository is created or modified,
  including the SDD artifacts written by `openspec-propose`. Phases 2–6
  must stay on that branch; none of them may run `git checkout`, `git
  switch`, `git merge`, invoke `gitflow`, or create another branch. If a
  subagent finds itself on a different branch, that is a bug in the
  workflow — surface it to the user rather than silently switching.
- **The feature branch is never synced from `main`, only `develop`.**
  Step 1.5's resume path uses `gitflow`'s sync feature action exclusively,
  which merges `origin/develop` and nothing else — matching `gitflow`'s own
  guardrail that `main` is never a valid source for a feature branch's
  content, even right after a release/hotfix has merged into both `main`
  and `develop` and the two look momentarily interchangeable. No phase in
  this workflow may merge, rebase, or pull `main` into the feature branch
  under any circumstance.
- **No git commits at any point.** Neither this skill nor any subagent it
  spawns may run `git commit`, `git push`, or any other git write command
  at any point in the workflow. This applies to all phases, including after
  tests pass and during archiving. If a subagent or invoked skill attempts
  to commit, block it and continue without committing. Branch creation and
  branch sync (`git fetch`, `git checkout`, `git checkout -b`, `git merge
  origin/develop`, and `git merge origin/feature/<change-name>` to pick up
  commits pushed from another device — all only in Phase 1's Step 1.5, the
  merges either directly or via `gitflow`'s start-feature/sync-feature
  actions) are the sole exceptions to this rule. A sync-feature merge
  commit brings in already-reviewed upstream work from `develop`, and an
  `origin/feature/<change-name>` merge brings in already-pushed work on
  this same change from another device; neither is uncommitted
  implementation of new work, so neither conflicts with the reason this
  guardrail exists.
- **No destructive Docker commands.** Do not run `docker compose down`,
  `docker rm`, `docker rmi`, or any command that stops or removes containers
  or images beyond what is strictly required to restart the application
  services being deployed.
- **No destructive kubectl commands.** Do not run `kubectl delete` or
  `kubectl scale --replicas=0` at any point. Phase 5's Kubernetes branch's
  only deploy action is running `build-and-deploy-k8s.sh` (plus the
  `kubectl rollout status` / `kubectl get pods` verification that follows
  it) — the script's own `kubectl apply -f`/`kubectl apply -k` calls are an
  accepted exception to "don't reconcile the whole stack by hand" precisely
  because they're script-owned, reviewed, and idempotent; do not additionally
  run `kubectl apply -k` or equivalent manually, outside the script, for any
  reason.
- **Never touch `k8s/secret.yaml` or `k8s/catalog-volumes.yaml`.** Per
  `JPPhotoManagerWeb/CLAUDE.md`, these two files hold real secrets and
  machine-specific paths and must never be read, created, or edited by any
  subagent — not even to "fix" a `build-and-deploy-k8s.sh` failure caused by
  one being missing. If the script reports either missing, surface its exact
  `ERROR:` line to the user and stop; the user must copy the `.example`
  template and fill it in themselves.
