---
name: feature-development
description: >
  Orchestrates the full feature lifecycle end-to-end: selects the next
  feature, proposes SDD artifacts if missing, implements all change tasks,
  runs code, security, and (when schema files changed) database reviews
  (findings fixed before continuing), runs backend and frontend tests until
  they pass, builds updated Docker images and deploys them via Kubernetes (if
  a live cluster deployment exists) or Docker Compose (if Docker is running),
  then archives the SDD change and marks the feature as implemented. Use
  when you want a fully automated feature development cycle with minimal
  manual steps. TRIGGER when the user asks to develop a feature.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.2"
---

Orchestrate the full feature lifecycle from selection to archive using
dedicated subagents for each phase.

**Input**: Optional feature name or number. If provided, passes it to
`features-next` to skip the recommendation step and jump straight to
confirmation for that feature.

---

## Overview

Five phases executed by six dedicated subagents (3a and 3b run in parallel):

| Phase                  | Subagent   | Skills / actions                                                                                                                                  |
| ---------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 — Select & Propose   | Subagent 1 | `features-next` (confirm only) → `gitflow` (start feature) to create `feature/<change-name>` branch from `develop` → `openspec-propose` (if artifacts missing) |
| 2 — Implement & Review | Subagent 2 | `openspec-apply-change <name>` + `code-reviewer` + `database-reviewer` + `security-reviewer` (conditional, findings fixed before done)          |
| 3a — Backend tests     | Subagent 3 | runs `cd JPPhotoManagerWeb/backend && mvn test` until passing                                                                                     |
| 3b — Frontend tests    | Subagent 4 | runs `cd JPPhotoManagerWeb/frontend && npm test` until passing                                                                                    |
| 4 — Build & deploy     | Subagent 5 | deploys via `build-and-deploy-k8s.sh` if a live `photomanager` deployment exists, else Docker Compose/Dockerfiles (skipped if Docker not running) |
| 5 — Archive            | Subagent 6 | `openspec-archive-change <name>` → `features-archive <name>`                                                                                      |

---

## Phase 1 — Select & Propose (Subagent 1)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (this phase's result gates every later phase, and
it embeds an interactive `AskUserQuestion` confirmation via `features-next`
that a backgrounded agent cannot reliably surface — see the foreground
guardrail below), with the following prompt (substitute `<input>` with the
argument passed to this skill, if any):

> Perform these steps in sequence. Do NOT skip any step.
>
> **Step 0 — Verify openspec CLI is available**
> Run: `openspec --version`
> If the command fails or is not found, end your response with
> `PROPOSE_BLOCKED — openspec CLI not found` and stop.
>
> **Step 1 — Select the feature**
> Use the Skill tool to invoke the `features-next` skill (pass the argument
> `<input>` if one was provided; otherwise invoke with no argument). The skill
> will present a recommendation, ask for user confirmation, and return a
> `CHANGE_NAME: <change-name>` line. Capture that value and proceed to Step 2.
> If the skill returns without a `CHANGE_NAME:` line, the user cancelled —
> end your response with `CANCELLED` and stop.
>
> **Step 1.5 — Create the feature branch**
> Before creating or modifying any file in the repository (including SDD
> artifacts in Step 3 below), make sure work happens on a dedicated branch
> cut from `develop`:
>
> 1. Run: `git status --porcelain`
>    If this prints anything (uncommitted changes present), end your
>    response with `PROPOSE_BLOCKED — uncommitted changes present, cannot
>    create feature branch` and stop.
> 2. Run: `git rev-parse --verify --quiet feature/<change-name>`
>    - If it prints a commit hash, the branch already exists (resuming a
>      prior run): run `git checkout feature/<change-name>` and continue on
>      that branch — do not invoke `gitflow` for this case, it only creates
>      new branches.
>    - If it fails (branch doesn't exist yet): use the Skill tool to invoke
>      `gitflow` with the action "start feature `<change-name>`". It checks
>      out `develop`, pulls the latest, and creates `feature/<change-name>`
>      from it. If it reports a blocker (e.g. `develop` doesn't fast-forward
>      cleanly), end your response with `PROPOSE_BLOCKED — <the gitflow
>      blocker>` and stop.
> 3. Run `git branch --show-current` and confirm the output is exactly
>    `feature/<change-name>` before proceeding. If it is not, end your
>    response with `PROPOSE_BLOCKED — could not switch to feature branch`
>    and stop.
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
> **Step 4 — Return the change name**
> End your response with exactly this line so the orchestrator can extract it:
> `CHANGE_NAME: <change-name>`

After the subagent completes:

- If the response contains `CANCELLED`: stop the entire workflow and inform the user.
- If the response contains `PROPOSE_BLOCKED`: surface the reason to the user and stop.
- Otherwise: extract the change name from the `CHANGE_NAME:` line. Store this
  value — it is passed as the argument to every skill invoked in Phases 2, 3, 4, and 5.

---

## Placeholder substitution (Phases 2–5)

Before spawning any subagent in Phases 2–5, replace every occurrence of
`<change-name>` in that subagent's prompt with the actual change name
extracted from Phase 1.

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
> writes a single dated `docs/code-review/CODE_REVIEW_FINDINGS_*.md` report
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
>   `docs/database-review/DATABASE_REVIEW_FINDINGS_*.md` report (repo root,
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
>   dated `docs/security-review/SECURITY_REVIEW_FINDINGS_*.md` report (repo
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

## Phase 4 — Build & Deploy (Subagent 5)

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
`run_in_background: false` (Phase 5 cannot start until this subagent
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
>    bash JPPhotoManagerWeb/scripts/build-and-deploy-k8s.sh
>    ```
>    Allow up to 20 minutes total before treating it as a failure — it builds
>    both images (up to 10 min each), may install the ingress-nginx
>    controller on a first run (up to ~5 min to become ready), and applies
>    the full Kubernetes stack.
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
>
> End your response with one of:
>
> - `DOCKER: DEPLOYED — build-and-deploy-k8s.sh (namespace photomanager)`
> - `DOCKER: BLOCKED — <brief reason>` if the script, or the rollout wait
>   after it, failed (or `kubectl rollout status` timed out) and you cannot
>   resolve it without human input.
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
> End your response with one of:
>
> - `DOCKER: DEPLOYED — <list of images built and containers restarted>`
> - `DOCKER: SKIPPED — <reason>`
> - `DOCKER: BLOCKED — <brief reason>` if a build or restart failed and you
>   cannot resolve it without human input.

Do not start Phase 5 until this subagent completes. A `DOCKER: SKIPPED` result
is not a failure — proceed to Phase 5 normally. Only `DOCKER: BLOCKED` requires
surfacing the issue to the user before continuing.

---

## Phase 5 — Archive (Subagent 6)

Spawn a **general-purpose subagent** via the Agent tool, with
`run_in_background: false` (the Final Summary cannot be displayed until this
subagent returns `ARCHIVE: DONE` — see the foreground guardrail below), with
the following prompt:

> Perform these two steps in sequence using the Skill tool:
>
> **Step 1** — Invoke `openspec-archive-change <change-name>`. Wait for it to
> complete fully (the SDD change directory must be moved to
> `openspec/changes/archive/`). This skill may prompt you about delta spec sync
> or incomplete tasks — respond to those prompts normally; they are part of the
> archiving workflow.
>
> **Step 2** — Invoke `features-archive <change-name>`. Wait for it to
> complete fully (the feature row must be updated to `✅ Implemented` in
> `openspec/features.md`).
>
> After both steps complete, end your response with exactly this line:
> `ARCHIVE: DONE`

Do not display the Final Summary until this subagent returns `ARCHIVE: DONE`.

---

## Final Summary

After all phases complete, display:

```
## Feature Development Complete

**Change:** <change-name>
**Artifacts:** ✓ Created
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
**Docker:** ✓ <value from DOCKER signal, e.g. "Deployed — build-and-deploy-k8s.sh (namespace photomanager)", "Deployed — backend, frontend", or "Skipped — Docker not running">
**SDD change:** ✓ Archived
**Feature:** ✓ Marked as implemented
```

---

## Guardrails

- Always capture and propagate the change name from Phase 1 to all later
  phases. Before spawning any subagent in Phases 2–5, substitute every
  `<change-name>` occurrence in its prompt with the actual value from Phase 1.
- **Cancellation detection**: if Subagent 1's response contains no `CHANGE_NAME:`
  line (or contains `CANCELLED`), treat it as user cancellation — stop the
  workflow immediately and inform the user.
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
- Do not start Phase 5 until Phase 4 subagent reports `DOCKER: DEPLOYED` or
  `DOCKER: SKIPPED`. If it reports `DOCKER: BLOCKED`, surface the details to
  the user and wait for guidance before continuing.
- Do not display the Final Summary until Phase 5 subagent returns `ARCHIVE: DONE`.
- Subagents 3 and 4 must be launched in the same message (parallel). Do not
  launch one before the other.
- **Foreground guardrail**: every Agent tool call in this skill (Subagents
  1–6) must pass `run_in_background: false`. Every phase in this workflow is
  gated on the prior phase's subagent actually finishing ("do not start
  Phase N until Subagent M returns ..."), but the Agent tool defaults to
  background execution, which returns immediately with no result. Spawning
  any of these subagents in the background risks the orchestrator
  advancing to the next phase — or worse, fabricating a phase result —
  before the subagent has actually completed, which the Agent tool's own
  guidance explicitly warns against. This applies even to Subagents 3 and 4:
  issuing both calls with `run_in_background: false` in the same message
  still runs them concurrently: it means this skill waits for both results
  before proceeding, rather than defaulting to background execution. Phase
  1's foreground requirement is doubly important because it embeds
  `features-next`'s interactive `AskUserQuestion` confirmation, which a
  backgrounded agent cannot reliably surface to the user.
- **Missing signal fallback**: if any subagent returns without its expected
  signal, treat it as `BLOCKED`, surface the subagent's raw response to the
  user, and wait for guidance before proceeding to the next phase.
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
  `develop`.** Phase 1's Step 1.5 is the only place a branch is created or
  switched — it invokes the `gitflow` skill (start feature) to create
  `feature/<change-name>` from `develop`, or resumes it directly if it
  already exists from a prior run, before any file in the repository is
  created or modified, including the SDD artifacts written by
  `openspec-propose`. Phases 2–5 must stay on that branch; none of them may
  run `git checkout`, `git switch`, invoke `gitflow`, or create another
  branch. If a subagent finds itself on a different branch, that is
  a bug in the workflow — surface it to the user rather than silently
  switching.
- **No git commits at any point.** Neither this skill nor any subagent it
  spawns may run `git commit`, `git push`, or any other git write command
  at any point in the workflow. This applies to all phases, including after
  tests pass and during archiving. If a subagent or invoked skill attempts
  to commit, block it and continue without committing. Branch creation
  (`git checkout`, `git checkout -b`) in Phase 1's Step 1.5 is the sole
  exception to this rule.
- **No destructive Docker commands.** Do not run `docker compose down`,
  `docker rm`, `docker rmi`, or any command that stops or removes containers
  or images beyond what is strictly required to restart the application
  services being deployed.
- **No destructive kubectl commands.** Do not run `kubectl delete` or
  `kubectl scale --replicas=0` at any point. Phase 4's Kubernetes branch's
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
