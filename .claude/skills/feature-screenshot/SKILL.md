---
name: feature-screenshot
description: >
  Shows what a feature/page/dialog currently looks like by driving it through
  Cypress against a real local stack and a real login — never Puppeteer or
  live browser automation (claude-in-chrome). TRIGGER when asked to "take a
  screenshot of X", "show me what X looks like", "what does the Y page/
  dialog look like right now" — as opposed to `e2e-testing` (verifying
  behaviour after a change) or `e2e-suite` (committed regression coverage).
  Reuses e2e-testing's `cy.login()`/`cy.session()` helpers; writes a
  throwaway spec directly under `JPPhotoManagerWeb/frontend/cypress/e2e/`
  (a `zz-scratch-` prefix, deleted after use) — `cypress.config.ts`'s
  `specPattern` rejects a spec outside that folder, so it can't live in the
  session scratchpad the way an ad-hoc check might otherwise.
metadata:
  scope: [JPPhotoManagerWeb]
---

# Feature Screenshot Skill

Produce a real screenshot of the running app — logged in, real seeded data —
without more setup than a screenshot actually needs.

**Relationship to `e2e-testing`**: same mechanism (scratch spec under
`cypress/e2e/`, `cy.login('admin', 'admin')`, the same interaction
pitfalls), different purpose — that skill verifies behaviour after a
change; this one only looks. Read `e2e-testing` §7's "Interaction
pitfalls" section before scripting any click/type this skill needs beyond
a plain `cy.visit()` + `cy.screenshot()`.

**Relationship to `e2e-suite`**: the scratch spec unavoidably lives in the
same directory as the maintained suite (`cypress.config.ts`'s
`specPattern` leaves no other option), but is never committed and is
deleted at the end of every run; a `zz-scratch-` filename prefix keeps it
visibly distinct from the suite's own real spec files while it briefly
exists.

---

## 1. Resolve the target

Map the request to a route — check `JPPhotoManagerWeb/frontend/src/app/app.routes.ts`
or `docs/frontend.md`'s routing table if the feature name doesn't obviously
imply one. If the request is a specific *state* rather than just "the
page" (a dialog open, a filter applied, a particular tab) — not just "the
app at that route" — read that component's `.html` template first to find
the right selector/button to reach it, the same rule `e2e-suite` enforces
for its own specs. Don't guess a dialog trigger's selector from the
feature name alone — `e2e-testing` §9's CSS Selector Reference covers the
common ones, but a feature-specific trigger still needs its own template
checked.

## 2. Prerequisites

- **Full local stack running** — PostgreSQL, MongoDB, Redis, Kafka, and the
  Spring Boot backend, per `e2e-testing` §1–2. A screenshot needs real
  catalogued data to look like anything; an empty database just shows an
  empty state, which usually isn't the point of the request.
- **Frontend**: reuse a dev server already running on `localhost:4200`;
  otherwise start it (`e2e-testing` §3) and remember that this skill
  started it, so teardown only stops what it started.
- Confirm `JPPhotoManagerWeb/frontend/node_modules/cypress` is present
  (`e2e-testing` §7) — same check that skill already runs.

## 3. Write the scratch spec

One spec file at
`JPPhotoManagerWeb/frontend/cypress/e2e/zz-scratch-feature-screenshot.cy.ts`
— **not** the session scratchpad. `cypress.config.ts`'s `e2e.specPattern`
is `cypress/e2e/**/*.cy.ts` (with `cypress/e2e/mocked/**`/`cypress/e2e/a11y/**`
excluded); a `--spec` pointing outside that pattern matches nothing and
Cypress reports "no spec files were found" even though the file exists on
disk — confirmed empirically, not a config to second-guess. Placing it in
`cypress/e2e/` directly (never the `mocked/`/`a11y/` subfolders) also means
the default `supportFile` (`cypress/support/e2e.ts`) resolves normally,
with no `--config supportFile=...` override needed.

Cover every screenshot requested in this turn as separate `it()` blocks in
one `describe()` — not one Cypress process per screenshot. `cy.login()`
wraps `cy.session()`, so only the *first* `it()` actually drives the login
form; every later one in the same run restores the cached session.

```typescript
// JPPhotoManagerWeb/frontend/cypress/e2e/zz-scratch-feature-screenshot.cy.ts
describe('ad-hoc: feature screenshots', () => {
  beforeEach(() => {
    cy.login('admin', 'admin');
  });

  it('home dashboard', () => {
    cy.visit('/home');
    cy.url().should('include', '/home');
    cy.get('.stat-value', { timeout: 10000 }).should('have.length.greaterThan', 0);
    cy.screenshot('home-dashboard');
  });

  it('about dialog', () => {
    cy.visit('/home');
    cy.get('button[aria-label="About"]').click(); // confirm real selector in the component's .html first
    cy.get('mat-dialog-container').should('be.visible');
    cy.screenshot('about-dialog');
  });
});
```

Run with:

```bash
cd JPPhotoManagerWeb/frontend
npx cypress run --e2e --spec "cypress/e2e/zz-scratch-feature-screenshot.cy.ts" \
  --config baseUrl=http://localhost:4200
```

**Before running, clear any stale entries under
`JPPhotoManagerWeb/frontend/cypress/screenshots/` left by earlier failed
runs.** Cypress's `trashAssetsBeforeRuns` step tries to recycle-bin the
whole folder before each run; a leftover subfolder with `--`/`()`/space-
heavy filenames (failure screenshots from an old failing spec) can make
Windows' trash step throw and the run abort — delete
`JPPhotoManagerWeb/frontend/cypress/screenshots/` outright first if this
happens (it's gitignored, regenerated every run, never source).

Apply every pitfall from `e2e-testing` §7's "Interaction pitfalls" section
for any click/type beyond a plain `cy.visit()` (a `mat-select` open
retried rather than clicked once, `mat-checkbox` clicked directly not its
native input, `{ force: true }` on a required/pre-filled field, etc.) — a
screenshot of a state that silently failed to open (a `mat-select` that
no-opped) is worse than no screenshot, since it looks correct at a glance.

**Viewport — defaults to desktop, no extra config needed.** Cypress's
default viewport (1000×660) already renders the desktop layout. Only add
an explicit `cy.viewport(390, 844)` (or `cy.viewport('iphone-x')`) when
the request specifically asks for a mobile/phone view, and call it out as
such in the summary so it's clear which viewport a given screenshot used.

## 4. Run, then show

Run the spec once (all `it()` blocks execute in the one process).
Screenshots land under
`JPPhotoManagerWeb/frontend/cypress/screenshots/<spec-name>/<name>.png` —
Read each one and present it.

## 5. Teardown

- Delete `JPPhotoManagerWeb/frontend/cypress/e2e/zz-scratch-feature-screenshot.cy.ts`.
- Delete the run's own output under
  `JPPhotoManagerWeb/frontend/cypress/screenshots/` after reading the
  images (it's gitignored and regenerated every run — leaving it around is
  exactly what caused the trash-step failure in §3).
- Stop the backend/frontend only if §2 started them — never kill a stack
  the user was already running.
- Nothing else to clean up: this skill only reads, it doesn't seed or
  mutate data (if a view genuinely needs catalogued assets to look
  populated, that's `e2e-testing`'s territory, not this skill's default
  path).

---

## 6. Checklist summary

- [ ] Target resolved to a route (and, if a specific state, the triggering
      selector confirmed against the component's actual `.html` template)
- [ ] Full local stack (Postgres/Mongo/Redis/Kafka/backend) confirmed
      running before the frontend was started
- [ ] Dev server reused or started (and only stopped in teardown if this
      skill started it)
- [ ] All requested screenshots batched into one spec/one Cypress run, not
      one process per screenshot
- [ ] Any click/type beyond a plain `cy.visit()` follows `e2e-testing`
      §7's "Interaction pitfalls"
- [ ] Viewport was left at the default (desktop) unless a mobile view was
      specifically requested, and the summary says which was used
- [ ] Screenshot(s) read and shown to the user
- [ ] Scratch spec deleted from `cypress/e2e/`, and `cypress/screenshots/`
      cleared after reading the images
