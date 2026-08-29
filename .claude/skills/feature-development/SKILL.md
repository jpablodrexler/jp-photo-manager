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
| 1 — Propose                   | Subagent 1 | `git fetch origin --prune` to detect a branch/commits pushed from another device → `gitflow` (start feature to create `feature/<change-name>` from `develop`, resume + merge in another device's pushed commits, or sync feature to catch up an existing one) → mark the feature `🔶 In Progress` in `JPPhotoManagerWeb/docs/backlog/features-planned.md` → `openspec-propose` (if artifacts missing) |
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

## Phase 1 — Propose (Subagent 1)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (this phase's result gates every later phase —
see the foreground guardrail below), with the following prompt (substitute
`<change-name>` with the value confirmed in Phase 0):

> Perform these steps in sequence. Do NOT skip any step.
>
> The change name has already been selected and confirmed with the user by
> the orchestrator, before you were spawned: `<change-name>`. Do not invoke
> `features-next` yourself, and do not attempt to select, change, or
> re-confirm a feature on your own — that decision is final and out of
> scope for you. Proceed directly to Step 1.5 using this change name.
>
> **Step 1.5 — Create the feature branch**
>
> **Batch mode (`--skip-branch-setup` was passed):** skip everything else in
> this step. Run `git branch --show-current` once and confirm it is not
> `main` or `develop` — if it is, end your response with `PROPOSE_BLOCKED —
> batch mode requires an existing feature branch already checked out, not
> main/develop` and stop. Otherwise proceed to Step 1.6 below (not Step 2
> directly — marking the feature In Progress applies in batch mode too)
> using whatever branch is currently checked out. Do not check for
> uncommitted changes here — an orchestrator running several features on
> one branch without committing between them will always have a dirty tree
> by the second feature onward, and that's expected, not a blocker. Do not
> create, switch, or sync any branch in this mode; the caller is
> responsible for branch state.
>
> **Normal mode (no flag passed):** before creating or modifying any file in
> the repository (including SDD artifacts in Step 3 below), make sure work
> happens on a dedicated branch cut from `develop`. Check resume status
> *before* checking for uncommitted changes — not the other way around: a
> resumed session (e.g. this skill being re-run after an interruption
> mid-Phase-2) is typically already checked out on `feature/<change-name>`
> with the in-progress implementation sitting there uncommitted, since this
> workflow never commits until a human reviews it. That's expected,
> resumable state, not a blocker — but if the uncommitted-changes check ran
> first, it would misfire on exactly that state and block the resume before
> the already-on-the-right-branch check ever got a chance to short-circuit
> it.
>
> 0. Run: `git fetch origin --prune`. This refreshes remote-tracking refs
>    (`origin/feature/*`) *before* any of the checks below reason about
>    branch existence — this repo is worked from more than one device, and
>    without a fresh fetch a branch (or new commits on a branch) pushed
>    from another device since this device's last fetch would be invisible
>    to steps 1 and 3 below, risking either a second, divergent
>    `feature/<change-name>` branch getting created, or a stale local copy
>    silently shadowing newer work that already exists on the remote.
>    `--prune` also drops remote-tracking refs for branches already deleted
>    on the remote, matching `gitflow`'s own cleanup-branches convention.
> 1. Run: `git branch --show-current`.
>    - If the output is already exactly `feature/<change-name>`: we're
>      already on the target branch — this is a resume. Uncommitted
>      changes here are expected. Before skipping ahead, check whether
>      another device has pushed commits to this same branch that this
>      local checkout doesn't have yet: run
>      `git rev-list HEAD..origin/feature/<change-name> --count` (if
>      `origin/feature/<change-name>` doesn't exist — i.e. this branch was
>      never pushed — treat the count as `0`, nothing to reconcile).
>      - If the count is `0`: nothing to reconcile — skip steps 2 and 3
>        below entirely and proceed straight to Step 1.6 of this prompt.
>      - If the count is greater than `0`: another device has pushed work
>        to this branch that isn't reflected in this local, uncommitted
>        checkout. Merging it now risks colliding with those uncommitted
>        local changes, which is not something to resolve silently. End
>        your response with `PROPOSE_BLOCKED — origin/feature/<change-name>
>        has <N> commit(s) not present locally (likely pushed from another
>        device); commit or stash local changes, then merge/rebase
>        manually before continuing` and stop.
>    - Otherwise, continue to step 2 below.
> 2. Run: `git status --porcelain`
>    If this prints anything (uncommitted changes present), end your
>    response with `PROPOSE_BLOCKED — uncommitted changes present, cannot
>    create feature branch` and stop. This check only applies here — we're
>    about to check out `develop` and either cut a new branch from it or
>    switch to an existing one, both of which need a clean tree; it does
>    not apply once step 1 has already confirmed we're on the target branch.
> 3. Run: `git rev-parse --verify --quiet feature/<change-name>`
>    - If it prints a commit hash, the branch already exists locally but
>      isn't currently checked out — this is a resume of a change that's
>      been sitting idle, possibly while other work (including a release,
>      or further commits pushed to this same branch from another device)
>      landed in the meantime: run `git checkout feature/<change-name>`.
>      Then, if `git rev-parse --verify --quiet origin/feature/<change-name>`
>      succeeds, run `git merge origin/feature/<change-name>` to pick up
>      any commits pushed from another device before this local branch was
>      checked out — this merges already-pushed history of this same
>      change, not new uncommitted implementation, so — like a sync-feature
>      merge from `develop` — it's exempt from this workflow's "no git
>      commits" guardrail (see the guardrails section). If this merge
>      doesn't complete cleanly, end your response with
>      `PROPOSE_BLOCKED — feature branch has diverged from
>      origin/feature/<change-name>, needs manual resolution` and stop.
>      Then use the Skill tool to invoke `gitflow` with the action "sync
>      feature `<change-name>`" to bring it up to date with `develop`
>      before continuing. `develop` is the *only* branch this may ever be
>      synced from — never `main`, even though right after a release merges
>      into both, they're momentarily identical and it's tempting to treat
>      either as equivalent; see `gitflow`'s guardrails for why that's
>      still wrong. If `gitflow` reports a merge conflict it can't resolve
>      automatically, end your response with `PROPOSE_BLOCKED — feature
>      branch sync conflict with develop, needs manual resolution` and
>      stop. Otherwise, if it reports any other blocker, end your response
>      with `PROPOSE_BLOCKED — <the gitflow blocker>` and stop.
>    - If it fails locally but
>      `git rev-parse --verify --quiet origin/feature/<change-name>`
>      succeeds: the branch has never been checked out on this device but
>      already exists on the remote — this is the case where another
>      device started (and pushed) this same feature branch first. Do
>      **not** invoke `gitflow`'s "start feature" action here — that always
>      branches fresh from `develop` and would silently orphan the work
>      already sitting on the remote branch. Instead, check it out tracking
>      the remote directly: `git checkout -b feature/<change-name>
>      origin/feature/<change-name>`. Then use the Skill tool to invoke
>      `gitflow` with the action "sync feature `<change-name>`" to bring it
>      up to date with `develop`, exactly as the "already exists locally"
>      case above does, applying the same conflict handling.
>    - If it fails both locally and on the remote (the branch doesn't exist
>      anywhere yet — the common case, no other device has started this
>      change): use the Skill tool to invoke `gitflow` with the action
>      "start feature `<change-name>`". It checks out `develop`, pulls the
>      latest, and creates `feature/<change-name>` from it. If it reports a
>      blocker (e.g. `develop` doesn't fast-forward cleanly), end your
>      response with `PROPOSE_BLOCKED — <the gitflow blocker>` and stop.
> 4. Run `git branch --show-current` and confirm the output is exactly
>    `feature/<change-name>` before proceeding. If it is not, end your
>    response with `PROPOSE_BLOCKED — could not switch to feature branch`
>    and stop.
>
> **Step 1.6 — Mark the feature In Progress**
> Both the batch-mode and normal-mode paths above reach this point once the
> branch to work on is ready. Before doing anything else, reflect that
> development has actually started: open
> `JPPhotoManagerWeb/docs/backlog/features-planned.md`, find the row in the
> `## Feature List` table whose `Change name` column (backtick-wrapped)
> matches `<change-name>`, and if its `Implementation` column shows
> `⬜ Pending`, change it to `🔶 In Progress` and save the file. If the row
> already shows `🔶 In Progress` (e.g. this is a resume of a previously
> interrupted run) or `✅ Implemented`, leave it unchanged — do not
> overwrite `✅ Implemented`. If no matching row exists (e.g. the change was
> proposed ad hoc, outside the tracked backlog), skip this silently — it is
> not an error. This runs before Step 2's artifact check so a change that
> still needs `openspec-propose` is marked in progress too, not only one
> that already has artifacts.
>
> **Step 2 — Check whether SDD artifacts exist**
> Run:
>
> ```
> openspec status --change "<change-name>" --json
> ```
>
> Parse the JSON. Find the `applyRequires` array and check whether every
> artifact ID in that array has `"status": "done"` in the `artifacts` list.
>
> - If **all** `applyRequires` artifacts are `done`: artifacts are ready —
>   skip Step 3.
> - If **any** are not `done`: artifacts are missing — proceed to Step 3.
>
> **Step 3 — Create missing artifacts (if needed)**
> Use the Skill tool to invoke `openspec-propose <change-name>`. Wait for it
> to complete. If it fails or reports an error, end your response with
> `PROPOSE_BLOCKED — <brief reason>` and stop.
> After it completes, re-run `openspec status --change "<change-name>" --json`
> and confirm every artifact ID in `applyRequires` now has `"status": "done"`.
> If any are still missing, end your response with
> `PROPOSE_BLOCKED — artifacts incomplete after propose` and stop.
>
> Now that artifacts are confirmed `done`, reflect that in the backlog: open
> `JPPhotoManagerWeb/docs/backlog/features-planned.md`, find the row in the
> `## Feature List` table whose `Change name` column (backtick-wrapped)
> matches `<change-name>`, and if its `SDD Artifacts` column shows `⬜ Pending`,
> change it to `✅ Created` and save the file. If the row already shows
> `✅ Created`, leave it unchanged. If no matching row exists (e.g. the
> change was proposed ad hoc, outside the tracked backlog), skip this
> silently — it is not an error.
>
> **Step 3.5 — Check for a newly discovered hard dependency**
> The artifacts just confirmed `done` (proposal.md, and design.md if
> present) may reveal, only now that the change has actually been designed,
> that `<change-name>` needs something another *not-yet-implemented* backlog
> feature provides (a table/column, an endpoint, a service method, a Kafka
> topic) — a dependency nobody could have known about back when this
> feature was recommended, because it only became apparent once the design
> was written. Left undetected, this surfaces later — mid-Phase-2
> implementation, or worse, after review — as a surprise blocker. Catch it
> here instead, before any implementation work starts.
>
> 1. Read proposal.md (and design.md if it exists) for `<change-name>`.
> 2. Read `JPPhotoManagerWeb/docs/backlog/features-planned.md`'s
>    `## Feature List` table and its `### Hard implementation dependencies`
>    subsection.
> 3. Determine whether the design depends on something owned by a
>    *different* feature row that is still `⬜ Pending` or `🔶 In Progress`
>    (not `<change-name>` itself, and not something already shipped —
>    shipped prerequisites are fine and not what this step is checking for).
> 4. **If no such dependency exists, or it's already recorded** in the
>    `### Hard implementation dependencies` subsection: nothing to do,
>    continue to Step 4.
> 5. **If a genuine, not-yet-recorded dependency is found**: add a new
>    `**Feature <this feature's #> → Feature <prerequisite's #>**
>    (prerequisite still pending)` block to the
>    `### Hard implementation dependencies` subsection, mirroring the exact
>    format of the existing entries there (one short sentence naming what's
>    needed and why), and save the file. Then end your response with
>    `PROPOSE_NEEDS_DEPENDENCY_DECISION — <change-name> (#<this feature's #>)
>    depends on <prerequisite-name> (#<prerequisite's #>), which is not yet
>    implemented; dependency recorded in
>    JPPhotoManagerWeb/docs/backlog/features-planned.md` and stop — do not
>    proceed to Step 4. This is not a `PROPOSE_BLOCKED` failure (nothing
>    went wrong — proposing surfaced real information); the orchestrator
>    handles it as a decision point, below.
>
> **Step 4 — Return the change name**
> End your response with exactly this line so the orchestrator can extract it:
> `CHANGE_NAME: <change-name>`

After the subagent completes:

- If the response contains `PROPOSE_BLOCKED`: surface the reason to the user and stop.
- If the response contains `PROPOSE_NEEDS_DEPENDENCY_DECISION`: this is a
  decision point, not a failure — handle it here in the orchestrator's own
  context (never delegate this confirmation to a subagent, for the same
  reason Phase 0's feature selection never is: reliable `AskUserQuestion`
  access is only guaranteed in the foreground). Surface the dependency
  found (which feature, which prerequisite, why) and use
  **AskUserQuestion** with:
  - **Question**: "`<change-name>` turned out to depend on
    `<prerequisite-name>` (#<prerequisite's #>), which isn't implemented
    yet. How do you want to proceed?"
  - **Options**:
    1. "Implement `<prerequisite-name>` first (recommended)" — stop this
       workflow run here (the dependency is already recorded in
       `features-planned.md`, so a future `features-next` run will
       correctly treat `<change-name>` as blocked and can recommend the
       prerequisite instead); tell the user to re-run `feature-development`
       once ready, either unnamed or naming the prerequisite directly.
    2. "Proceed with `<change-name>` anyway" — only if the user judges the
       dependency isn't actually a hard blocker for a usable first version;
       continue to Phase 2 and the rest of this workflow as normal. The
       dependency note stays recorded regardless — it was real, even if
       not blocking today.
    3. "Cancel" — stop the entire workflow here.
  - If the user picks option 1 or 3, stop the whole `feature-development`
    run now — do not proceed to Phase 2. Do not revert the dependency note
    just written to `features-planned.md`; it's accurate information
    regardless of what happens next.
- Otherwise: confirm the `CHANGE_NAME:` line matches the value Phase 0
  already confirmed with the user (it should always match, since Subagent 1
  was given that exact name and never re-selects one — this is a sanity
  check, not a new decision point). If it somehow doesn't match, treat that
  as a bug in the workflow: stop and surface both values to the user rather
  than silently trusting either one.

---

## Placeholder substitution (Phases 1–6)

Before spawning any subagent in Phases 1–6, replace every occurrence of
`<change-name>` in that subagent's prompt with the actual change name
confirmed in Phase 0.

---

## Phase 2 — Implement & Review (Subagent 2)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 3 cannot start until this subagent
returns `IMPLEMENT: DONE` — see the foreground guardrail below), with the
following prompt:

> Perform these steps in sequence:
>
> **Step 1 — Implement**
> Use the Skill tool to invoke `openspec-apply-change <change-name>`.
> Work through all pending tasks until implementation is complete.
> Do not stop until all tasks show `[x]` or you encounter a blocker that
> requires user guidance.
> When `openspec-apply-change` completes, do NOT invoke `code-reviewer` or
> `security-reviewer` proactively — both are handled explicitly in Steps 2
> and 3.
> If you encounter a blocker during implementation, end your response with
> `IMPLEMENT_BLOCKED — <brief reason>` and stop.
>
> **Never materialize a generated secret's raw value anywhere in this
> response, a report file, a tasks.md checkbox note, or any implementation
> file.** If a task requires generating a credential (a private key, API
> token, password, signing key, or similar), you may generate it and use it
> only to complete the narrowly-required local action — e.g. writing its
> *public* half to a committed file, when the scheme has one. Do not write,
> log, or echo the secret half anywhere this session's transcript, a
> report, or a repo file would capture it. Treat actually supplying that
> value to a secrets store (`k8s/secret.yaml`, a CI secret, an env var) as
> an out-of-band action only the user performs, in their own terminal,
> outside this workflow — never something you run with the value passed as
> a visible argument. Leave that task's checkbox unchecked with a note
> naming the required manual command (never the value). Do this even under
> this workflow's general "keep going until done or blocked" instruction —
> a task that can only be finished by exposing a secret's raw value is, by
> definition, blocked. This applies equally to any skill invoked from
> within this step (e.g. `decision-record` writing an ADR that describes
> the credential) — a description of *which* secret and *how* it's stored
> is fine; the value itself is never fine.
>
> **Determining "the files changed by `<change-name>`" — recompute fresh immediately before every single use, never cache one snapshot**
> Run `git status --porcelain` and parse every line's file path —
> including untracked (`??`) entries, not just modified/staged ones — into
> a concrete list; call it `CHANGED_FILES`. Do not use
> `git diff --name-only HEAD` for this, alone or as an alternative: it only
> reports changes to already-tracked files, so it silently misses any
> brand-new file — and a brand-new Flyway migration or a brand-new JPA
> entity (untracked until ever committed) is exactly the case Step 3 below
> most needs to catch. Never substitute a branch-to-branch diff like
> `git diff develop..HEAD` either: this branch was cut from `develop` and
> per this skill's "No git commits at any point" guardrail nothing gets
> committed during this workflow, so the branch's committed history stays
> identical to `develop` throughout — a branch-to-branch diff would always
> show zero files.
>
> **Do not compute this once and reuse it across Steps 2–4.** The working
> tree keeps changing throughout Phase 2 — Step 2's fix-and-re-review loop
> (up to 3 rounds), Step 3's, and Step 4's each modify or create files, and
> a fix can easily be exactly the kind of file the *next* step's trigger
> check needs to see (e.g. a Step 2 fix that adds a new entity or
> migration must be visible to Step 3's database-review trigger). A
> `CHANGED_FILES` snapshot taken once before Step 2 and reused afterward
> would silently miss all of that. Instead, re-run `git status --porcelain`
> and recompute `CHANGED_FILES` fresh immediately before **every** point
> below marked "(recompute `CHANGED_FILES`)" — every trigger check, every
> initial reviewer invocation, every re-check after a fix round, and every
> "final pass" after a conditional step's fixes — and pass that freshly
> computed list as an explicit argument each time. Never just describe
> scope in prose as "the files changed by `<change-name>`" and let the
> invoked skill re-derive it itself: a skill that re-derives scope might
> reach for its own branch-diff logic and reintroduce the exact zero-files
> failure mode this computation exists to avoid — one layer down, invisibly
> to this skill, which would otherwise believe the problem was already
> solved.
>
> **Step 2 — Code review**
> (recompute `CHANGED_FILES`) Invoke `code-reviewer` via the Skill tool
> using its **Review** workflow, passing the freshly recomputed
> `CHANGED_FILES` as the explicit scope — do not describe the scope in
> prose and let the skill re-derive it (this subagent runs
> unattended — do not invoke the skill's interactive Fix Workflow, which is
> designed for a human to steer turn-by-turn across a conversation). This is
> a scoped review of one change, not a full-codebase sweep of the whole web
> application — the code-reviewer skill's per-layer report split and
> subagent dispatch only apply to whole-app sweeps, so do not request or
> expect multiple reports or additional subagents here. Every invocation
> writes a single dated `docs/reports/code-review/CODE_REVIEW_FINDINGS_*.md` report
> (repo root, gitignored). After the review completes, examine the findings:
>
> - If the report contains **no 🔴 Critical or 🟡 Warning findings**: proceed.
> - If the report contains any 🔴 Critical or 🟡 Warning findings: fix every
>   one of them in the source files. As you fix each finding, check it off in
>   that report file (`- [ ]` → `- [x]`) with a `**Fixed:** <one-sentence
summary of what changed>` note appended to the same line — the same
>   convention the code-reviewer skill's Fix Workflow uses — so the report is
>   left as an accurate record of what this session resolved rather than a
>   stale "nothing done" snapshot. Then (recompute `CHANGED_FILES` — the
>   fixes just applied changed the working tree) re-invoke `code-reviewer`,
>   passing the freshly recomputed list, to confirm no new findings were
>   introduced; this writes a fresh report for the re-check (the original
>   report already reflects what was fixed). Repeat until the re-check
>   report is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `REVIEW_BLOCKED — repeated review cycles` and stop.
> - If you encounter a finding you cannot fix without human input: end your
>   response with `REVIEW_BLOCKED — <brief reason>` and stop.
>
> Do not proceed to Step 3 until all Critical and Warning findings are resolved.
>
> **Step 3 — Database review (conditional)**
> (recompute `CHANGED_FILES` — Step 2's fix rounds have modified the
> working tree since it was last computed) Check whether any path in the
> freshly recomputed `CHANGED_FILES` falls under any of these
> database-schema areas: a Flyway migration under `db/migration/`, a JPA
> entity under `infrastructure/persistence/entity/`, or a repository query
> (`@Query`, derived query method, or native query) under
> `infrastructure/persistence/jpa/` or
> `infrastructure/persistence/adapter/`.
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `database-reviewer` via the Skill tool
>   using its **Review** workflow, passing that same `CHANGED_FILES` as the
>   explicit scope — do not describe the scope in prose and let the skill
>   re-derive it (this subagent runs unattended — do not invoke the
>   skill's interactive Fix Workflow, which is designed for a human to steer
>   turn-by-turn across a conversation). This is a scoped review of one
>   change, not a full migration-history audit — do not request or expect
>   additional subagents here. Every invocation writes a single dated
>   `docs/reports/database-review/DATABASE_REVIEW_FINDINGS_*.md` report (repo root,
>   gitignored). After the review completes, fix every 🔴 Critical and 🟡
>   Warning finding in the source files — check each off in that report file
>   (`- [ ]` → `- [x]`) with a `**Fixed:**` note as you go, the same
>   convention `code-reviewer`'s Fix Workflow uses — then (recompute
>   `CHANGED_FILES`) re-invoke `database-reviewer`, passing the freshly
>   recomputed list, to confirm no new findings were introduced; this
>   writes a fresh report for the re-check. Repeat until the re-check report
>   is clean.
>   Remember that a fix to an already-applied Flyway migration is never an
>   edit to the existing file — it must be a new `V{n+1}__*.sql` migration
>   (per the skill's own §5); only entity-only findings (e.g. `@Table(indexes
= ...)` drift) may be fixed by editing the entity directly.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `DATABASE_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input (e.g. it
>   requires a data-backfill/locking strategy decision): end your response
>   with `DATABASE_BLOCKED — <brief reason>` and stop.
>   Once the database report is clean, (recompute `CHANGED_FILES` — the
>   database fixes just applied changed the working tree again) re-invoke
>   `code-reviewer` (Review workflow, same non-interactive and
>   scoped-not-full-sweep caveats as Step 2), passing the freshly
>   recomputed list as scope, for a final pass covering the database fixes
>   — apply any 🔴 Critical or 🟡 Warning findings, checking each off in
>   that pass's report file with a `**Fixed:**` note as in Step 2, before
>   proceeding.
>
> **Step 4 — Security review (conditional)**
> (recompute `CHANGED_FILES` — Step 3's fixes, if any, have modified the
> working tree since it was last computed) Check whether any path in the
> freshly recomputed `CHANGED_FILES` falls under any of these
> security-sensitive areas: authentication, authorization, file I/O, user
> input handling, dependency changes (`pom.xml` / `package.json`), or data
> persistence.
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `security-reviewer` via the Skill tool
>   using its **Review** workflow, passing that same `CHANGED_FILES` as the
>   explicit scope — do not describe the scope in prose and let the skill
>   re-derive it (this subagent runs unattended — do not invoke the
>   skill's interactive Fix Workflow, which is designed for a human to steer
>   turn-by-turn across a conversation). This is a scoped review of one
>   change, not a full-codebase sweep of the whole web application — the
>   security-reviewer skill's per-layer report split and subagent dispatch
>   only apply to whole-app sweeps, so do not request or expect multiple
>   reports or additional subagents here. Every invocation writes a single
>   dated `docs/reports/security-review/SECURITY_REVIEW_FINDINGS_*.md` report (repo
>   root, gitignored). After the review completes, fix every 🔴 Critical and
>   🟡 Warning finding in the source files — check each off in that report
>   file (`- [ ]` → `- [x]`) with a `**Fixed:**` note as you go, the same
>   convention `code-reviewer`'s Fix Workflow uses — then (recompute
>   `CHANGED_FILES`) re-invoke `security-reviewer`, passing the freshly
>   recomputed list, to confirm no new findings were introduced; this
>   writes a fresh report for the re-check. Repeat until the re-check report
>   is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `SECURITY_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input: end your
>   response with `SECURITY_BLOCKED — <brief reason>` and stop.
>   Once the security report is clean, (recompute `CHANGED_FILES` — the
>   security fixes just applied changed the working tree again) re-invoke
>   `code-reviewer` (Review workflow, same non-interactive and
>   scoped-not-full-sweep caveats as Step 2), passing the freshly
>   recomputed list as scope, for a final pass covering the security fixes
>   — apply any 🔴 Critical or 🟡 Warning findings, checking each off in that pass's report
>   file with a `**Fixed:**` note as in Step 2, before proceeding.
>
> Do not proceed to Step 5 until all Critical and Warning code-review,
> database-review, and security-review findings (including any follow-on
> code-review findings from the database and security sub-steps) are resolved.
>
> **Step 5 — Signal completion**
> End your response with exactly this line:
> `IMPLEMENT: DONE`

Do not start this phase until Phase 1 subagent has confirmed artifacts are
ready. Do not start Phase 3 until this subagent returns `IMPLEMENT: DONE`.

---

## Phase 3 — Test (Subagents 3 & 4 — launch in parallel)

Spawn **two general-purpose subagents in a single message** (both Agent tool
calls in the same response) so they run in parallel. Both calls must set
`run_in_background: false` — Phase 4 cannot start until both report `PASS`
or `BLOCKED` (see the foreground guardrail below), and `false` still runs
them concurrently when issued together in one message; it only means this
skill waits for both results before continuing rather than defaulting to
background execution.

### Subagent 3 — Backend tests

Prompt:

> Run backend unit tests for the JPPhotoManager web application.
> Change being validated: `<change-name>` (focus failure diagnosis here first).
> Only modify files under `JPPhotoManagerWeb/backend/` — do not touch frontend files.
>
> 1. Run (using the Bash tool): `cd JPPhotoManagerWeb/backend && mvn test`
> 2. If all tests pass, report success with the total test count.
> 3. If any tests fail:
>    a. Read the failure output carefully.
>    b. Identify the root cause (compilation error, assertion mismatch, missing
>    mock setup, etc.), checking files related to `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/backend/`. Prefer fixing
>    test code over production source code. If you must modify a production
>    source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/backend && mvn test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>    without human input.
>
> End your response with one of:
>
> - `BACKEND_TESTS: PASS (<n> tests)` if all tests pass.
> - `BACKEND_TESTS: BLOCKED — <brief reason>` if you encountered a failure
>   you cannot resolve.

### Subagent 4 — Frontend tests

Prompt:

> Run frontend component tests for the JPPhotoManager web application.
> Change being validated: `<change-name>` (focus failure diagnosis here first).
> Only modify files under `JPPhotoManagerWeb/frontend/` — do not touch backend files.
>
> 1. Run (using the Bash tool): `cd JPPhotoManagerWeb/frontend && npm test`
> 2. If all tests pass, report success with the total test count.
> 3. If any tests fail:
>    a. Read the failure output carefully.
>    b. Identify the root cause (type error, missing stub, assertion mismatch,
>    missing `provideNoopAnimations()`, etc.), checking files related to
>    `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/frontend/`. Prefer fixing
>    test code over production source code. If you must modify a production
>    source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/frontend && npm test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>    without human input.
>
> End your response with one of:
>
> - `FRONTEND_TESTS: PASS (<n> tests)` if all tests pass.
> - `FRONTEND_TESTS: BLOCKED — <brief reason>` if you encountered a failure
>   you cannot resolve.

Wait for **both** subagents to complete before proceeding to Phase 4.

If either subagent reports `BLOCKED`, stop the workflow and surface the failure
details to the user before continuing. Do not proceed to Phase 4 until both
subagents report `PASS`.

If either subagent's response contains one or more `PROD_CODE_FIXED:` lines,
surface them to the user with a note that those production source changes were
not covered by Phase 2's review. Ask the user whether to:

- **Proceed** — continue to Phase 4 without additional review. If chosen,
  record this (e.g. `UNREVIEWED_PROD_FIXES: <the PROD_CODE_FIXED lines>`) —
  the Final Summary below must surface it, since those files shipped without
  going through code-review.
- **Re-run Phase 2** — spawn a new Subagent 2 with the same prompt to review
  and code-review the production fixes, then re-run Phase 3 (spawn new
  Subagents 3 and 4) to confirm all tests still pass before continuing to
  Phase 4. If this path is taken, there is nothing left to caveat in the
  Final Summary — the fixes did go through review.

---

## Phase 4 — E2E Verification (Subagent 5, conditional)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 5 cannot start until this subagent
reports its signal — see the foreground guardrail below), with the
following prompt:

> Perform these steps in sequence.
>
> **Step 1 — Determine whether E2E verification applies to this change**
> Run `git status --porcelain` and parse every line's file path — including
> untracked (`??`) entries — into a concrete list; call it `CHANGED_FILES`.
> This is the same recompute-fresh convention Phase 2 uses — never assume a
> list from an earlier phase is still accurate.
>
> Check whether any path in `CHANGED_FILES` falls under one of these
> UI-facing/data areas: authentication (`core/guards/auth.guard.ts`,
> `core/interceptors/auth.interceptor.ts`, the `features/auth/login`
> component), the gallery (`features/gallery/`, the app's primary
> photo-browsing flow), or a Flyway migration under `db/migration/` (a
> schema change can silently change what any of the above renders, or what
> the backend serves, even with no frontend file touched).
>
> - If **none** of these areas were touched: end your response with exactly
>   `E2E: SKIPPED — no auth/gallery/migration changes in this change` and
>   stop. Do not start the backend/frontend or run Cypress for a change
>   that doesn't touch any of these areas.
> - If **any** were touched: continue to Step 2.
>
> **Step 2 — Confirm prerequisites are ready**
> Follow `e2e-testing` §1 (PostgreSQL, MongoDB, Redis, and — only if this
> change touches sync/convert/duplicate-detection or another
> Kafka-consuming flow — Kafka; §1.5's check that real data exists). If any
> required prerequisite isn't reachable, end your response with
> `E2E_BLOCKED — <brief reason>` and stop — do not attempt to fix an
> environment/infrastructure problem yourself.
>
> **Step 3 — Start the backend and frontend**
> Follow `e2e-testing` §2 (start the backend) and §3 (start the frontend).
> If either never comes up, end your response with `E2E_BLOCKED — backend/
> frontend did not start` and stop.
>
> **Step 4 — Run the browser checks**
> 1. Authenticate per `e2e-testing` §4 (the fixed `admin`/`admin`
>    credential — see §4.1 for the BCrypt-reset fallback if login returns
>    401). Confirm the session reaches `/home` and the dashboard renders.
> 2. Follow `e2e-testing` §7 (visual verification via a throwaway Cypress
>    spec) for whichever surface this change touched. If this change
>    touched the gallery specifically, also exercise its relevant flow
>    (thumbnail grid load, viewer mode, or whichever interaction changed)
>    once through the actual UI — not just confirming the page loads — to
>    confirm the new/changed behavior works against the real backend.
> 3. Follow `e2e-testing` §8 (interactive navigation) to confirm routing
>    between the touched area and at least one neighboring route works.
> 4. Take a screenshot at the final state and read it back to confirm
>    there's no visible layout break.
>
> **Step 5 — Tear down**
> Follow `e2e-testing` §10 (stop the backend/frontend, and any infra this
> run started that isn't part of the developer's normal always-on setup).
>
> End your response with one of:
>
> - `E2E: PASS — <one-line summary of what was exercised>` if every check in
>   Step 4 passed.
> - `E2E_BLOCKED — <brief reason>` if any check failed, a prerequisite
>   wasn't ready, or a console error/visible layout break was observed.

Do not start Phase 5 until this subagent reports `E2E: PASS` or
`E2E: SKIPPED`. If it reports `E2E_BLOCKED`, surface the details to the
user and wait for guidance before continuing — a failure here is against
locally running infrastructure, so it may equally be an environment/data
problem as a code defect (see `e2e-testing`'s own framing); don't assume
it belongs back in Phase 2's code review without the user confirming that.

---

## Phase 5 — Build & Deploy (Subagent 6)

This project can be deployed via Kubernetes (`JPPhotoManagerWeb/k8s/` +
`kustomization.yaml`, driven by `JPPhotoManagerWeb/scripts/build-and-deploy-k8s.sh`)
or Docker Compose (`JPPhotoManagerWeb/docker-compose.yml`) — both build the
same `photomanager-backend`/`photomanager-frontend` images. Kubernetes takes
priority when both are present, because running docker-compose's `frontend`
service (`ports: "80:80"`) alongside an active Kubernetes ingress controller
fails outright on a host port 80 conflict, and because a stray
`docker compose up` would silently deploy to a disconnected instance while
the real (Kubernetes) environment everyone tests against stays on the old
image.

The Kubernetes branch always redeploys through `build-and-deploy-k8s.sh`
rather than replicating its steps by hand — the script is the single source
of truth for the build-and-deploy sequence (documented in the top-level
README's "Running with Kubernetes" section) and is idempotent, safe to
re-run. Do not inline `docker build` / `kubectl apply` steps here that
duplicate what the script already does.

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (Phase 6 cannot start until this subagent
completes — see the foreground guardrail below), with the following
prompt:

> **Step 1 — Check whether Docker is running**
> Run: `docker info`
>
> - If the command fails or returns an error (daemon not running): end your
>   response with `DOCKER: SKIPPED — Docker not running` and stop.
> - If Docker is running: proceed to Step 2.
>
> **Step 2 — Determine the deploy target: Kubernetes or Docker Compose**
> First check whether `kubectl` even has a context to work with: run
> `kubectl config current-context`.
>
> - If this fails (`kubectl` not installed, or installed with no current
>   context configured): there is no live Kubernetes deployment intended
>   for this machine. Continue with **Step 3** below (Docker Compose).
> - If this succeeds (a context is configured): Kubernetes is the intended
>   deploy target on this machine, so a failure from here on is a blocker,
>   not a signal to fall back — the surrounding rationale above is explicit
>   that a stray Compose deploy alongside a live K8s environment is
>   dangerous (host port 80 conflict, or silently deploying to a
>   disconnected instance while the real environment everyone tests
>   against stays on the old image). Run:
>   `kubectl get deployment backend frontend -n photomanager --no-headers`
>   - If this succeeds and lists both `backend` and `frontend`: a live
>     Kubernetes deployment exists. Follow **Step 3K** below, then stop —
>     do not perform Steps 3–7.
>   - If this fails because the `photomanager` namespace or its
>     `backend`/`frontend` deployments simply don't exist yet (a first-time
>     setup, not an error talking to the cluster): treat this the same as
>     "no live deployment" and continue with **Step 3** below.
>   - If it fails any other way (cluster unreachable, auth/RBAC error,
>     timeout, or any error message that isn't clearly "these deployments
>     don't exist"): a context is configured but the query itself is
>     broken. Do **not** silently fall back to Step 3 — end your response
>     with `DOCKER: BLOCKED — kubectl context '<context>' is configured
>     but querying deployments failed: <error>` and stop; this needs a
>     human to confirm whether Kubernetes is actually the intended target
>     before Compose touches anything.
>
> **Step 3K — Kubernetes build & deploy via script**
>
> 1. Run the deploy script from the repo root (it `cd`s to `JPPhotoManagerWeb/`
>    internally, so this works regardless of current working directory):
>    ```
>    bash JPPhotoManagerWeb/scripts/build-and-deploy-k8s.sh > /tmp/build-deploy.log 2>&1 &
>    echo $!
>    ```
>    Allow up to 20 minutes total before treating it as a failure — it builds
>    both images (up to 10 min each), may install the ingress-nginx
>    controller on a first run (up to ~5 min to become ready), and applies
>    the full Kubernetes stack. That 20-minute ceiling is longer than the
>    Bash tool's own 10-minute blocking cap, which is why the script is
>    launched with a trailing `&` as shown above instead of run directly.
>
>    **Do not use the Bash tool's `run_in_background` parameter for this, and
>    do not end your response after launching it.** `run_in_background: true`
>    defers the result to a later notification — but that notification wakes
>    whichever agent is still live and listening, and once you end your
>    response, you are not it: the orchestrator's `run_in_background: false`
>    Agent call for you resolves immediately with whatever you just said,
>    treating it as your final answer even though the script is still
>    running. Nothing automatically re-invokes you later to pick this back
>    up — an unattended subagent that stops here simply stops, mid-deploy,
>    until a human notices and manually resumes it.
>
>    Instead, stay in this same turn and poll for completion with repeated
>    **blocking** (foreground) Bash calls against the PID you captured above:
>    ```
>    while kill -0 <PID> 2>/dev/null; do sleep 30; done
>    wait <PID>; echo "EXIT_CODE=$?"
>    ```
>    Issue this as one or more sequential foreground Bash calls — if a single
>    call's own timeout is reached while the process is still running,
>    re-issue the same polling loop again — until you have the script's
>    actual exit code in hand. Only then move on to step 2.
> 2. If the script exits non-zero: read its output — it prints a specific
>    `ERROR:` line for each failure mode (missing `k8s/secret.yaml` or
>    `k8s/catalog-volumes.yaml`, `kubectl` not connected, ingress-nginx pod
>    not scheduled/ready in time). **Do not create, edit, or read
>    `k8s/secret.yaml` or `k8s/catalog-volumes.yaml` yourself** — per
>    `JPPhotoManagerWeb/CLAUDE.md` they hold real secrets and machine-specific
>    paths and must never be read under any circumstances. If either is
>    missing, end your response with `DOCKER: BLOCKED — <the script's ERROR
line>` verbatim so the user can create it from the matching
>    `.example` template themselves. For any other script failure, end with
>    `DOCKER: BLOCKED — <brief reason>`.
> 3. The script triggers `kubectl rollout restart` near the end but returns
>    as soon as it prints pod status — it does not block until the rollout
>    finishes. After the script exits 0, explicitly wait for the rollout to
>    settle. Give the backend up to 12 minutes — its Spring Boot startup has
>    been observed taking several minutes on CPU-constrained clusters (see
>    the `startupProbe` comment in `k8s/backend.yaml`); a slow-but-successful
>    rollout is expected, not a failure:
>    ```
>    kubectl rollout status deployment/backend -n photomanager --timeout=12m
>    kubectl rollout status deployment/frontend -n photomanager --timeout=2m
>    ```
> 4. Verify:
>    ```
>    kubectl get pods -n photomanager -l 'app in (backend,frontend)'
>    ```
>    Both should show `1/1` and `Running`.
> 5. **Post-deploy smoke test.** `Running` pods and a passing readiness probe
>    only prove `/actuator/health` responds inside the backend pod — they
>    don't prove a real request routes through Spring Security and a
>    controller correctly, or that the frontend's nginx `/api` proxy_pass to
>    the backend Service actually resolves. Verify with a transient
>    port-forward, torn down immediately after (never left running):
>    ```
>    kubectl port-forward -n photomanager svc/backend 18080:8080 &
>    PF_PID=$!
>    sleep 2
>    curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:18080/api/home/stats
>    kill $PF_PID
>    ```
>    Expect `401` or `403` (unauthenticated) — this proves the Spring
>    context, security filter chain, and a real controller round-trip all
>    work, not just the actuator health indicator. A connection error,
>    timeout, or `500` here is a real regression the rollout/pod checks
>    above cannot see; treat it as a smoke-test failure.
>
>    Also try the ingress path, but treat it as informational only — a
>    missing local DNS entry for `photomanager.local` is an environment gap,
>    not a deploy failure:
>    ```
>    curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://photomanager.local/
>    ```
>    Note the result either way in the final summary, but only the
>    port-forward check above can fail the smoke test.
>
> End your response with one of:
>
> - `DOCKER: DEPLOYED — build-and-deploy-k8s.sh (namespace photomanager)`
> - `DOCKER: BLOCKED — <brief reason>` if the script, the rollout wait after
>   it, or Step 5's port-forward smoke test failed (or `kubectl rollout
>   status` timed out) and you cannot resolve it without human input.
>
> **Step 3 — Probe Docker Compose version**
> Determine which compose command is available:
>
> - Run `docker compose version`. If it succeeds, use `docker compose` for all
>   subsequent compose commands.
> - Otherwise run `docker-compose version`. If it succeeds, use `docker-compose`
>   for all subsequent compose commands.
> - If neither succeeds, note that only individual `docker build` commands will
>   be used.
>
> **Step 4 — Discover the Docker setup**
> Look for the project's Docker configuration in this order:
>
> 1. A `docker-compose.yml` or `compose.yml` at the repository root or under
>    `JPPhotoManagerWeb/`.
> 2. Individual `Dockerfile` files under `JPPhotoManagerWeb/backend/` and
>    `JPPhotoManagerWeb/frontend/`.
>
> If neither is found: end your response with
> `DOCKER: SKIPPED — no Dockerfile or compose file found`.
>
> **Step 5 — Identify application services**
> If a compose file exists, read it and classify each service:
>
> - **Application service**: has a `build:` key pointing to a local directory
>   or Dockerfile — regardless of whether `image:` is also present (the
>   `image:` key in that case just names the resulting tag). These should be
>   rebuilt and redeployed.
> - **Infrastructure service**: has an `image:` key referencing an external
>   registry (e.g. `postgres:15`, `apache/kafka:3.9.0`) and no `build:` key.
>   These must NOT be rebuilt or restarted.
>
> Build the list of application service names to pass to the compose command.
>
> **Step 6 — Build and deploy**
> Allow up to 10 minutes per image build before treating it as a failure.
>
> - **If a compose file exists**: run
>   `<compose-cmd> up --build -d <app-services>`
>   where `<compose-cmd>` is `docker compose` or `docker-compose` (from Step 3)
>   and `<app-services>` is the space-separated list identified in Step 5.
>   Example: `docker compose up --build -d backend frontend`
> - **If only individual Dockerfiles exist**: build and restart each image:
>   1. Build the images:
>
>      ```
>      docker build -t photomanager-backend:latest JPPhotoManagerWeb/backend
>      docker build -t photomanager-frontend:latest JPPhotoManagerWeb/frontend
>      ```
>
>   2. Find the running containers that use those images:
>
>      ```
>      docker ps --filter ancestor=photomanager-backend:latest
>      docker ps --filter ancestor=photomanager-frontend:latest
>      ```
>
>   3. Restart each container found:
>
>      ```
>      docker restart <container-name-or-id>
>      ```
>
> **Step 7 — Verify**
> Run `docker ps` and confirm the updated containers are listed as running.
>
> **Step 8 — Post-deploy smoke test**
> Containers showing `Up` doesn't prove the app actually serves a correct
> response — verify with the same authenticated-endpoint check `e2e-testing`
> §5 uses:
> ```
> curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:8080/api/home/stats
> curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:80/
> ```
> Expect `401`/`403` from the first (an unauthenticated API call reaching a
> real controller, not just a raw TCP accept) and `200` from the second
> (frontend serving). A connection error, timeout, or `500`/`502` from either
> is a real regression Step 7's `docker ps` check cannot see; treat it as a
> smoke-test failure. Adjust the ports if the compose file maps them
> differently than the defaults above.
>
> End your response with one of:
>
> - `DOCKER: DEPLOYED — <list of images built and containers restarted>`
> - `DOCKER: SKIPPED — <reason>`
> - `DOCKER: BLOCKED — <brief reason>` if a build, restart, or Step 8's smoke
>   test failed and you cannot resolve it without human input.

Do not start Phase 6 until this subagent completes. A `DOCKER: SKIPPED` result
is not a failure — proceed to Phase 6 normally. Only `DOCKER: BLOCKED` requires
surfacing the issue to the user before continuing.

---

## Phase 6 — Archive (Subagent 7)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (the Final Summary cannot be displayed until this
subagent returns `ARCHIVE: DONE` — see the foreground guardrail below), with
the following prompt:

> Perform these steps in sequence using the Skill tool:
>
> **Step 1 — Verify spec compliance and close every coverage gap.** Invoke
> `spec-compliance-check` for `<change-name>`. It reads
> `openspec/changes/<change-name>/specs/**/spec.md` and `tasks.md`
> (unmodified — this skill is read-only against `openspec/`) and reports
> each acceptance scenario as Verified, Failing, or Unverified; it does not
> invoke `openspec-archive-change` itself and does not edit any files.
>
> - **If the report has any Failing scenario:** stop immediately — do not
>   proceed to Step 2. End your response with
>   `ARCHIVE_BLOCKED: spec-compliance-check found <N> failing scenario(s) — <one-line summary>`.
>   A Failing scenario is a real behavioral gap, not a coverage gap — it
>   belongs back in Phase 2's implementation/review loop, not fixed here.
> - **If the report has any Unverified scenario (no Failing ones): close
>   every one of them before proceeding — never carry an Unverified
>   scenario forward as a caveat.** An Unverified scenario means the
>   behavior may well be correct, but nothing actually proves it — that gap
>   gets closed now, not documented and left open. For each Unverified
>   scenario:
>   1. Read the report's explanation of exactly what's missing (e.g. "no
>      test exercises this compound claim," "no test covers the X branch,"
>      "no mounted UI test asserts Y is absent").
>   2. Add the missing test coverage directly — a new backend
>      (`@SpringBootTest` / `@WebMvcTest` / `@DataJpaTest`) test or a
>      Cypress component test/assertion extending the relevant existing
>      spec file (preferred over a new file, matching this project's
>      conventions — see `java-unit-test-developer` /
>      `cypress-unit-test-developer`), or an E2E case if that's the layer
>      the gap sits at.
>   3. Re-invoke `spec-compliance-check` for `<change-name>` to confirm the
>      scenario now reports Verified.
>   4. Repeat for every Unverified scenario from the original report.
>   - **If closing a gap's test fails** (the scenario turns out to be
>     genuinely broken, not just untested): treat this exactly like a
>     Failing scenario — stop and end your response with
>     `ARCHIVE_BLOCKED: spec-compliance-check found <N> failing scenario(s) — <one-line summary>`.
>     A coverage gap that turns out to hide a real bug is not something to
>     paper over by weakening or dropping the new test.
>   - **If after 3 rounds of add-test-and-re-check a scenario still isn't
>     Verified** and it's not a genuine behavioral failure either (e.g. you
>     cannot find any way to exercise it with this project's test tooling):
>     end your response with `ARCHIVE_BLOCKED — repeated spec-compliance
>     cycles, <N> scenario(s) still unverified — <one-line summary>` rather
>     than silently archiving with a gap. This needs a human judgment call
>     on the testing approach, not indefinite auto-retry.
>   - Once every scenario reports Verified, run the affected test suite(s)
>     once more (`cd JPPhotoManagerWeb/backend && mvn test` and/or `cd
>     JPPhotoManagerWeb/frontend && npm test`) to confirm the newly added
>     tests pass alongside everything else, then continue to Step 2.
> - **If everything is already Verified:** continue to Step 2 with nothing
>   to close.
>
> **Step 2** — Invoke `web-docs-sync` in its scoped-sync mode for the
> current branch's changes (it diffs against `origin/develop` itself — no
> need to pass a file list). This keeps `JPPhotoManagerWeb/CLAUDE.md` and
> `JPPhotoManagerWeb/docs/*.md` from drifting behind whatever this feature
> just shipped (a new endpoint, config property, cache, Kafka topic, route,
> deploy-manifest env var, or custom metric). This step is **best-effort,
> not blocking**: if the skill reports nothing to sync, or the diff is
> outside its scope (e.g. a WPF-only or tooling-only change), continue to
> Step 3 regardless. Only stop and surface details to the user if the skill
> itself errors out in a way that leaves files in a broken state.
>
> **Step 3** — Invoke `openspec-archive-change <change-name>`. Wait for it to
> complete fully (the SDD change directory must be moved to
> `openspec/changes/archive/`). This skill may prompt you about delta spec sync
> or incomplete tasks — respond to those prompts normally; they are part of the
> archiving workflow.
>
> **Step 4** — Invoke `features-archive <change-name>`. Wait for it to
> complete fully (the feature row must be updated to `✅ Implemented` in
> `JPPhotoManagerWeb/docs/backlog/features-planned.md`).
>
> After all four steps complete, end your response with exactly this line:
> `ARCHIVE: DONE`

Do not display the Final Summary until this subagent returns `ARCHIVE: DONE`
or `ARCHIVE_BLOCKED`. An `ARCHIVE_BLOCKED` result means the feature is
implemented and tested but **not archived** — Step 1's `spec-compliance-check`
found a Failing scenario, a test added to close an Unverified scenario
itself failed (revealing a real bug), or an Unverified scenario survived 3
rounds of add-test-and-re-check. Surface the scenario(s) to the user and
wait for guidance (fix the code, fix the spec via the normal `openspec-*`
workflow, or override and archive manually) rather than proceeding or
retrying automatically. This workflow never archives a change with a known
spec-compliance gap: Step 1 closes every Unverified scenario with real test
coverage (or escalates via `ARCHIVE_BLOCKED` if it genuinely can't) before
Step 2 ever runs — there is no "archive now, note the gap for later" path.

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
  backlog). Phase 6's `features-archive` is what eventually flips the row
  to `✅ Implemented`.
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
