---
name: e2e-suite
description: >
  The maintained, committed end-to-end regression suite for
  JPPhotoManagerWeb's frontend — Cypress E2E driving a real Chrome/Electron
  instance against the full application redeployed to the local Kubernetes
  cluster (`./scripts/build-and-deploy-k8s.sh`), reached through the
  cluster's own ingress rather than a local `ng serve` dev server. Lives at
  `JPPhotoManagerWeb/frontend/cypress/e2e/` (excluding `mocked/`), runs via
  `npm run test:e2e` from `JPPhotoManagerWeb/frontend`. Covers auth
  (login/logout/guard), albums (full CRUD), admin/users (create/change-
  password/delete a throwaway user), and profile/sessions (list + revoke).
  Unlike the `e2e-testing` skill's improvised per-session scripts, this is
  version-controlled, self-cleaning, and re-runs identically every time. A
  sibling, `cy.intercept`-mocked golden-path smoke tier lives at
  `cypress/e2e/mocked/` (its own `cypress.mocked.config.ts`, `npm run
  test:e2e:mocked`) — needs no running backend and is the one wired into
  CI; see §9. TRIGGER when asked to run the E2E suite/regression tests, add
  an E2E test for a new feature, extend E2E coverage, or fix a
  failing/flaky E2E spec — including a mocked-tier spec.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.1"
  scope: [JPPhotoManagerWeb]
---

# E2E Suite Skill

A real, maintained regression suite — not ad-hoc scripts. Every spec file
is committed, runs the same way every time, and cleans up after itself.
Nothing here is mocked: it drives an actual Chrome/Electron instance
(Cypress) against the real application — frontend and backend images built
from current source and running as pods in the local Kubernetes cluster,
reached through `k8s/ingress.yaml` at `http://photomanager.local` — rather
than a local `ng serve` dev server talking to ad-hoc `docker run`
containers.

**Relationship to `e2e-testing`**: that skill is for a quick, targeted
check right after implementing one feature — a throwaway scratch pass, no
persisted artifacts, and it also covers ground this suite deliberately
doesn't (SSE progress-stream verification, Puppeteer screenshots, the
multi-replica consistency check). This skill is the opposite: narrower in
what it drives (browser interactions only, via Cypress) but broad,
committed, repeatable CRUD/business-rule coverage you run on demand (before
a release, after a refactor, when asked to verify nothing broke). The two
skills no longer share the same backend prerequisites: `e2e-testing` still
drives a locally-running `ng serve` + ad-hoc `docker run` containers for
its scratch checks, while this suite always runs against a fresh
Kubernetes redeploy (§1 below) — `admin`/`admin` credentials are the one
thing still shared between them. This app's `frontend/src/**/*.cy.ts`
**component** tests are a separate, unrelated layer — see the
`cypress-unit-test-developer` skill for those; this skill is E2E only.

**Relationship to the mocked E2E smoke tier**: `cypress/e2e/mocked/` is a
*third*, distinct E2E layer — see §9 below. Where this suite drives a real
login against a real, running backend, the mocked tier fabricates a session
via `localStorage` and stubs every API call via `cy.intercept`, trading this
suite's full business-rule depth for something that needs no backend/
infrastructure at all — specifically so it can run in CI, which this suite
still deliberately doesn't (§8).

---

## 1. Running the suite

**Always redeploy to the local Kubernetes cluster first — never bring up
docker-compose or an ad-hoc `docker run` stack for this suite.** This
project standardized on a local k8s cluster (Docker Desktop's built-in
Kubernetes, kind, or minikube — see `docs/kubernetes.md`) as the one
environment the real E2E tier runs against; docker-compose repeatedly hit a
Kafka advertised-listener hostname (`kafka`) that can't resolve from the
host when the backend runs outside the cluster, which the k8s path avoids
entirely (backend and Kafka both run in-cluster and talk over in-cluster
DNS — Cypress on the host only ever talks to the ingress).

From `JPPhotoManagerWeb`, before every real-tier run:

```bash
./scripts/build-and-deploy-k8s.sh
```

This builds fresh backend/frontend images from current source, applies the
full manifest set, and restarts the `backend`/`frontend` Deployments so the
new images actually get picked up (`imagePullPolicy: IfNotPresent` won't
repull a `:latest` tag it already has cached) — see `docs/kubernetes.md`
for what it does step by step. It requires `k8s/secret.yaml` and
`k8s/catalog-volumes.yaml` to already exist (copied from their `.example`
templates and filled in once, machine-specific, git-ignored) and fails
loudly with the exact `cp` command if either is missing rather than
deploying broken config. It's idempotent — safe to run before every single
E2E session, not just the first time.

Wait for pods to settle before running Cypress — a fresh image can take a
few minutes to become ready (Flyway migrations + joining Kafka consumer
groups), and a still-`CrashLoopBackOff`/`0/1` pod means requests will just
time out or 502 through the ingress rather than fail with anything
Cypress-legible:

```bash
kubectl get pods -n photomanager -w
```

Ctrl-C once every pod reads `1/1 Running` with stable `RESTARTS` (a restart
or two while dependencies start up is normal — see
`docs/kubernetes.md`#troubleshooting). Then, from
`JPPhotoManagerWeb/frontend`:

```bash
npm run test:e2e
```

This runs `cypress run --e2e` against `cypress.config.ts`'s real (non-
mocked) `e2e` block, whose `baseUrl` points at `http://photomanager.local`
— the app as served through the cluster's own ingress, not a local `ng
serve` — and which already `excludeSpecPattern`s `cypress/e2e/mocked/**`,
so a plain `npm run test:e2e` never picks up the mocked specs. Like the
mocked tier's `npm run test:e2e:mocked`, this command does **not** start
anything itself — no dev server, no port-forward — since the cluster's
ingress-nginx controller (installed by `build-and-deploy-k8s.sh`) is
already serving the app the moment the redeploy above finishes.
`http://photomanager.local` requires a one-time hosts-file entry
(`127.0.0.1 photomanager.local` — see `docs/kubernetes.md`'s "Accessing
services" section); `build-and-deploy-k8s.sh` prints the exact command if
it's ever missing, but does not add it itself (needs admin rights).

To run a single spec file directly while writing a new one:

```bash
npx cypress run --e2e --spec "cypress/e2e/albums.cy.ts"
```

**Credentials**: fixed, not a secret file. The seeded `admin`/`admin`
account (`DataInitializer`, seeded once if the `users` table is empty) is
what every spec logs in as — no `.e2e-credentials.json` equivalent needed,
since there's no email-confirmation constraint like a third-party auth
provider would impose. `admin`/`admin` is already documented in the
`e2e-testing` skill and used directly in the mocked tier's `login.cy.ts`,
so it isn't a new secret this suite introduces.

**Login rate limiting**: this backend's own `RateLimitFilter` (Bucket4j)
caps `POST /api/auth/login` at 10 requests per 60 seconds per IP — a
legitimate anti-brute-force control, confirmed working as designed, not a
bug. Running the *entire* suite (`npm run test:e2e`, all 4 spec files) back
to back in one invocation can consume all 10 within the run (`cy.session()`
does not eliminate every real login — `auth.cy.ts` alone issues 3 genuine
`POST /api/auth/login` calls by design, since two of its tests exercise the
real login form itself rather than a cached session), leaving
`profile-sessions.cy.ts` needing more than what's left. If `npm run
test:e2e` fails with `POST 429 /api/auth/login` on `profile-sessions.cy.ts`
(or any spec) after another spec ran moments before, this is why — either
wait ~60s between runs, or run spec files individually/in smaller batches
(`npx cypress run --e2e --spec "cypress/e2e/admin-users.cy.ts,cypress/e2e/
albums.cy.ts,cypress/e2e/auth.cy.ts"` followed by `profile-sessions.cy.ts`
separately is a batching split confirmed to stay under the limit). Don't
raise the backend's rate limit to work around this — it's a real security
control, not a test-suite bug.

---

## 2. Directory layout

```
JPPhotoManagerWeb/frontend/
  cypress.config.ts              # e2e (real backend) + component blocks
  cypress.mocked.config.ts       # e2e (mocked tier) — separate config, see §8
  cypress/
    tsconfig.json
    support/
      e2e.ts                        # shared e2e support file — imports commands.ts
      commands.ts                    # cy.login/cy.logout, E2E_PREFIX, uniqueSuffix, strongPassword
      mocked/
        seed-session.ts              # mocked-tier only — see §8, not used here
    e2e/
      auth.cy.ts                       # login/logout, guard redirect, invalid-credentials error
      albums.cy.ts                      # album CRUD (create/open/delete), zero-photo album
      admin-users.cy.ts                  # create/change-password/delete a throwaway user
      profile-sessions.cy.ts              # current-session flag/revoke-guard, sign-out-everywhere-else
      mocked/                                # a different E2E layer entirely — see §8
```

---

## 3. Test-data conventions

Every piece of test data this suite creates (album names, secondary
usernames) is prefixed with `E2E_PREFIX` (`"zzE2E"`, in
`cypress/support/commands.ts`) plus `uniqueSuffix()` (a timestamp + short
random string) — e.g. `zzE2E album 1786469088924-p48hp`. This makes every
row this suite creates unambiguously identifiable, which the cleanup
mechanic (§4 below) depends on.

**No bulk "delete everything with this prefix" endpoint exists** in this
backend, unlike a database client with row-level access. Every spec must
capture the id of whatever it creates (via a follow-up `GET` after
creation, matching the response against the known name/username) and
delete it explicitly — either through the real UI (the last test in a spec
usually does this as part of what it's testing) or via a direct `cy.request`
`DELETE` call in an `after()` hook as a safety net if the in-test delete
didn't run (e.g. an earlier test in the file failed). This is the *default*
pattern here, for every table this backend exposes.

**Never operate on the seeded `admin` account.** Confirmed via
`DeleteUserUseCaseImpl`: the backend has no safeguard against deleting the
last admin or the currently-logged-in user. `admin-users.cy.ts` only ever
creates/modifies/deletes a throwaway `zzE2E_user_<suffix>` account — never
`admin` itself. Follow this same rule for any new admin-users spec.

---

## 4. Cleanup mechanics — simpler than a database-client-based suite

Cypress's `cy.request()` automatically carries the browser's current
session cookies (the `jwt`/`refreshToken` HttpOnly cookies this app's login
sets) for requests to the same origin as `baseUrl` — so cleanup is just a
plain authenticated `cy.request({ method: 'DELETE', url: ... })` call, no
Node-side task or separate authenticated client needed (unlike a suite
built against a client SDK that needs its own service-role-equivalent
connection). `failOnStatusCode: false` on cleanup requests means an
already-deleted resource (the common case, since the in-test UI delete
usually already ran) doesn't fail the `after()` hook.

---

## 5. Why sequential, not parallel

This suite shares **one** dev server, **one** running backend, and **one**
`admin` session across every spec file — running files concurrently would
race one file's setup/cleanup against another's assertions against the
same account. `cypress run` already executes spec files sequentially
within a single invocation by default (Cypress Cloud's `--record
--parallel` would change that, but it isn't configured here) — no extra
config needed for this guarantee.

Within a single spec file, tests run in declaration order and **share
state deliberately** — e.g. `albums.cy.ts`'s "open album" test operates on
the album the first test created, and its "delete" test removes that same
album. This mirrors a real user's session more than fully-isolated tests
would, and `cy.login()`'s `cy.session()` caching means only the *first*
login in a run actually drives the login form — every subsequent call,
even across spec files, restores the cached session instead.

---

## 6. Async assertions — two Cypress-specific gotchas

- **`cy.get(selector).then(callback)` does not retry** — it snapshots the
  DOM once, immediately, and hands it to `callback`. For anything that
  needs to wait on async content (a page mid-fetch on first load), use a
  function-bound `cy.get(selector).should(($el) => { ... })` instead —
  only `.should()`'s callback form retries until the assertion inside it
  passes or the command times out.
- **`cy.get(selector).contains(text).should('not.exist')` throws instead
  of asserting "not found," if `selector` itself matches zero elements** —
  Cypress needs `.contains()` to have something to search *within*. Use
  `cy.contains(selector, text).should('not.exist')` (contains as the
  primary command, selector as its first argument) for "this text is
  nowhere inside these elements" checks.

---

## 7. Adding a new spec

1. Follow an existing spec file's shape: a `beforeEach()` calls
   `cy.login()` (and usually `cy.visit()` the relevant route), an `after()`
   captures a safety-net cleanup for anything not already deleted via the
   real UI in-test.
2. Name every piece of test data `` `${E2E_PREFIX} <description>
   ${uniqueSuffix()}` `` (or `${E2E_PREFIX}_<description>_${uniqueSuffix()}`
   for anything that can't contain spaces, like a username).
3. **Read the target component's actual `.html` template before writing
   selectors** — every selector in this suite's existing specs
   (`formControlName`s, button `title`/text, table row/cell classes) was
   confirmed against the real template, not guessed. This app has no
   translation-key layer (UI text is hardcoded English directly in
   templates), so matching on visible text is safe and stable here, unlike
   apps with an i18n pipe.
4. **A `mat-form-field` whose control has a `required` validator needs
   `{ force: true }` on `.type()`.** Angular Material renders an
   `aria-hidden` `*` marker inside the label for a required field, and that
   marker geometrically overlaps a resting (unfocused) input closely enough
   to trip Cypress's actionability check — even though a real click/type
   passes through it fine. Confirmed empirically on `user-admin.component`'s
   inline password-change field (`New Password*`) — its `.type()` hung with
   "element cannot be interacted with" until `{ force: true }` was added.
   Fields without a visible `*` (e.g. the login form's `Username`/`Password`,
   the add-user form's own fields) don't need this. When a new field starts
   failing the same way, check whether its label renders a `*` before
   assuming the app itself is broken.
5. Check whether the action you're testing opens a `ConfirmDialogComponent`
   (`shared/components/confirm-dialog/`) — some destructive actions do
   (`admin-users`' delete-user, `profile-sessions`' sign-out-everywhere-else)
   and some don't (`albums`' delete-album, `profile-sessions`' single-session
   revoke go straight through). Grep the feature's `.component.ts` for
   `ConfirmDialogComponent`/`MatDialog` rather than assuming either way — the
   dialog's own `confirmLabel` varies per call site (`'Delete'` default,
   but `profile-sessions` overrides it to `'Sign out'`).
6. Run the new spec in isolation (`npx cypress run --e2e --spec
   "cypress/e2e/your-new-spec.cy.ts"`, backend + dev server already
   running) repeatedly (3+ times) before considering it done — a flaky spec
   that happens to pass once is worse than an honest failure.
7. Run the full suite (`npm run test:e2e`) once your new spec passes in
   isolation, to confirm it doesn't interact badly with specs that run
   before/after it (shared `admin` session, shared backend state).
8. When a **new route** ships (not just a new spec for an existing one),
   run `npm run route-coverage:report` from `frontend/` afterward — it
   cross-references every path in `app.routes.ts` against every literal
   `cy.visit('/path')` (real tier) or `visitWithSession('/path')` (mocked
   tier, `cypress/support/mocked/seed-session.ts`) across both E2E tiers,
   writing a dated route → tier-coverage table to
   `docs/reports/route-coverage/`. Confirms the new route actually picked
   up a visit somewhere rather than being added to `app.routes.ts` and
   never getting E2E coverage at all.

---

## 8. What this suite deliberately does not cover (yet)

- **Catalog-dependent features** — gallery, sync, convert, duplicates,
  analytics, recycle-bin. All of these depend on `CatalogAssetsUseCase`
  actually scanning real files from disk; there is no "create a fake asset
  row" endpoint, so covering them needs a dedicated fixture-image-folder
  design (checked-in sample images + a catalog run pointed at them,
  cleaned up via `DELETE /api/assets` afterward) that hasn't been built
  yet. Worth adding as a follow-up, not attempted in this suite's first
  pass.
- **True concurrent-session testing** — `profile-sessions.cy.ts` can only
  verify the current session's own flag/revoke-guard and that "sign out
  everywhere else" doesn't break it, not that a *second*, genuinely
  concurrent session actually gets revoked — a single Cypress cookie jar
  can't hold two live sessions at once to check that.
- **CI integration for *this* suite specifically** — this suite (the
  real-backend one) is not wired into `.github/workflows/web-test.yml`, and
  that's a deliberate decision, not an oversight: it needs Postgres/Mongo/
  Redis/Kafka plus a running backend, which is heavier than CI should carry
  on every push. Run it manually (`npm run test:e2e`) before a release or
  after a change that touches multiple features at once. `gitflow`'s
  finish-release/finish-hotfix actions already gate on this suite passing
  (see that skill). CI is not *entirely* without browser-level E2E signal,
  though — see §9: the mocked tier runs on every push/PR, just at
  golden-path smoke depth rather than this suite's full coverage.

---

## 9. The mocked E2E smoke tier — a different, CI-safe layer

`cypress/e2e/mocked/` is a sibling E2E layer, not part of this suite — it
exists specifically to close the gap §8 describes (this suite's deliberate
absence from CI), without changing this suite's own scope or config at
all.

- **Directory**: `cypress/e2e/mocked/` (12 spec files at the time of
  writing: `auth-guard`, `login`, `home`, `gallery`, `sync`, `convert`,
  `duplicates`, `albums`, `admin-users`, `profile-sessions`, `analytics`,
  `recycle-bin`).
- **Its own config**: `cypress.mocked.config.ts` — a separate file from
  `cypress.config.ts`, not a `specPattern`/`excludeSpecPattern` split on
  the same config: `excludeSpecPattern` is applied *before* spec selection, so
  putting the exclude on the real-suite config would also block a targeted
  `--spec cypress/e2e/mocked/**` invocation against that same config.
- **Command**: `npm run test:e2e:mocked` — `start-server-and-test start
  http://localhost:4200 cy:e2e:mocked:run`, which *does* start `ng serve`
  itself (unlike this suite's own `npm run test:e2e`) since it needs no
  running backend to be meaningful.
- **How it works**: no real login, no running backend at all.
  `cypress/support/mocked/seed-session.ts`'s `visitWithSession()` writes a
  `photomanager_session` key directly into `localStorage` before the app
  bootstraps, so the auth guard sees a logged-in user with zero real
  requests — every `/api/**` call the visited page makes is stubbed via
  `cy.intercept`.
- **Scope boundary vs. this suite**: golden-path smoke only — an
  auth-guard redirect check, plus one render assertion and one
  representative CRUD interaction per major route. It is not a replacement
  for this suite's full CRUD/business-rule depth, and never will be; that
  depth stays here, deliberately out of CI, per §8.
- **When to touch it**: add a new mocked spec (or extend an existing one)
  when a new major route ships, mirroring the "one render + one CRUD
  interaction" shape of the existing files — not full coverage of that
  route's business rules, which belongs in this suite instead.
