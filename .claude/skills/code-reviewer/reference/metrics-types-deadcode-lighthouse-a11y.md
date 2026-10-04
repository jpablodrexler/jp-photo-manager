# Code Reviewer — Type Coverage, Dead Code, Lighthouse & Accessibility Audits

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 20. Type Coverage (frontend)

Every reviewed frontend scope also gets a type-coverage pass — how much of
the TypeScript identifier surface has a real, non-`any` type, as opposed
to `any` reached via an explicit annotation, an untyped third-party return
value, or TypeScript inference giving up. This is the quantitative
backstop for §11's "no `any` unless unavoidable" convention: that rule
catches an `any` a reviewer happens to read past, this catches one that
slipped through review entirely.

Run from `frontend/`:

```
npx type-coverage --project tsconfig.app.json --detail
```

(or `npm run type-coverage:report` for a dated snapshot under
`docs/reports/type-coverage/`, listing every uncovered identifier grouped
by file — better for a full-codebase sweep than a scoped review).

🟡 Flag any identifier the tool reports as untyped that isn't already
caught by §11's manual `any` check. Same severity as that rule, not a
separate threshold-based gate — a single untyped identifier is exactly as
fixable regardless of the project-wide percentage.

🟢 Don't chase the last fraction of a percent in files that are
overwhelmingly typed already — note the project-wide percentage but only
flag specific uncovered identifiers actually touched by the reviewed
change (or, for a full sweep, the files with the most per the report's
file-grouped table).

---

## 21. Dead Code

Every full-codebase sweep also gets an unused-code pass — the automated
counterpart to whatever "reuse/simplification" findings a manual read
would catch, just extended to catch what a single-file read cannot: an
export or dependency nothing references *anywhere else* in the codebase.

### 21.1 Frontend (knip)

Run from `frontend/`:

```
npx knip
```

(or `npm run dead-code:report` for a dated snapshot under
`docs/reports/dead-code/`, split into unused files/exports/types/
dependencies/unlisted-dependencies). Configured in `frontend/knip.jsonc` —
see that file's comments for why Cypress config/spec files and a handful
of name-resolved devDependencies (`@angular-devkit/build-angular`,
`babel-plugin-istanbul`) need explicit entries/ignores before the tool's
findings are trustworthy on this Angular + Cypress project.

### 21.2 Backend (Maven)

Run from `backend/`:

```
mvn dependency:analyze
```

(or `bash scripts/dead-code-report.sh` for a dated snapshot under
`JPPhotoManagerWeb/docs/reports/dead-code/`). Maven's own built-in
unused/undeclared-dependency detector — the closest backend equivalent to
knip, though narrower in scope: it only covers dependencies, not unused
application-source exports/classes, since Maven has no direct analog to
knip's source-level dead-code detection.

**Known, expected noise:** every `spring-boot-starter-*` "umbrella"
dependency is reported as "unused declared" because `dependency:analyze`
works by scanning compiled bytecode for direct class references, and a
starter POM has no classes of its own — it exists purely to pull in a
bundle of real dependencies transitively. This is a well-documented
limitation of bytecode-based analysis for Spring Boot specifically, not a
real finding — skim past every `spring-boot-starter-*` entry and focus on
anything else in the "Unused declared dependencies" list.

### 21.3 Flagging

🟡 Flag an unused file, export, or type (frontend) — either it should be
made module-private or, if nothing in the codebase needs it anymore,
deleted outright per this project's own "no half-finished implementations"
convention.

🟡 Flag an unused declared dependency (either sub-project, excluding
§21.2's Spring Boot starter noise) — dead weight in the build and a wider
(if unused) attack surface for `security-reviewer` §1 to worry about.

🟢 Flag an `unlisted` dependency (frontend: imported but only present
transitively; backend: `dependency:analyze`'s "Used undeclared
dependencies") as a suggestion — it works today only because some other
direct dependency happens to pull it in, fragile across dependency-tree
changes.

---

## 22. Performance & Accessibility (Lighthouse, frontend)

Scoped narrowly today: `npm run lighthouse:report` (self-builds the
production bundle and audits it — see
`frontend/scripts/lighthouse-report.mjs`) only covers `/login`, the one
route reachable without an authenticated session (every other route is
behind `authGuard`, and there is no self-registration flow to also cover —
accounts are admin-created via `/admin/users`). There is no equivalent to
the mocked E2E tier's session-fabrication trick for Lighthouse, so this
does not run against any authenticated route yet.

Run from `frontend/`:

```
npm run lighthouse:report
```

Writes a dated snapshot to `docs/reports/lighthouse/` — Performance,
Accessibility, Best Practices, and SEO scores (0–100), plus every failed
accessibility audit by name.

🟡 Flag any accessibility audit failure on a route touched by the reviewed
change — Angular Material does not guarantee WCAG compliance for free,
and this is the only automated a11y signal this project has.

🟢 A performance/SEO score regression is worth a passing mention (note the
before/after numbers if both are available) but isn't a hard gate the way
§18/§19's thresholds are — there is no established baseline yet for either
score.

---

## 23. Deep Accessibility Audit (cypress-axe, frontend)

§22's Lighthouse accessibility score only ever covers `/login` — the one
route reachable without an authenticated session — and even there it's a
single aggregate number, not the actual rule that failed. `npm run
a11y:report` (`frontend/scripts/a11y-report.js`) is the deep, per-route
complement: it drives `cypress-axe` (axe-core through Cypress) against every
authenticated route (`/home`, `/gallery`, `/sync`, `/convert`, `/duplicates`,
`/admin/users`, `/albums`, `/albums/:id`, `/recycle-bin`, `/analytics`,
`/profile/sessions`) using the same session-fabrication trick as the mocked
E2E smoke tier (`visitWithSession()` from
`cypress/support/mocked/seed-session.ts`, `cy.intercept`-stubbed `/api/**`
calls — no live backend needed).

Run from `frontend/` (needs the dev server reachable at
`http://localhost:4200` first, e.g. `npm start` in another terminal):

```
npm run a11y:report
```

The underlying spec (`cypress/e2e/a11y/a11y-audit.cy.ts`, driven by its own
`cypress.a11y.config.ts`) lives outside both existing Cypress tiers —
excluded from `cypress.config.ts`'s `e2e.excludeSpecPattern` and never
matched by `cypress.mocked.config.ts`'s narrower `specPattern` — so it never
runs as part of `npm run test:e2e` or `npm run test:e2e:mocked`. It calls
`cy.checkA11y(..., skipFailures: true)`, so a real violation is recorded as a
finding, not a failed test; the report script also tolerates a non-zero
Cypress exit code rather than crashing.

Writes a dated snapshot to `docs/reports/a11y/` — total violations by
impact level (critical/serious/moderate/minor), then a per-route breakdown
of every violated WCAG rule (rule id, impact, affected selector(s), help
URL).

🔴 Flag any `critical` or `serious` impact violation on a route touched by
the reviewed change.

🟡 Flag any `moderate` impact violation on a route touched by the reviewed
change.

🟢 A `minor` impact violation, or any violation on a route the change didn't
touch, is worth a passing mention but isn't required to be flagged.

---
