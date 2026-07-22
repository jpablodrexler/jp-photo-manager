---
name: e2e-testing
description: >
  End-to-end testing skill for the JPPhotoManager web application (Spring Boot
  3.4 / Java 21 backend + Angular 19 frontend). TRIGGER when asked to run or
  verify E2E behaviour after completing a feature — especially for UI-facing
  changes to the dashboard, gallery, or any user flow. Covers: starting
  prerequisites, API response verification, SSE progress-stream
  verification, visual screenshot capture via Puppeteer, interactive
  navigation checks, and an optional multi-replica Kafka/Redis consistency
  check for changes touching consumer-group or cache-invalidation logic.
metadata:
  scope: [JPPhotoManagerWeb]
---

# E2E Testing Skill

Verify that a completed feature works correctly end-to-end: database → backend
API → Angular frontend → user interactions. This skill documents the exact
steps, commands, and pitfalls encountered during real E2E sessions on this
project.

---

## 1. Prerequisites

Before starting the servers, confirm all infrastructure is ready.

### 1.1 PostgreSQL

The backend requires a running PostgreSQL instance on port 5432. Check first:

```bash
ss -tlnp | grep 5432
```

If nothing is listening, start the local Docker container:

```bash
docker start photomanager-db 2>/dev/null || \
  docker run -d --name photomanager-db \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=photomanager \
    -p 5432:5432 postgres:18
```

**Pitfall:** Port 5432 may already be in use by a local PostgreSQL system
service. In that case the `docker run` will fail with "address already in use".
Use the local service — it should already have the `photomanager` database.
Verify with:

```bash
PGPASSWORD=postgres psql -U postgres -h localhost -p 5432 -l 2>&1 | grep photomanager
```

### 1.2 MongoDB

The backend requires MongoDB for `asset_audit_log` (written by
`AuditLogKafkaListener`/direct `AuditLogRepository.log(...)` calls). Check
first:

```bash
ss -tlnp | grep 27017
```

If nothing is listening, start the local Docker container:

```bash
docker start photomanager-mongo 2>/dev/null || \
  docker run -d --name photomanager-mongo -p 27017:27017 mongo:8
```

No manual index setup needed — `MongoIndexInitializer` ensures the
compound `userId`/`timestamp` index and the 365-day TTL index on
`asset_audit_log` at backend startup.

### 1.3 Redis

The backend requires Redis for the Spring Cache abstraction (`home-stats`,
`sub-folders`, `asset-exif`, `assets`, `tags` caches — see
`redis-caching-conventions`), the thumbnail L2 cache, the refresh-token
mirror, and rate limiting. Check first:

```bash
ss -tlnp | grep 6379
```

If nothing is listening, start the local Docker container **with the
`allkeys-lru` eviction policy** — several of the above features assume
bounded, self-evicting growth rather than an unbounded keyspace:

```bash
docker start photomanager-redis 2>/dev/null || \
  docker run -d --name photomanager-redis -p 6379:6379 \
    redis:7-alpine redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

**Pitfall:** every Redis-backed code path in this app fails open (catches
and logs `WARN` rather than failing the request — see
`redis-caching-conventions` §4), so a *missing* Redis won't produce an
obvious startup error. Symptoms are subtler: cached endpoints always take
the slow (uncached) path, thumbnails always hit disk, and rate limiting is
effectively disabled. If E2E behavior seems to ignore caching entirely,
confirm Redis is actually up before assuming the feature is broken.

### 1.4 Kafka

The backend requires Kafka for catalog/sync/convert/upload progress
streaming and the `asset.cataloged`/`asset.deleted`/`asset.uploaded`
domain events (see `kafka-events-conventions`). Check first:

```bash
ss -tlnp | grep 9092
```

If nothing is listening, start a local single-node KRaft broker (no
Zookeeper needed):

```bash
docker start photomanager-kafka 2>/dev/null || \
  docker run -d --name photomanager-kafka -p 9092:9092 -p 9094:9094 \
    -e KAFKA_NODE_ID=1 \
    -e KAFKA_PROCESS_ROLES=broker,controller \
    -e KAFKA_LISTENERS=PLAINTEXT://:9092,EXTERNAL://:9094,CONTROLLER://:9093 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,EXTERNAL://localhost:9094 \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT \
    -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=false \
    apache/kafka:3.9.0
```

**Pitfall:** `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` means the broker itself
won't create a topic on first use — but this is not a manual setup step you
need to perform. Spring's `KafkaAdmin` (via the `NewTopic` beans in
`config/KafkaTopicConfig.java`) creates all seven topics automatically the
first time the backend starts against this broker. If a listener never
receives anything, confirm the *backend* actually started successfully
against this broker (check `/tmp/backend.log` for `KafkaAdmin` errors)
before suspecting a missing topic.

**Alternative:** `JPPhotoManagerWeb/docker-compose.yml` defines all four
infrastructure services (`db`, `kafka`, `redis`, `mongo`) together —
`docker compose up -d db kafka redis mongo` starts just the infra, not the
app, and can replace 1.1–1.4 in one command. **Port note:** compose maps
Postgres to host port `5433` (`"5433:5432"`), not `5432` — if you use
compose for infra, point `mvn spring-boot:run` at `POSTGRES_PORT=5433`
rather than reusing §1.1's commands verbatim, or stick to the individual
`docker run` commands above for a setup that matches this skill's other
port assumptions exactly.

### 1.5 Verify real data exists

A meaningful E2E test requires catalogued assets. Check counts before
proceeding:

```bash
PGPASSWORD=postgres psql -U postgres -h localhost -p 5432 -d photomanager \
  -c "SELECT COUNT(*) AS assets, SUM(file_size) AS total_size FROM assets;"
```

If the table is empty, run a catalog pass first via the UI or the
`/api/assets/catalog` SSE endpoint before verifying any dashboard stats.

---

## 2. Start the Backend

Run from `JPPhotoManagerWeb/backend/`:

```bash
cd JPPhotoManagerWeb/backend && mvn spring-boot:run -q > /tmp/backend.log 2>&1 &
```

Wait for the backend to be ready (Flyway migrations + Spring context startup
take ~10–15 seconds):

```bash
sleep 15 && curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/home/stats
```

Expected: `403` (unauthenticated) — this confirms the backend is up and the
security filter is active. Any other status (connection refused, 500) means the
backend is not ready; wait a few more seconds and retry.

**Pitfall:** If port 8080 is already occupied by a previous run that was not
shut down, the new process will fail silently. Kill stale processes first:

```bash
pkill -f "mvn spring-boot:run" 2>/dev/null; sleep 2
```

---

## 3. Start the Frontend

Run from `JPPhotoManagerWeb/frontend/`:

```bash
cd JPPhotoManagerWeb/frontend && npm start > /tmp/frontend.log 2>&1 &
```

Wait for the Angular dev server (typically 15–20 seconds):

```bash
sleep 20 && curl -s -o /dev/null -w "%{http_code}" http://localhost:4200/
```

Expected: `200`. The proxy configuration (`proxy.conf.json`) forwards `/api`
requests to `localhost:8080` automatically — no CORS issues in dev.

---

## 4. Authenticate

All protected endpoints require a valid JWT stored in an HttpOnly cookie.
Use `curl` with a cookie jar to authenticate and reuse the session for all
subsequent API checks.

```bash
curl -sv -c /tmp/cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' 2>&1 | grep "HTTP/"
```

Expected: `HTTP/1.1 200`.

### 4.1 Troubleshooting 401

If login returns 401 the admin password has been changed. Reset it directly
in the database using a fresh BCrypt hash:

```bash
HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw(b'admin', bcrypt.gensalt(rounds=12)).decode())")
PGPASSWORD=postgres psql -U postgres -h localhost -p 5432 -d photomanager \
  -c "UPDATE users SET password_hash = '$HASH' WHERE username = 'admin';"
```

Then retry the login. No backend restart is required — password lookups hit
the database on every request.

**Pitfall:** Pre-generated BCrypt hashes found online are often wrong (wrong
cost factor, different password, `$2a` vs `$2b` prefix variation). Always
generate a fresh hash at runtime with the `bcrypt` Python library as shown
above.

---

## 5. Verify the Backend API Response

Call the target endpoint with the authenticated cookie and inspect the JSON:

```bash
curl -s -b /tmp/cookies.txt http://localhost:8080/api/home/stats | python3 -m json.tool
```

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
- `totalFileSize` matches `SELECT SUM(file_size) FROM assets`
- `duplicateCount > 0` when the DB has assets with repeated hashes
- `topFolders` has at most 5 entries, ordered by `assetCount` descending
- `recentAssets` has at most 12 entries

---

## 6. Verify SSE Progress Streams

Catalog, sync, convert, and upload all report progress over Server-Sent
Events rather than a single request/response, so verifying them needs a
different approach than §5's one-shot `curl | json.tool`: the connection
must stay open and each event needs to be captured as it arrives.

### 6.1 Capture events with curl

`curl -N` (no buffering) keeps the connection open and prints each SSE
frame as it's received. Run with a timeout so the command terminates once
the operation completes (or after a sane upper bound if it doesn't):

```bash
timeout 30 curl -N -s -b /tmp/cookies.txt http://localhost:8080/api/assets/catalog
```

Expected output is a stream of `event: <name>` / `data: <payload>` frames
ending in a terminal event (`catalog-done` for catalog; `results`/`status`
for sync and convert — see `KafkaProgressListener` for the exact event names
each stream emits). No terminal event before the timeout means the backend
never received (or never processed) the completion message — check
`/tmp/backend.log` for a stuck `@KafkaListener` or a Kafka consumer that
never received the message (cross-check the consumer-group shape against
`kafka-events-conventions` §2 if progress is reaching some but not all
expected observers).

### 6.2 What to assert

- At least one intermediate progress event arrives before the terminal one
  (a stream that jumps straight to done on a non-trivial catalog run
  suggests progress messages aren't being published, not that the operation
  is unusually fast).
- The terminal event's payload matches reality: for catalog, the
  `foldersScanned`/`assetsAdded` counts in the final notification should be
  checked against the DB state from §1.2 taken before and after the run; for
  sync/convert, the `results` payload's counts should match files actually
  present in the destination directory.
- The stream actually closes after the terminal event (`emitter.complete()`
  server-side) — a `curl -N` that hangs past the terminal event instead of
  the connection closing indicates the emitter wasn't completed.

### 6.3 Verify via the browser (Puppeteer)

`EventSource` isn't directly inspectable via `curl` from the page's own
session (cookies, same-origin behavior differ from a raw `curl` call), so
also confirm the frontend actually renders the streamed updates: trigger the
operation from the UI (e.g. click the catalog button), then poll the DOM for
the progress indicator's text/value at a couple of points before it reaches
100%/done, in addition to the final screenshot from §7. A screenshot only at
completion can't distinguish "progress rendered correctly throughout" from
"the UI silently waited and only updated once at the end."

### 6.4 Common SSE pitfalls

- **Wrong consumer group swallows events for this instance.** If only a
  single backend instance is running locally this won't reproduce, but when
  verifying against a multi-instance deployment, confirm progress reaches
  every instance's SSE observers, not just one — see
  `kafka-events-conventions` §2 for why this depends on the listener's
  consumer-group configuration.
- **Async dispatch security rejection.** A `403`/`401` mid-stream instead of
  a clean terminal event usually means `SecurityConfig` is missing
  `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` as its first
  rule (see `java-developer` §15.2) — check `/tmp/backend.log` for
  `AuthorizationDeniedException` if a stream cuts off unexpectedly.

---

## 7. Visual Verification via Puppeteer

Puppeteer may be available in the npx cache. Locate it first:

```bash
PUPPETEER_PATH=$(ls "$HOME/.npm/_npx"/*/node_modules/puppeteer 2>/dev/null | head -1)
echo "$PUPPETEER_PATH"   # should print a path; empty means not cached
```

If empty, install it temporarily: `npm install -g puppeteer` and use
`require('puppeteer')` instead of the explicit path below.

Use the following Node.js snippet to:
1. Log in through the Angular login form
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

  // Log in via the Angular form
  await page.goto('http://localhost:4200/login');
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
- Numbers in stat cards match the API response values
- Badges, icons, and Material components render without layout breaks

**Pitfall:** The Angular auth guard redirects unauthenticated users to `/login`
immediately. A headless screenshot of `http://localhost:4200/home` without
going through the login form will always show the login page, not the
dashboard. Always log in through the Angular form, not by setting cookies
directly — the app stores session metadata in `localStorage` in addition to
the HttpOnly JWT cookie.

---

## 8. Verify Interactive Navigation

Test click-to-navigate behaviour by clicking a UI element and checking the
resulting URL.

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
  await page.goto('http://localhost:4200/login');
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
URL after click: http://localhost:4200/gallery?folder=%2Fhome%2F<user>%2FPictures%2FVacation
```

where `<user>` is the OS user who owns the catalogued photo library.

---

## 9. CSS Selector Reference

These selectors are used for targeting elements with Puppeteer in this project:

| Feature                        | Selector                               |
| ------------------------------ | -------------------------------------- |
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

After verification, stop the background servers:

```bash
pkill -f "mvn spring-boot:run" 2>/dev/null
pkill -f "ng serve\|npm start\|angular" 2>/dev/null
```

Or kill by PID if you recorded them at startup.

---

## 11. Checklist Summary

Use this as a quick reference for any E2E session:

- [ ] PostgreSQL is running and `photomanager` database exists
- [ ] MongoDB, Redis, and Kafka are all running (§1.2–1.4)
- [ ] Database has catalogued assets (non-zero count)
- [ ] Backend started; `curl localhost:8080/api/home/stats` returns `403`
- [ ] Frontend started; `curl localhost:4200/` returns `200`
- [ ] Login succeeds (`HTTP/1.1 200` from `/api/auth/login`)
- [ ] API response contains all expected fields with correct values
- [ ] For SSE-driven features (catalog/sync/convert/upload): intermediate progress events arrive and the terminal event's payload matches DB/filesystem state
- [ ] Puppeteer screenshot shows all UI sections rendered
- [ ] Clicking an interactive element produces the correct URL and view
- [ ] No console errors visible in the Puppeteer session

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

### 12.1 Getting two backend instances running locally

Neither default local setup supports this out of the box; pick one:

- **Docker Compose** — the checked-in `docker-compose.yml` publishes a fixed
  host port for `backend` (`"8080:8080"`), which makes
  `docker compose up -d --scale backend=2` fail outright (two containers
  can't both bind host port 8080). Don't edit the checked-in file to work
  around this — use a local override instead:
  ```bash
  cat > docker-compose.override.yml <<'EOF'
  services:
    backend:
      ports: []
  EOF
  docker compose up -d --scale backend=2
  ```
  Compose named volumes (`thumbnails`) are natively shared read-write across
  containers on the same host, so — unlike the Kubernetes path below —
  there's no PVC access-mode caveat here; this is the simpler path for a
  local consistency check specifically. Delete `docker-compose.override.yml`
  when done; it's for this check only, not a normal dev setup.
- **Kubernetes** — `kubectl scale deployment/backend -n photomanager --replicas=2`,
  but per `k8s/backend.yaml`'s own comment, the `thumbnails` PVC must already
  use a `ReadWriteMany`-capable storage class — the default `ReadWriteOnce`
  class on a single-node dev cluster (Docker Desktop, kind, minikube) will
  leave the second pod stuck `Pending`. Only worth setting up if you're
  specifically validating the Kubernetes-realistic path; Compose is enough
  to verify the application-level consumer-group/cache logic in isolation.

### 12.2 What to verify

**Kafka consumer-group shape** (cross-check `kafka-events-conventions` §2):
```bash
# Per-instance group: expect ONE member per running backend instance,
# each with a *different* generated group id (sse-broadcaster-<hostname-or-uuid>)
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list | grep sse-broadcaster

# Shared groups: expect exactly ONE of these regardless of replica count,
# with member count == number of running instances (partition-assignment
# balances across them, but the group itself is singular)
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group asset-search-cache-invalidator
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group audit-log-writer
```
A second `sse-broadcaster-*` group appearing confirms progress events reach
every instance's own SSE observers (§6.4's consumer-group pitfall, now
actually exercised with a real second instance instead of inferred from
config).

**Cross-instance cache invalidation** (cross-check
`redis-caching-conventions` §3): trigger a write that should evict the
`assets` cache for a folder (e.g. add a tag to an asset in that folder)
through **one** instance's port, then immediately read that folder's asset
list through the **other** instance's port and confirm the tag/updated data
is present — not stale. If both instances are behind the same load balancer
(the normal case via `frontend`'s nginx / the K8s Service), this happens
automatically across requests; hitting each backend port directly (Compose:
whatever host ports the two scaled containers landed on, `docker compose ps`
to find them) is what actually isolates *which* instance served which
request, which a single shared front door would hide.

### 12.3 Teardown

```bash
docker compose up -d --scale backend=1   # or: kubectl scale deployment/backend --replicas=1
rm -f docker-compose.override.yml        # if created for §12.1
```
