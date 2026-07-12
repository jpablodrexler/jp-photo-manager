---
name: improvement-development
description: >
  Orchestrates the full improvement lifecycle end-to-end: selects the next
  improvement, proposes SDD artifacts if missing, implements all change tasks,
  runs code and security reviews (findings fixed before continuing), runs
  backend and frontend tests until they pass, builds updated Docker images and
  deploys them via Kubernetes (if a live cluster deployment exists) or Docker
  Compose (if Docker is running), then archives the SDD change and marks the
  improvement as implemented. Use when you want a fully automated improvement
  development cycle with minimal manual steps.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Orchestrate the full improvement lifecycle from selection to archive using
dedicated subagents for each phase.

**Input**: Optional improvement name or number. If provided, passes it to
`improvements-next` to skip the recommendation step and jump straight to
confirmation for that improvement.

---

## Overview

Five phases executed by six dedicated subagents (3a and 3b run in parallel):

| Phase                   | Subagent   | Skills / actions                                                                                                 |
| ----------------------- | ---------- | ---------------------------------------------------------------------------------------------------------------- |
| 1 — Select & Propose    | Subagent 1 | `improvements-next` (confirm only) → `openspec-propose` (if artifacts missing)                                  |
| 2 — Implement & Review  | Subagent 2 | `openspec-apply-change <name>` + `code-reviewer` + `security-reviewer` (conditional, findings fixed before done) |
| 3a — Backend tests      | Subagent 3 | runs `cd JPPhotoManagerWeb/backend && mvn test` until passing                                                    |
| 3b — Frontend tests     | Subagent 4 | runs `cd JPPhotoManagerWeb/frontend && npm test` until passing                                                   |
| 4 — Build & deploy      | Subagent 5 | builds app images and deploys via Kubernetes (`kubectl rollout restart`) if a live `photomanager` deployment exists, else Docker Compose/Dockerfiles (skipped if Docker not running) |
| 5 — Archive             | Subagent 6 | `openspec-archive-change <name>` → `improvements-archive <name>`                                                 |

---

## Phase 1 — Select & Propose (Subagent 1)

Spawn a **general-purpose subagent** via the Agent tool with the following
prompt (substitute `<input>` with the argument passed to this skill, if any):

> Perform these steps in sequence. Do NOT skip any step.
>
> **Step 0 — Verify openspec CLI is available**
> Run: `openspec --version`
> If the command fails or is not found, end your response with
> `PROPOSE_BLOCKED — openspec CLI not found` and stop.
>
> **Step 1 — Select the improvement**
> Use the Skill tool to invoke the `improvements-next` skill (pass the argument
> `<input>` if one was provided; otherwise invoke with no argument). The skill
> will present a recommendation, ask for user confirmation, and return a
> `CHANGE_NAME: <change-name>` line. Capture that value and proceed to Step 2.
> If the skill returns without a `CHANGE_NAME:` line, the user cancelled —
> end your response with `CANCELLED` and stop.
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

Spawn a **general-purpose subagent** via the Agent tool with the following
prompt:

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
> **Step 2 — Code review**
> Invoke `code-reviewer` via the Skill tool. After the review completes,
> examine the findings:
>
> - If the report contains **no 🔴 Critical or 🟡 Warning findings**: proceed.
> - If the report contains any 🔴 Critical or 🟡 Warning findings: fix every
>   one of them in the source files, then re-invoke `code-reviewer` to confirm
>   no new findings were introduced. Repeat until the report is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `REVIEW_BLOCKED — repeated review cycles` and stop.
> - If you encounter a finding you cannot fix without human input: end your
>   response with `REVIEW_BLOCKED — <brief reason>` and stop.
>
> Do not proceed to Step 3 until all Critical and Warning findings are resolved.
>
> **Step 3 — Security review (conditional)**
> Check whether all changes made so far (Steps 1 and 2) touch any of these
> security-sensitive areas: authentication, authorization, file I/O, user input
> handling, dependency changes (`pom.xml` / `package.json`), or data persistence.
>
> - If **none** of these areas were touched: skip this step.
> - If **any** were touched: invoke `security-reviewer` via the Skill tool.
>   After the review completes, fix every 🔴 Critical and 🟡 Warning finding in
>   the source files, then re-invoke `security-reviewer` to confirm no new
>   findings were introduced. Repeat until the report is clean.
>   If after 3 rounds of fix-and-re-review findings are still present, end
>   your response with `SECURITY_BLOCKED — repeated review cycles` and stop.
>   If you encounter a finding you cannot fix without human input: end your
>   response with `SECURITY_BLOCKED — <brief reason>` and stop.
>   Once the security report is clean, re-invoke `code-reviewer` for a final
>   pass scoped to the security fixes — apply any 🔴 Critical or 🟡 Warning
>   findings before proceeding.
>
> Do not return until all Critical and Warning security and code-review findings
> are resolved.
>
> **Step 4 — Signal completion**
> End your response with exactly this line:
> `IMPLEMENT: DONE`

Do not start this phase until Phase 1 subagent has confirmed artifacts are
ready. Do not start Phase 3 until this subagent returns `IMPLEMENT: DONE`.

---

## Phase 3 — Test (Subagents 3 & 4 — launch in parallel)

Spawn **two general-purpose subagents in a single message** (both Agent tool
calls in the same response) so they run in parallel.

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
>       mock setup, etc.), checking files related to `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/backend/`. Prefer fixing
>       test code over production source code. If you must modify a production
>       source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/backend && mvn test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>       without human input.
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
>       missing `provideNoopAnimations()`, etc.), checking files related to
>       `<change-name>` first.
>    c. Fix the affected files under `JPPhotoManagerWeb/frontend/`. Prefer fixing
>       test code over production source code. If you must modify a production
>       source file, note it with `PROD_CODE_FIXED: <filename> — <reason>`.
>    d. Re-run `cd JPPhotoManagerWeb/frontend && npm test`.
>    e. Repeat until all tests pass or you reach a failure you cannot fix
>       without human input.
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
- **Proceed** — continue to Phase 4 without additional review.
- **Re-run Phase 2** — spawn a new Subagent 2 with the same prompt to review
  and code-review the production fixes, then re-run Phase 3 (spawn new
  Subagents 3 and 4) to confirm all tests still pass before continuing to
  Phase 4.

---

## Phase 4 — Build & Deploy (Subagent 5)

This project can be deployed via Kubernetes (`JPPhotoManagerWeb/k8s/` +
`kustomization.yaml`) or Docker Compose (`JPPhotoManagerWeb/docker-compose.yml`)
— both build the same `photomanager-backend`/`photomanager-frontend` images.
Kubernetes takes priority when both are present, because running
docker-compose's `frontend` service (`ports: "80:80"`) alongside an active
Kubernetes ingress controller fails outright on a host port 80 conflict, and
because a stray `docker compose up` would silently deploy to a disconnected
instance while the real (Kubernetes) environment everyone tests against stays
on the old image.

Spawn a **general-purpose subagent** via the Agent tool with the following
prompt:

> **Step 1 — Check whether Docker is running**
> Run: `docker info`
>
> - If the command fails or returns an error (daemon not running): end your
>   response with `DOCKER: SKIPPED — Docker not running` and stop.
> - If Docker is running: proceed to Step 2.
>
> **Step 2 — Determine the deploy target: Kubernetes or Docker Compose**
> Run: `kubectl get deployment backend frontend -n photomanager --no-headers`
>
> - If this succeeds and lists both `backend` and `frontend`: a live
>   Kubernetes deployment exists. Follow **Step 3K** below, then stop — do
>   not perform Steps 3–7.
> - If it fails for any reason (`kubectl` not installed, no cluster context
>   configured, the namespace or deployments don't exist): there is no live
>   Kubernetes deployment. Continue with **Step 3** below.
>
> **Step 3K — Kubernetes build & deploy**
>
> 1. Build the images (same tags `k8s/backend.yaml`/`k8s/frontend.yaml`
>    reference):
>    ```
>    docker build -t photomanager-backend:latest JPPhotoManagerWeb/backend
>    docker build -t photomanager-frontend:latest JPPhotoManagerWeb/frontend
>    ```
>    Allow up to 10 minutes per build before treating it as a failure.
> 2. Check whether the freshly built image needs loading into the cluster's
>    own container runtime — Docker Desktop's Kubernetes shares the local
>    Docker image cache, so this is often a no-op:
>    ```
>    kubectl config current-context
>    ```
>    - Context name contains `kind`: run
>      `kind load docker-image photomanager-backend:latest photomanager-frontend:latest`
>    - Context name contains `minikube`: run
>      `minikube image load photomanager-backend:latest` and
>      `minikube image load photomanager-frontend:latest`
>    - Otherwise (`docker-desktop`, or a remote cluster pulling from a
>      registry): skip this step.
> 3. Restart both Deployments so they pick up the freshly built `:latest`
>    image — `imagePullPolicy: IfNotPresent` will not repull a tag it
>    already has cached, even after a rebuild:
>    ```
>    kubectl rollout restart deployment/backend deployment/frontend -n photomanager
>    ```
> 4. Wait for the rollout. Give the backend up to 12 minutes — its Spring
>    Boot startup has been observed taking several minutes on
>    CPU-constrained clusters (see the `startupProbe` comment in
>    `k8s/backend.yaml`); a slow-but-successful rollout is expected, not a
>    failure:
>    ```
>    kubectl rollout status deployment/backend -n photomanager --timeout=12m
>    kubectl rollout status deployment/frontend -n photomanager --timeout=2m
>    ```
> 5. Verify:
>    ```
>    kubectl get pods -n photomanager -l 'app in (backend,frontend)'
>    ```
>    Both should show `1/1` and `Running`.
>
> End your response with one of:
> - `DOCKER: DEPLOYED — kubectl rollout restart backend, frontend (namespace photomanager)`
> - `DOCKER: BLOCKED — <brief reason>` if a build, image-load, or rollout
>   failed (or `kubectl rollout status` timed out) and you cannot resolve it
>   without human input.
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

Spawn a **general-purpose subagent** via the Agent tool with the following
prompt:

> Perform these two steps in sequence using the Skill tool:
>
> **Step 1** — Invoke `openspec-archive-change <change-name>`. Wait for it to
> complete fully (the SDD change directory must be moved to
> `openspec/changes/archive/`). This skill may prompt you about delta spec sync
> or incomplete tasks — respond to those prompts normally; they are part of the
> archiving workflow.
>
> **Step 2** — Invoke `improvements-archive <change-name>`. Wait for it to
> complete fully (the improvement row must be updated to `✅ Implemented` in
> `openspec/improvements.md`).
>
> After both steps complete, end your response with exactly this line:
> `ARCHIVE: DONE`

Do not display the Final Summary until this subagent returns `ARCHIVE: DONE`.

---

## Final Summary

After all phases complete, display:

```
## Improvement Development Complete

**Change:** <change-name>
**Artifacts:** ✓ Created
**Implementation:** ✓ All tasks complete
**Code review:** ✓ All findings resolved
**Security review:** ✓ All findings resolved (or N/A — no security-sensitive changes)
**Backend tests:** ✓ All passing
**Frontend tests:** ✓ All passing
**Docker:** ✓ <value from DOCKER signal, e.g. "Deployed — kubectl rollout restart backend, frontend (namespace photomanager)", "Deployed — backend, frontend", or "Skipped — Docker not running">
**SDD change:** ✓ Archived
**Improvement:** ✓ Marked as implemented
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
  returns `IMPLEMENT_BLOCKED`, `REVIEW_BLOCKED`, or `SECURITY_BLOCKED`, surface
  the details to the user and wait for guidance before continuing.
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
- **Missing signal fallback**: if any subagent returns without its expected
  signal, treat it as `BLOCKED`, surface the subagent's raw response to the
  user, and wait for guidance before proceeding to the next phase.
- **No git commits at any point.** Neither this skill nor any subagent it
  spawns may run `git commit`, `git push`, or any other git write command
  at any point in the workflow. This applies to all phases, including after
  tests pass and during archiving. If a subagent or invoked skill attempts
  to commit, block it and continue without committing.
- **No destructive Docker commands.** Do not run `docker compose down`,
  `docker rm`, `docker rmi`, or any command that stops or removes containers
  or images beyond what is strictly required to restart the application
  services being deployed.
- **No destructive kubectl commands.** Do not run `kubectl delete`,
  `kubectl scale --replicas=0`, or `kustomize`/`kubectl apply -k` (which would
  reconcile the whole stack, including PVCs and Secrets, when Phase 4 only
  needs to redeploy two Deployments). `kubectl rollout restart` is the only
  deploy action Phase 4's Kubernetes branch should ever run.
