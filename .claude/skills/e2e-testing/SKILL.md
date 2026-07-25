---
name: e2e-testing
description: >
  End-to-end testing skill for the JPPhotoManager web application (Spring Boot
  3.4 / Java 21 backend + Angular 19 frontend), targeting the live Kubernetes
  deployment in the `photomanager` namespace. TRIGGER when asked to run or
  verify E2E behaviour after completing a feature — especially for UI-facing
  changes to the dashboard, gallery, or any user flow. Also invoked
  automatically by `feature-development` Phase 4.5, after a successful
  Kubernetes deploy and before archiving. Covers: verifying the live
  deployment is ready, API response verification via port-forward, SSE
  progress-stream verification, visual screenshot capture via Puppeteer,
  interactive navigation checks, and an optional multi-replica consistency
  check for changes touching consumer-group or cache-invalidation logic. This
  skill verifies behavior — it never deploys or builds images itself; that's
  `feature-development` Phase 4 / `build-and-deploy-k8s.sh`.
metadata:
  scope: [JPPhotoManagerWeb]
---

# E2E Testing Skill

Verify that a completed feature works correctly end-to-end against the
**live Kubernetes deployment**: database → backend API → Angular frontend →
user interactions. This skill assumes the app is already deployed and
running in the `photomanager` namespace — it verifies behavior, it does not
build images or deploy. If no live deployment exists yet, stop and point the
user at `feature-development` Phase 4 or `build-and-deploy-k8s.sh` directly
rather than trying to stand up a local substitute.

---

## 1. Prerequisites: Verify the Live Deployment

### 1.1 kubectl context and deployments exist

```bash
kubectl config current-context
kubectl get deployment backend frontend -n photomanager --no-headers
```

If either command fails (no context configured, or the `photomanager`
namespace/deployments don't exist), stop — there is nothing to verify
against. Report this to the user rather than falling back to any local
process; a deploy must happen first.

### 1.2 Pods are actually ready

```bash
kubectl get pods -n photomanager -l 'app in (backend,frontend,db,redis,kafka,mongo)'
```

Every pod should show `1/1` (or `N/N`) and `Running`. If `backend`/`frontend`
aren't ready yet right after a fresh deploy, the rollout may still be in
progress:

```bash
kubectl rollout status deployment/backend -n photomanager --timeout=12m
kubectl rollout status deployment/frontend -n photomanager --timeout=2m
```

Spring Boot startup has been observed taking several minutes on
CPU-constrained clusters (see the `startupProbe` comment in
`k8s/backend.yaml`) — a slow-but-successful rollout is expected, not a
failure. If `db`/`redis`/`kafka`/`mongo` aren't ready, that's a cluster
infrastructure problem outside this feature's own deploy — investigate via
`incident-response` rather than proceeding with E2E checks.

**No manual infrastructure startup is needed beyond this check.** Postgres
(`db-0`), MongoDB (`mongo-0`), Redis, and Kafka (`kafka-0`) all run as their
own persistent deployments/statefulsets in the `photomanager` namespace
already. This skill never starts, stops, or scales any infrastructure
service — the only scaling it ever does is §12's optional replica check,
which only scales `backend` (application service), and only with an
explicit scale-back teardown.

---

## 2. Set Up Port-Forwards

Every API/SSE/browser check below goes through **transient port-forwards**,
not the ingress — this avoids depending on a local DNS entry for
`photomanager.local` (a machine-specific setup gap, not something this skill
should require) and matches the direct-service verification pattern
`feature-development` Phase 4's own smoke test already uses. The ingress
path is checked once, informationally, in §8.

```bash
kubectl port-forward -n photomanager svc/backend 18080:8080 > /tmp/e2e-backend-pf.log 2>&1 &
BACKEND_PF_PID=$!
kubectl port-forward -n photomanager svc/frontend 14200:80 > /tmp/e2e-frontend-pf.log 2>&1 &
FRONTEND_PF_PID=$!
sleep 2
```

Record both PIDs — **§10 Teardown must kill both**, even if a check below
fails partway through. Never leave a port-forward running past the end of
the session; a leaked one silently keeps a local port bound and can shadow a
real local dev server the next time someone runs one.

Verify both are actually accepting connections before proceeding:

```bash
curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:18080/api/home/stats
curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:14200/
```

Expect `401`/`403` from the first (an unauthenticated API call reaching a
real controller, not just a raw TCP accept) and `200` from the second
(frontend serving). A connection error or timeout means the port-forward
didn't establish — check `/tmp/e2e-backend-pf.log` / `/tmp/e2e-frontend-pf.log`
before proceeding; a common cause is local port `18080`/`14200` already in
use by a stray port-forward from a previous, uncleanly-ended session (kill
it: `pkill -f "kubectl port-forward.*photomanager"`, then retry).

---

## 3. Authenticate

All protected endpoints require a valid JWT stored in an HttpOnly cookie.
Use `curl` with a cookie jar against the port-forwarded backend to
authenticate and reuse the session for all subsequent API checks.

```bash
curl -sv -c /tmp/cookies.txt -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' 2>&1 | grep "HTTP/"
```

Expected: `HTTP/1.1 200`.

### 3.1 Troubleshooting 401

If login returns 401 the admin password has been changed. Reset it directly
in the live database using a fresh BCrypt hash, via `kubectl exec` into the
Postgres pod — this uses the pod's own trusted local connection, so no
password is needed:

```bash
HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw(b'admin', bcrypt.gensalt(rounds=12)).decode())")
kubectl exec -n photomanager db-0 -- psql -U postgres -d photomanager \
  -c "UPDATE users SET password_hash = '$HASH' WHERE username = 'admin';"
```

Then retry the login. No backend restart is required — password lookups hit
the database on every request.

**Pitfall:** Pre-generated BCrypt hashes found online are often wrong (wrong
cost factor, different password, `$2a` vs `$2b` prefix variation). Always
generate a fresh hash at runtime with the `bcrypt` Python library as shown
above.

**Pitfall — `kubectl exec` CRI proxy error:** on some Docker Desktop
Kubernetes setups, `kubectl exec` against a pod can fail with `error sending
request: ... http: server gave HTTP response to HTTPS client` — this is a
local Docker Desktop / kubectl version-compatibility bug in the exec proxy,
not a problem with the pod, the query, or this skill's approach. It is not
generally fixable from inside a session (no local `psql` client is
guaranteed to be installed as a substitute, and port-forwarding Postgres out
just to run a local client is unnecessary extra surface). If hit, report it
to the user as an environment issue (worth trying a Docker Desktop
Kubernetes restart) rather than attempting other database-mutating
workarounds — resetting a password is the **only** place in this skill that
needs raw DB access; do not extend `kubectl exec` usage to any other check
below.

---

## 4. Verify the Backend API Response

Call the target endpoint with the authenticated cookie and inspect the JSON.
This also doubles as the "real data exists" check — no separate DB query is
needed once authenticated, since `home/stats` already reports asset/folder
counts:

```bash
curl -s -b /tmp/cookies.txt http://localhost:18080/api/home/stats | python3 -m json.tool
```

If `assetCount` is `0`, trigger a catalog run first (§5 covers verifying it
via SSE) before checking any dashboard stats that depend on catalogued data.

Check each field the feature is supposed to populate. For the enriched
dashboard, the expected shape is:

```json
{
  "folderCount": <int>,
  "assetCount": <int>,
  "lastCatalogCompletedAt": "<ISO-8601>",
  "totalFileSize": <long>,
  "duplicateCount": <long>,
  "topFolders": [
    { "path": "<string>", "assetCount": <int> }
  ],
  "recentAssets": [
    {
      "assetId": <long>,
      "fileName": "<string>",
      "folderPath": "<string>",
      "thumbnailUrl": "/api/assets/<id>/thumbnail"
    }
  ]
}
```

Assertions to make manually:
- `topFolders` has at most 5 entries, ordered by `assetCount` descending
- `recentAssets` has at most 12 entries
- Values are internally consistent (e.g. `duplicateCount > 0` only if the
  feature under test is expected to have produced duplicate hashes)

---

## 5. Verify SSE Progress Streams

Catalog, sync, convert, and upload all report progress over Server-Sent
Events rather than a single request/response, so verifying them needs a
different approach than §4's one-shot `curl | json.tool`: the connection
must stay open and each event needs to be captured as it arrives.

### 5.1 Capture events with curl

`curl -N` (no buffering) keeps the connection open and prints each SSE
frame as it's received, against the port-forwarded backend. Run with a
timeout so the command terminates once the operation completes (or after a
sane upper bound if it doesn't):

```bash
timeout 30 curl -N -s -b /tmp/cookies.txt http://localhost:18080/api/assets/catalog
```

Expected output is a stream of `event: <name>` / `data: <payload>` frames
ending in a terminal event (`catalog-done` for catalog; `results`/`status`
for sync and convert — see `KafkaProgressListener` for the exact event names
each stream emits). No terminal event before the timeout means the backend
never received (or never processed) the completion message — check backend
pod logs (`kubectl logs -n photomanager deployment/backend --tail=200`) for
a stuck `@KafkaListener` or a Kafka consumer that never received the message
(cross-check the consumer-group shape against `kafka-events-conventions` §2
if progress is reaching some but not all expected observers).

### 5.2 What to assert

- At least one intermediate progress event arrives before the terminal one
  (a stream that jumps straight to done on a non-trivial catalog run
  suggests progress messages aren't being published, not that the operation
  is unusually fast).
- The terminal event's payload matches reality: for catalog, the
  `foldersScanned`/`assetsAdded` counts in the final notification should be
  checked against the `home/stats` counts (§4) taken before and after the
  run; for sync/convert, the `results` payload's counts should match files
  actually present in the destination directory.
- The stream actually closes after the terminal event (`emitter.complete()`
  server-side) — a `curl -N` that hangs past the terminal event instead of
  the connection closing indicates the emitter wasn't completed.

### 5.3 Verify via the browser (Puppeteer)

`EventSource` isn't directly inspectable via `curl` from the page's own
session (cookies, same-origin behavior differ from a raw `curl` call), so
also confirm the frontend actually renders the streamed updates: trigger the
operation from the UI (e.g. click the catalog button) against the
port-forwarded frontend (§6), then poll the DOM for the progress indicator's
text/value at a couple of points before it reaches 100%/done, in addition to
the final screenshot from §6. A screenshot only at completion can't
distinguish "progress rendered correctly throughout" from "the UI silently
waited and only updated once at the end."

### 5.4 Common SSE pitfalls

- **Wrong consumer group swallows events for this instance.** If the
  deployment is running a single backend replica this won't reproduce, but
  when verifying against a scaled deployment (§12), confirm progress reaches
  every instance's SSE observers, not just one — see
  `kafka-events-conventions` §2 for why this depends on the listener's
  consumer-group configuration.
- **Async dispatch security rejection.** A `403`/`401` mid-stream instead of
  a clean terminal event usually means `SecurityConfig` is missing
  `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as its first
  rule (see `java-developer` §15.2) — check
  `kubectl logs -n photomanager deployment/backend --tail=200` for
  `AuthorizationDeniedException` if a stream cuts off unexpectedly.

---

## 6. Visual Verification via Puppeteer

Puppeteer may be available in the npx cache. Locate it first:

```bash
PUPPETEER_PATH=$(ls "$HOME/.npm/_npx"/*/node_modules/puppeteer 2>/dev/null | head -1)
echo "$PUPPETEER_PATH"   # should print a path; empty means not cached
```

If empty, install it temporarily: `npm install -g puppeteer` and use
`require('puppeteer')` instead of the explicit path below.

Use the following Node.js snippet to:
1. Log in through the Angular login form, served by the port-forwarded frontend
2. Wait for the home dashboard to fully render (stats load asynchronously)
3. Capture a screenshot

```javascript
const puppeteer = require(process.env.PUPPETEER_PATH);
(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });

  // Log in via the Angular form, served by the port-forwarded frontend Service
  await page.goto('http://localhost:14200/login');
  await page.waitForSelector('input[formControlName="username"]', { timeout: 10000 });
  await page.type('input[formControlName="username"]', 'admin');
  await page.type('input[formControlName="password"]', 'admin');
  await page.click('button[type="submit"]');
  await page.waitForNavigation({ timeout: 10000 });

  // Wait for async stats to load
  await new Promise(r => setTimeout(r, 3000));

  await page.screenshot({ path: '/tmp/home-dashboard.png' });
  console.log('Current URL:', page.url());
  await browser.close();
})().catch(e => { console.error(e.message); process.exit(1); });
```

Run with:

```bash
PUPPETEER_PATH=$(ls "$HOME/.npm/_npx"/*/node_modules/puppeteer 2>/dev/null | head -1)
node -e "<paste script here>" 2>&1
```

Then view the screenshot:

```bash
# Open with system viewer, or read via Claude's Read tool
xdg-open /tmp/home-dashboard.png 2>/dev/null
```

**What to check in the screenshot:**
- Page title and navigation bar visible
- All expected UI sections are rendered (quick actions, stat cards, photo strip,
  folder list)
- Numbers in stat cards match the API response values from §4
- Badges, icons, and Material components render without layout breaks

**Pitfall:** The Angular auth guard redirects unauthenticated users to `/login`
immediately. A headless screenshot of `http://localhost:14200/home` without
going through the login form will always show the login page, not the
dashboard. Always log in through the Angular form, not by setting cookies
directly — the app stores session metadata in `localStorage` in addition to
the HttpOnly JWT cookie.

---

## 7. Verify Interactive Navigation

Test click-to-navigate behaviour by clicking a UI element and checking the
resulting URL, against the port-forwarded frontend.

```javascript
const puppeteer = require(process.env.PUPPETEER_PATH);
(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });

  // Log in
  await page.goto('http://localhost:14200/login');
  await page.waitForSelector('input[formControlName="username"]', { timeout: 10000 });
  await page.type('input[formControlName="username"]', 'admin');
  await page.type('input[formControlName="password"]', 'admin');
  await page.click('button[type="submit"]');
  await page.waitForNavigation({ timeout: 10000 });
  await new Promise(r => setTimeout(r, 3000));

  // Click the first recent-photo thumbnail
  await page.click('.strip-item');
  await new Promise(r => setTimeout(r, 3000));

  await page.screenshot({ path: '/tmp/gallery-after-click.png' });
  console.log('URL after click:', page.url());
  await browser.close();
})().catch(e => { console.error(e.message); process.exit(1); });
```

**What to assert:**
- `URL after click` contains `/gallery?folder=` followed by the encoded folder
  path of the thumbnail that was clicked
- The gallery screenshot shows assets from the correct folder pre-loaded
- The folder nav tree shows the pre-selected folder highlighted

For the enriched dashboard, a successful run prints something like:

```
URL after click: http://localhost:14200/gallery?folder=%2Fhome%2F<user>%2FPictures%2FVacation
```

where `<user>` is the OS user who owns the catalogued photo library (as seen
by the backend pod's mounted volume, not necessarily the machine running
this check).

---

## 8. Ingress Check (Informational Only)

```bash
curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://photomanager.local/
```

Expect `200`. A missing local DNS entry for `photomanager.local` on the
machine running this check is an environment gap, not a deployment failure
— note the result either way, but never fail the overall E2E pass based on
this check alone. §2's port-forward checks are what actually gate pass/fail
for every other section in this skill.

---

## 9. CSS Selector Reference

These selectors are used for targeting elements with Puppeteer in this project:

| Feature                        | Selector                               |
| ------------------------------ | --------------------------------------- |
| Login username field           | `input[formControlName="username"]`    |
| Login password field           | `input[formControlName="password"]`    |
| Login submit button            | `button[type="submit"]`                |
| Recent photo thumbnail wrapper | `.strip-item`                          |
| Top-folders row                | `.folder-row`                          |
| Stat card value                | `.stat-value`                          |
| Quick action button            | `.quick-actions button`                |
| Duplicates badge               | `[matbadge]`                           |

---

## 10. Teardown

Kill both port-forwards from §2 — **always run this**, including after a
failed/aborted check, so nothing is left bound past the session:

```bash
kill $BACKEND_PF_PID $FRONTEND_PF_PID 2>/dev/null
```

If the PIDs weren't captured (e.g. resuming after an interrupted prior
session left a stray port-forward), find and kill by pattern instead:

```bash
pkill -f "kubectl port-forward.*photomanager" 2>/dev/null
```

This skill never stops, deletes, or scales down any deployment/pod as part
of teardown — only the port-forward processes it started itself.

---

## 11. Checklist Summary

Use this as a quick reference for any E2E session:

- [ ] `kubectl config current-context` succeeds and `backend`/`frontend` deployments exist in `photomanager` (§1.1)
- [ ] All relevant pods (`backend`, `frontend`, `db`, `redis`, `kafka`, `mongo`) show `Running` (§1.2)
- [ ] Backend and frontend port-forwards established and verified reachable (§2)
- [ ] Login succeeds (`HTTP/1.1 200` from `/api/auth/login`, §3)
- [ ] API response contains all expected fields with correct values, including a non-zero `assetCount` if the feature depends on catalogued data (§4)
- [ ] For SSE-driven features (catalog/sync/convert/upload): intermediate progress events arrive and the terminal event's payload matches API/filesystem state (§5)
- [ ] Puppeteer screenshot shows all UI sections rendered (§6)
- [ ] Clicking an interactive element produces the correct URL and view (§7)
- [ ] No console errors visible in the Puppeteer session
- [ ] Both port-forwards killed (§10) — verify with `pgrep -f "kubectl port-forward.*photomanager"` printing nothing

---

## 12. Multi-Replica Consistency Check (Optional, Occasional)

Not part of the default checklist above — this verifies a claim the app
makes about itself (`k8s/backend.yaml`: "Scaling beyond 1 replica is
supported by the app... plus persistent Kafka consumer groups") rather than
a specific feature. Run it when a change touches
`kafka-events-conventions`-governed consumer-group logic, the
`redis-caching-conventions`-governed cache invalidation paths, or before a
release where multi-instance deployment is actually expected — not on every
session.

### 12.1 Scale up

```bash
kubectl scale deployment/backend -n photomanager --replicas=2
kubectl rollout status deployment/backend -n photomanager --timeout=12m
```

Per `k8s/backend.yaml`'s own comment, the `thumbnails` PVC must already use
a `ReadWriteMany`-capable storage class — the default `ReadWriteOnce` class
on a single-node dev cluster (Docker Desktop, kind, minikube) will leave the
second pod stuck `Pending`. If the rollout doesn't complete, check
`kubectl describe pod -n photomanager -l app=backend` for a PVC-related
scheduling failure before assuming the application code itself is at fault.

### 12.2 What to verify

**Kafka consumer-group shape** (cross-check `kafka-events-conventions` §2):

```bash
# Per-instance group: expect ONE member per running backend instance,
# each with a *different* generated group id (sse-broadcaster-<hostname-or-uuid>)
kubectl exec -n photomanager kafka-0 -- /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list | grep sse-broadcaster

# Shared groups: expect exactly ONE of these regardless of replica count,
# with member count == number of running instances (partition-assignment
# balances across them, but the group itself is singular)
kubectl exec -n photomanager kafka-0 -- /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group asset-search-cache-invalidator
kubectl exec -n photomanager kafka-0 -- /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group audit-log-writer
```

A second `sse-broadcaster-*` group appearing confirms progress events reach
every instance's own SSE observers (§5.4's consumer-group pitfall, now
actually exercised with a real second instance instead of inferred from
config). If `kubectl exec` fails with the CRI proxy error noted in §3.1,
this specific check has no port-forward-based substitute (the Kafka admin
CLI needs to run against the broker) — report the tooling gap rather than
guessing at consumer-group state from logs alone.

**Cross-instance cache invalidation** (cross-check
`redis-caching-conventions` §3): trigger a write that should evict the
`assets` cache for a folder (e.g. add a tag to an asset in that folder)
through **one** backend pod, then immediately read that folder's asset list
through the **other** pod and confirm the tag/updated data is present — not
stale. The `backend` Service load-balances across both pods per-request, so
a single port-forward (§2) already hits a different pod on different
connections; to deterministically target each pod, port-forward directly to
the pod instead of the Service:

```bash
kubectl get pods -n photomanager -l app=backend -o name
# for each pod name returned:
kubectl port-forward -n photomanager <pod-name> 18081:8080 &
```

Use each pod's own forwarded port to isolate which instance served which
request — this is what actually proves cross-instance invalidation rather
than the Service's load balancing coincidentally routing both requests to
the same pod.

### 12.3 Teardown

```bash
kubectl scale deployment/backend -n photomanager --replicas=1
kubectl rollout status deployment/backend -n photomanager --timeout=5m
# plus any pod-direct port-forwards started in §12.2, on top of §10's usual teardown
pkill -f "kubectl port-forward.*photomanager" 2>/dev/null
```
