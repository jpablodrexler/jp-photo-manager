# Cypress Unit Test Developer — Architecture, File Naming/Location & Naming Conventions

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 1. Architecture (already set up — read this for context, not setup steps)

Cypress is a `JPPhotoManagerWeb/frontend/` devDependency; `cypress.config.ts`
(that directory's root) already configures both the `component` and `e2e`
blocks (the `e2e` block belongs to the `e2e-suite` skill, not this one).
Nothing below needs to be installed or scaffolded — it already exists.
Four things about the setup are worth understanding before writing a new
test, because they explain why the conventions in this skill exist:

1. **Bundler is `webpack`, not an esbuild/`application` bundler.** Cypress
   15's Angular Component Testing preset only supports
   `devServer: { framework: 'angular', bundler: 'webpack' }` — there is no
   esbuild equivalent yet, unlike the app's own production build
   (`@angular/build:application`). This is why `@angular-devkit/build-angular`
   is a devDependency even though the app itself never uses it for
   building/serving — it exists solely to satisfy this preset.
2. **`cy.mount` comes from plain `cypress/angular`, not
   `cypress/angular-zoneless`**, even though this app itself is zoneless
   (`app.config.ts` calls `provideZonelessChangeDetection()`).
   `cypress/support/component.ts` already wires the correct one up as the
   global `cy.mount` command — never import `mount` directly in a test
   file, just call `cy.mount(...)`.
3. **Code coverage is already wired**: `component.devServer.webpackConfig`
   adds a post-loader `babel-loader` rule applying `babel-plugin-istanbul`
   to every bundled `.ts`/`.js` file (excluding `*.cy.ts` specs and
   `node_modules`), and `setupNodeEvents` registers
   `@cypress/code-coverage/task`. `npm run test` runs the plain suite; `npm
   run test:coverage` (`cypress run --component --env coverage=true`) runs
   the same specs and additionally writes `html`/`lcov`/text-summary
   reports to `coverage/` (gitignored); `npm run coverage:check` (`nyc
   check-coverage`, reading `.nycrc.json`) enforces thresholds afterward. A
   scope under 80% is a 🟡 finding per `code-reviewer` skill §19.1 — add the
   missing test cases for the uncovered lines/branches the report lists,
   then re-run `coverage:check` to confirm it clears 80%.
4. **Trending snapshot**: `npm run coverage:trend-report`
   (`scripts/code-coverage-report.js`) re-runs the suite itself and writes
   a dated markdown report to `docs/reports/code-coverage/` — the
   project-wide percentages plus a table of every file still below 80% on
   any metric. Useful after a batch of new/expanded test files (like this
   skill produces) to get a written record of the before/after numbers,
   rather than re-deriving them from a raw `nyc report` run each time.

If you ever need to touch this setup (rare), it lives in
`JPPhotoManagerWeb/frontend/cypress.config.ts`,
`JPPhotoManagerWeb/frontend/cypress/support/component.ts`, and
`JPPhotoManagerWeb/frontend/cypress/tsconfig.json`. There is no dedicated
`tsconfig.cy.json` here — `tsconfig.app.json`
doesn't `exclude` `*.cy.ts` files in this project, so no override was ever
needed.

---

## 2. File Naming and Location

| Rule                                        | Example                                                 |
| ------------------------------------------- | ------------------------------------------------------- |
| Test file sits **next to** the source file  | `gallery.component.cy.ts` beside `gallery.component.ts` |
| File extension is always `.cy.ts`           | `file-size.pipe.cy.ts`                                  |
| Spec pattern configured as `src/**/*.cy.ts` | Matches all feature/shared/core subdirectories          |

---

## 3. Naming Conventions

| Element     | Convention                          | Example                                 |
| ----------- | ----------------------------------- | --------------------------------------- |
| Test suite  | `describe('<ClassName>')`           | `describe('GalleryComponent')`          |
| Test case   | `it('should <expected behaviour>')` | `it('should display asset thumbnails')` |
| Alias       | `cy.get(...).as('alias')`           | `.as('assetGrid')`                      |
| Stub object | `<ServiceName>Stub`                 | `assetServiceStub`                      |

---

