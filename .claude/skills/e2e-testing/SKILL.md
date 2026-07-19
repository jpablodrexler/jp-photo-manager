---
name: e2e-testing
description: >
  End-to-end testing skill for the JPPhotoManager web application (Spring Boot
  3.4 / Java 21 backend + Angular 19 frontend). TRIGGER when asked to run or
  verify E2E behaviour after completing a feature — especially for UI-facing
  changes to the dashboard, gallery, or any user flow. Covers: starting
  prerequisites, API response verification, visual screenshot capture via
  Puppeteer, and interactive navigation checks.
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

### 1.2 Verify real data exists

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

## 6. Visual Verification via Puppeteer

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

## 7. Verify Interactive Navigation

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

## 8. CSS Selector Reference

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

## 9. Teardown

After verification, stop the background servers:

```bash
pkill -f "mvn spring-boot:run" 2>/dev/null
pkill -f "ng serve\|npm start\|angular" 2>/dev/null
```

Or kill by PID if you recorded them at startup.

---

## 10. Checklist Summary

Use this as a quick reference for any E2E session:

- [ ] PostgreSQL is running and `photomanager` database exists
- [ ] Database has catalogued assets (non-zero count)
- [ ] Backend started; `curl localhost:8080/api/home/stats` returns `403`
- [ ] Frontend started; `curl localhost:4200/` returns `200`
- [ ] Login succeeds (`HTTP/1.1 200` from `/api/auth/login`)
- [ ] API response contains all expected fields with correct values
- [ ] Puppeteer screenshot shows all UI sections rendered
- [ ] Clicking an interactive element produces the correct URL and view
- [ ] No console errors visible in the Puppeteer session
