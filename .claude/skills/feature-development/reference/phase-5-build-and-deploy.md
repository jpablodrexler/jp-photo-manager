# Feature Development — Phase 5 — Build & Deploy

_Part of the `feature-development` skill — see `../SKILL.md` for the overview, Phase 0, placeholder substitution, final summary and the cross-phase guardrails. Read this file in full before starting this phase._

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
completes — see the foreground guardrail in `../SKILL.md`), with the following
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
