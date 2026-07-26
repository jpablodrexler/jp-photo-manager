---
name: release-e2e-suite
description: >
  Full-regression, real-backend end-to-end test pass for the JPPhotoManager
  web application, covering every feature area with real seeded data — not
  mocked API responses. TRIGGER before finishing a release (invoked from the
  `gitflow` skill's "finish release"/"finish hotfix" actions), or whenever
  explicitly asked to run the full E2E regression suite. Generates
  synthetic test images, seeds them into the live Kubernetes deployment's
  dedicated /e2e-catalog test folder via the app's real Upload feature, then
  runs the real (unmocked) Cypress E2E spec suite under
  JPPhotoManagerWeb/frontend/cypress/e2e/release/ against every route:
  auth, catalog run + SSE, home dashboard, gallery (thumbnails, viewer,
  rating, tagging, search), duplicates, albums, sync, convert, recycle bin,
  analytics, user administration, and profile sessions. Always tears down
  its own test data at the end, even on failure. This is a much heavier,
  slower pass than `feature-development` Phase 4.5's per-feature
  `e2e-testing` spot-check — it exists for release-time confidence, not for
  running after every feature. Mandatorily rebuilds and redeploys both
  images from the current branch before running a single spec, every time
  it's invoked (confirmed with the user first, like any live-cluster
  action) — a deployment merely existing and looking healthy is not
  sufficient, since it may be running code that predates the fix/feature
  under test (see §0's documented incident for exactly what that risks).
  The dedicated e2e-suite-admin account is exempt from login/catalog rate
  limiting (see §1.3) so repeated runs aren't throttled. Every run writes a
  dated pass/fail report to docs/reports/release-e2e/ (gitignored), mirroring the
  code-reviewer skill's report convention (see §3.1).
metadata:
  scope: [JPPhotoManagerWeb]
---

# Release E2E Suite Skill

Run a comprehensive, real-backend regression pass against the live
Kubernetes deployment before a release ships: every feature area, real
frontend↔backend interaction (no `cy.intercept` mocking), real seeded data
(synthetic test images generated for this purpose), verifying the whole
application together rather than one feature in isolation.

This skill seeds data, runs the suite, and tears down — it does not decide
whether a release is ready beyond reporting pass/fail, and it never builds
or deploys as a *routine* part of its own operation. §0 below is the one
deliberate exception: redeploying from the current branch is a mandatory
precondition this skill enforces every time it runs, not something it
merely checks for and hopes is already true.

---

## 0. Mandatory: redeploy from the current branch before running anything

**Before running a single spec — every time this skill is invoked, not just
the first time — rebuild and redeploy both images from the current branch's
working tree.** Do not proceed on the assumption that whatever is currently
running in the `photomanager` namespace already reflects the code under
test, even if a deployment exists and its pods are `Running`. "A deployment
exists" and "the deployment is running the code on this branch" are
different claims, and only the second one makes this suite safe to run.

```bash
bash JPPhotoManagerWeb/scripts/build-and-deploy-k8s.sh
```

This is a live-cluster action — confirm with the user before running it,
the same as any other deploy. It's also idempotent and safe to re-run (see
the script's own header), so there's no cost to redeploying even when
nothing actually changed since the last run.

**Known incident — why this is mandatory, not advisory:** while building
this skill, it was validated against a deployment that *looked* ready
(pods `Running`, `/e2e-catalog` volume present) but was still running a
backend image built before that session's own gallery folder-deduplication
fix (migration `V35` + the atomic `findOrCreateByPath` pattern — see
`java-developer` §6.8 / `database-reviewer` §3.5). That stale image had the
exact duplicate-folder-rows bug the fix addresses: a query for the
suite's own `/e2e-catalog/trip` test folder returned the operator's real
personal photos from an unrelated real folder instead, because ambiguous
folder-path resolution silently mixed the two up. No data was actually lost
that time (verified by hand, asset-by-asset, after the fact) only because
the test uploads that would have populated `/e2e-catalog/trip` happened to
fail first — `99-teardown.cy.ts`'s hard-delete step never found anything to
act on. Had the uploads succeeded, teardown's own real "Delete files" action
would have deleted real files through that same folder-path confusion. A
fresh redeploy immediately fixed it: rebuilding and reapplying from the
branch that already contained the fix made the confusion disappear
entirely, confirmed by re-querying the same endpoints that had returned the
wrong photos moments before.

The lesson generalizes beyond this one bug: this suite creates and deletes
real data through the app's real feature surface (real uploads, real
catalog runs, real hard-deletes). Running any of that against code that
predates whatever fix or feature the release is meant to validate risks
exactly this kind of cross-contamination between synthetic test data and
the operator's real photo library — not a hypothetical, since it already
happened once. Treat "redeploy first" as non-negotiable, not as a step to
skip because a deployment already appears to be up.

---

## 0.1 Cluster prerequisite: the `/e2e-catalog` volume

This entire suite depends on a dedicated, isolated catalog root —
`/e2e-catalog`, an `emptyDir` volume declared directly in `k8s/backend.yaml`
(and included in `CATALOG_DIR` in `k8s/configmap.yaml`) — existing
specifically so generated test images never touch the three real,
machine-specific personal directories (`/catalog`, `/catalog2`, `/catalog3`
— see `k8s/catalog-volumes.yaml.example`). Unlike those three, `/e2e-catalog`
needs no real host path, so it's safe to declare directly in the versioned
manifest. The redeploy in §0 already applies this on every run, since
`build-and-deploy-k8s.sh` re-applies the full manifest set — this section
is just what to check if that redeploy didn't actually pick it up (e.g. the
branch under test doesn't yet have this volume declared at all).

Verify before proceeding to §1:

```bash
kubectl get deployment backend -n photomanager -o jsonpath='{.spec.template.spec.containers[0].volumeMounts[?(@.name=="e2e-catalog")].mountPath}'
# expect: /e2e-catalog
```

If this doesn't print `/e2e-catalog`, stop — seeding will silently write test
images into the container's ephemeral root filesystem instead of the
intended volume (still isolated from the host, but without the deliberate
"wiped on pod restart, explicitly test-only" semantics), or `CATALOG_DIR`
won't include the path at all and the app's own folder-path handling may
reject it. Redeploy first.

---

## 1. Prerequisites

Follow `e2e-testing` §1 (verify the live deployment: kubectl context,
`backend`/`frontend` deployments exist, pods ready) exactly as written
there — don't restate it, read that skill. By the time you reach this
step, §0 has already redeployed fresh, so "pods ready" here is confirming
that fresh rollout settled, not just that *some* deployment exists.

### 1.1 Access path: the real ingress, not a port-forward

Every spec drives a real browser (Cypress) against
`http://photomanager.local` — the actual Kubernetes ingress domain, not a
`kubectl port-forward` to an arbitrary local port. Confirm it resolves and
responds before running anything:

```bash
curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://photomanager.local/
```

Expect `200`. If it doesn't resolve, the machine is missing the one-time
hosts-file entry `build-and-deploy-k8s.sh` documents in its own "Next
steps" output (`127.0.0.1 photomanager.local`) — add it (requires admin)
rather than falling back to a port-forwarded port; see the incident below
for why the port-forward path is a trap, not a convenience.

**Known incident — why not just port-forward the frontend Service like
`e2e-testing` does:** this skill originally did exactly that
(`kubectl port-forward svc/frontend 14200:80`, then
`--config baseUrl=http://localhost:14200`). Every spec failed at the very
first real login with a 403 the UI displayed as "Invalid username or
password" — misleading, since `LoginComponent` hardcodes that message for
*any* login error regardless of the real cause. The actual backend response
was `Invalid CORS request`: `AppConfig.corsFilter()`'s allow-list
(`photomanager.cors-allowed-origins`) only contains `http://localhost:4200`
(the Angular dev-server default) by default, and real browsers send an
`Origin` header for this kind of request that a port-forwarded
`localhost:14200` simply isn't allowed to use — confirmed by reproducing
the exact rejection with `curl -H "Origin: http://localhost:14200"`, and by
confirming `http://photomanager.local` (a real browser session, via
Cypress/Electron) logs in successfully with no CORS changes needed at all.
Going through the real ingress sidesteps the whole problem: it's the access
path the app is actually configured (and, unlike `localhost:14200`, allowed)
for, and it's simpler than maintaining a port-forward besides. `e2e-testing`
§2 still uses a backend/frontend port-forward for its own curl-only API/SSE
checks, which never hit this problem since curl doesn't send an `Origin`
header unless explicitly told to — the failure mode is specific to a real
browser navigating to a non-allow-listed origin, which is exactly what
every spec in this suite does.

### 1.2 Admin credentials

Every spec logs in as a real admin user via
`cypress/support/e2e-release-helpers.ts`'s `ADMIN_USERNAME`/`ADMIN_PASSWORD`,
which are read from `Cypress.env()` — **never hardcoded in any committed
file**. Supply them via `JPPhotoManagerWeb/frontend/cypress.env.json`
(gitignored, create it locally):

```json
{
  "E2E_ADMIN_USERNAME": "e2e-suite-admin",
  "E2E_ADMIN_PASSWORD": "<the real password>"
}
```

Do not assume the seeded default (`admin`/`admin`) works — `DataInitializer`
explicitly warns to change it on first boot, so a real deployment's admin
password is expected to differ, and the suite fails loudly (a clear error
from the helper module, not a mysterious login timeout) if the env vars are
unset rather than silently falling back to the default.

**Prefer a dedicated admin account over the shared real `admin` login** —
this suite creates and deletes throwaway data/users as a normal part of its
run, and keeping that blast radius on its own account rather than the
operator's real login is worth the one-time setup cost. There is currently
no self-service way to create a second admin through the app itself (the
Admin Users UI/API only ever creates `role = 'USER'` accounts — confirmed
while building this skill; `CreateUserRequestDto` doesn't even accept a
`role` field) — promoting one requires a direct one-row database update:
`UPDATE users SET role = 'ADMIN' WHERE username = '<the account>';`. Ask the
user to run this (they may have DB access this skill doesn't — e.g.
`scripts/port-forward-k8s.sh`'s Postgres tunnel + a client like DBeaver) or
to just supply the real admin password instead if a dedicated account isn't
worth the friction for a given run.

No port-forward setup is needed for anything in this suite — every backend
call the Cypress specs make (`fetch`/`XHR` from the app itself, or
`cy.request()` for the couple of specs that verify state directly) goes
through the real ingress → frontend nginx → backend Service path described
in §1.1, exactly like a real user's browser.

### 1.3 Rate-limit exemption for the admin account

`RateLimitFilter` normally limits `/api/auth/login` to 10 requests/60s and
`/api/assets/catalog` to 5 requests/hour per client IP — a repeated suite run
(each of the 13 spec files logs in at least once, and `01-seed-and-catalog`
triggers a real catalog run) can legitimately exceed both within a single
session, which is a real problem this suite hit while being built: a burst
of manual debugging runs exhausted the catalog limit and returned a genuine
`429`/`Retry-After` from the live deployment, not a bug in the app.

The fix is `photomanager.rate-limit-exempt-usernames` (env var
`RATE_LIMIT_EXEMPT_USERNAMES`), set in `k8s/configmap.yaml` to
`e2e-suite-admin` — see `RateLimitFilter`'s JWT-cookie check (catalog) and
login-body check (login). This is scoped to that one dedicated, throwaway
account only; never add a real user account to that list, since the login
exemption specifically trades away brute-force protection for whichever
usernames are listed (see the property's comment in `application.yml` for
the full rationale). The redeploy in §0 already applies this configmap
change on every run — if login/catalog 429s recur despite this section,
confirm the deployed configmap actually has this value
(`kubectl get configmap photomanager-config -n photomanager -o jsonpath='{.data.RATE_LIMIT_EXEMPT_USERNAMES}'`)
rather than assuming the exemption code itself regressed.

---

## 2. Run the Real-Backend Cypress Suite

Everything else — generating test images, seeding them via the app's real
Upload feature, exercising every feature area, and cleaning up the test
data afterward — lives **inside** the Cypress spec suite itself
(`JPPhotoManagerWeb/frontend/cypress/e2e/release/*.cy.ts`), not in this
skill's own steps. This is intentional: the specs are ordinary, reviewable,
version-controlled Cypress tests (see
`JPPhotoManagerWeb/frontend/cypress/support/e2e-release-helpers.ts` for the
shared real-login/upload helpers), not a shell script this skill improvises
per run.

```bash
cd JPPhotoManagerWeb/frontend
npx cypress run --config-file cypress.release.config.ts \
  --config baseUrl=http://photomanager.local
```

**Always use `cypress.release.config.ts`, never the default
`cypress.config.ts` (with or without a `--spec` filter).** The two are
deliberately separate config files, not one config with a directory
exclusion toggled per-run: `cypress.config.ts`'s `e2e.excludeSpecPattern`
permanently excludes `cypress/e2e/release/**` so a bare `npm run test:e2e`/
`cypress open --e2e` never touches it — and that exclusion applies *before*
spec selection, meaning `--spec "cypress/e2e/release/**/*.cy.ts"` against
the default config silently resolves to zero specs rather than the 13
release specs (confirmed while building this skill — don't rediscover this
the hard way). `cypress.release.config.ts` exists specifically so this
skill has an unambiguous, correctly-scoped entry point.

**Spec order matters and is enforced by filename prefix** (Cypress runs
spec files in the glob's lexicographic order): `00-auth` → `01-seed-and-catalog`
(uploads the shared test dataset every later spec depends on, then verifies
the catalog-run/SSE feature) → `02` through `11` (one file per remaining
feature area) → `99-teardown` (always runs last, hard-deletes every
remaining test asset and purges the recycle bin). Never reorder or rename
these files without preserving that dependency chain, and never add a new
feature-area spec with a prefix that would run before `01` or after `99`.

**Expected duration**: significantly longer than `e2e-testing`'s spot-check
— real uploads, a real catalog job, real sync/convert jobs, and ~13 spec
files each doing several real page loads. Budget on the order of several
minutes, not seconds; don't treat a long run as evidence something is stuck
unless individual Cypress command timeouts (visible in the run output) are
actually being hit.

---

## 3. Interpret Results

Cypress's own exit code and per-spec pass/fail summary (printed at the end
of the `cypress run` output) is authoritative. On any failure:

- Cypress auto-captures a screenshot into
  `JPPhotoManagerWeb/frontend/cypress/screenshots/<spec-file>/` for every
  failed test — read the relevant one(s) via the Read tool before
  diagnosing from the error text alone; a layout break or an unexpected
  dialog is often obvious in the screenshot but not in the assertion
  message.
- Because every spec after `01-seed-and-catalog.cy.ts` depends on that
  spec's seeded data, a failure there should be treated as blocking for
  every later spec's results, not investigated per-file — fix the seeding
  failure first (most likely cause: §0's redeploy didn't actually happen or
  didn't include the `/e2e-catalog` volume — recheck §0.1 — or the Upload
  feature itself regressed) and
  re-run the whole suite rather than cherry-picking which specs to re-run.
- `99-teardown.cy.ts` failing is a genuine problem — it means test data may
  have leaked into the live deployment's real dashboard/analytics. Report
  it explicitly rather than folding it into a generic "N specs failed"
  count; a follow-up manual cleanup (re-run just `99-teardown.cy.ts`, or a
  backend pod restart to wipe the `/e2e-catalog` emptyDir entirely) is
  warranted for a genuinely irrecoverable teardown failure — confirm with
  the user before restarting the pod, since that's a live-deployment
  action.

### 3.1 Write the report to a dated markdown file

Every run — pass, fail, or partial — also gets written to a markdown file,
the same way `code-reviewer` writes `CODE_REVIEW_FINDINGS_*.md`, so a run's
outcome can be revisited later without re-deriving it from scrollback.

- **Path:** `docs/reports/release-e2e/RELEASE_E2E_REPORT_{YYYY-MM-DD}.md`, today's
  date, ISO 8601. **This `docs/` is the top-level repository root's
  `docs/` — the one sibling to `JPPhotoManager/` and `JPPhotoManagerWeb/`
  and containing `.git` — never `JPPhotoManagerWeb/docs/`,** even though
  every other step in this skill runs from inside `JPPhotoManagerWeb/`.
  Verify with `git rev-parse --show-toplevel` if unsure before writing.
  If a file for that date already exists (e.g. a second run the same day),
  append `-2`, `-3`, etc. before `.md` rather than overwriting the earlier
  run's report. Create the directory if it doesn't exist yet — it's
  gitignored (`docs/reports/release-e2e/`), same as `docs/reports/code-review/`: these are
  local working artifacts, not committed history.
- **Header:** the git branch and commit hash under test (`git rev-parse
  HEAD`), confirmation that §0's redeploy ran, and which admin account was
  used.
- **Per-spec results:** one GitHub task-list checkbox line per spec file, in
  the same `00` → `99` order they ran in, `- [x]` for every test in that file
  passing and `- [ ]` for any failure, with the pass count (e.g. `- [x]
  03-gallery — 5/5 passed`, `- [ ] 04-duplicates — 2/3 passed: <test name>
  failed`).
- **Failures:** for each failing spec, the assertion/error text, the
  screenshot path from §3, and root cause once diagnosed (or "not yet
  diagnosed" if the report is written before investigating).
- **Overall verdict:** total specs/tests passed vs. failed, and whether
  `99-teardown.cy.ts` succeeded (call this out explicitly either way, per the
  bullet above).
- **Scope of content:** write only what this run actually found — don't
  carry forward unresolved items from a previous dated report by default,
  matching `code-reviewer`'s same rule.

---

## 4. Teardown

Nothing to do here for this skill's own infrastructure — §1.1 established
that no port-forward is used, so there's no process of this skill's own
left running to kill. Test *data* cleanup is `99-teardown.cy.ts`'s job
(§2/§3), and §0's redeploy is the one live-cluster state change this skill
makes, which is meant to persist (it's the point of running this skill in
the first place) rather than being torn down afterward.

---

## 5. Maintaining the Suite

When a new feature is added to the web application (a new route, or new
functionality on an existing route), add a corresponding spec file under
`cypress/e2e/release/` rather than folding it into an existing one — one
file per feature area, matching the existing files' scope, so a failure's
blast radius stays legible from the filename alone. Reuse
`cypress/support/e2e-release-helpers.ts` for login/navigation/upload rather
than duplicating that logic per spec. If the new feature needs its own
seeded data, prefer extending `01-seed-and-catalog.cy.ts` (or, if the data
is only relevant to one feature, seeding it directly in that feature's own
spec via the same real-Upload-feature pattern) over reaching for
`kubectl exec`/`kubectl cp` — both are known to fail on at least one real
Docker Desktop Kubernetes setup (see the CRI proxy error class documented
in `e2e-testing` §3.1) and, more importantly, driving seeding through a real
app feature is itself E2E coverage, not just test setup.

New synthetic test images should be generated with
`JPPhotoManagerWeb/scripts/generate-e2e-test-images.js` (a pure Node.js PNG
encoder — no ImageMagick/ffmpeg/Pillow dependency) and committed as Cypress
fixtures under `cypress/fixtures/e2e-release/`, the same way the existing
six were produced, rather than sourced from any external/stock image.
