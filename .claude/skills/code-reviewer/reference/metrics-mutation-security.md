# Code Reviewer — Mutation Testing, Secrets, Licenses & Dependency Vulnerabilities

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 24. Mutation Testing (both sub-projects)

Line coverage (§19) only proves a line *executed* during the test suite —
it says nothing about whether a test actually *asserts* on that line's
behavior. Mutation testing closes that gap: a tool systematically changes
("mutates") small pieces of the code under test — flips a `>` to `>=`,
negates a condition, swaps a boolean literal — reruns the tests, and checks
whether anything failed. A "killed" mutant means some test caught the
change; a "survived" mutant means the whole suite still passed with the
code's behavior altered, i.e. that line has execution coverage but no real
assertion behind it. Unlike §18/§19/§21/§22, this is not wired as a
pass/fail gate anywhere (no CI job, no `npm run *:check` equivalent) — it's
a trending snapshot only, run on demand, the same way §18.4's complexity
trend report and the dead-code/dependency-staleness reports are.

### 24.1 Frontend (StrykerJS)

Run from `frontend/`:

```
npm run mutation:report
```

(`scripts/mutation-report.js`, driven by `stryker.conf.mjs`). Cypress
Component Testing has no native Stryker test-runner plugin (only
Jest/Mocha/Karma/Jasmine/Vitest are supported that way), so this uses
StrykerJS's generic **command** runner — and the config carries two
non-obvious things worth knowing before touching it:

- **The browser/`process` bridge.** The command runner's mutant-switch
  instrumentation reads `process.env.__STRYKER_ACTIVE_MUTANT__` to decide
  which mutant is "active," but the code under test runs inside a Cypress
  *browser* iframe, which has no Node `process` global. Without a bridge,
  every mutant silently "survives" — the switch never activates, the
  original code always runs, and the mutation score is permanently stuck
  at 0% regardless of test quality (confirmed empirically while building
  this: an identical mutation applied by hand and run directly correctly
  failed its test, while the same mutation via `npx stryker run` did not).
  The fix lives in `cypress.config.ts`'s component `setupNodeEvents`
  (forwards the env var into `config.env`) and
  `cypress/support/component.ts` (reads it via `Cypress.env(...)` and
  assigns `globalThis.process` before any spec file's own imports
  evaluate — ES module evaluation order guarantees the support file's
  top-level code runs first). If a mutation score is ever suspiciously and
  uniformly ~0% again, check that bridge before assuming the tests
  themselves are weak.
- **Narrow, curated scope.** `stryker.conf.mjs`'s `mutate` list is a
  hand-picked ~8 files under `core/` with genuine branching logic
  (services/interceptors with real conditionals — not thin HTTP CRUD
  wrappers), and `commandRunner.command` runs a scoped
  `cypress run --component --spec` (`npm run test:mutation`) covering only
  those files' own specs, not the full ~650-test suite. The command runner
  reruns the whole configured command per mutant — Stryker's live
  "remaining time" estimate is unreliable in the first few percent (it
  initially extrapolates from the slow first mutant, which pays a
  one-time Electron/npx warm-up cost the rest don't), so don't judge
  whether a run is worth letting finish from an early ETA: the full
  8-file scope (553 mutants) completed in under 14 minutes end to end at
  `concurrency: 1`, despite briefly showing an in-run estimate north of 8
  hours. When reviewing a scope this narrow, treat the resulting score as
  a sample of core/'s branchiest logic, not a whole-frontend figure —
  extend `mutate` to a changed file directly (temporarily, for a scoped
  review) rather than trusting the existing list to already cover it.

Writes a dated snapshot to `docs/reports/mutation/` — overall mutation
score, killed/survived/timeout/no-coverage counts, a per-file table sorted
worst-first, and a full list of surviving/uncovered mutants (file, line,
mutator, replacement).

### 24.2 Backend (PIT)

Run from `backend/`:

```
bash scripts/mutation-report.sh
```

Invokes `org.pitest:pitest-maven` (`backend/pom.xml`) via
`mvn org.pitest:pitest-maven:mutationCoverage@mutation-report` — a
report-only execution bound to `phase>none<`, mirroring
`maven-pmd-plugin`'s `complexity-report` execution (§18.2/§18.4), so it
never runs during `mvn test`/`verify`/`package`. Unlike the frontend, PIT
runs entirely inside the JVM test process alongside the real JUnit
suite — no browser boundary to bridge, and far faster than the frontend's
per-mutant-process command-runner approach for a comparable mutant count
(the full `application.usecase` scope — 65 classes, 443 mutants — runs in
about 4 minutes). `targetClasses`/`targetTests` are scoped to
`com.jpablodrexler.photomanager.application.usecase.*` — the actual
business-logic layer; `infrastructure.web`/`infrastructure.persistence`
are thin translation code not worth mutating.

Writes a dated snapshot to `docs/reports/mutation/` — PIT's own Line
Coverage / Mutation Coverage / Test Strength percentages, killed/survived/
no-coverage/timed-out counts, a per-class table sorted worst-first, and a
full list of surviving/uncovered mutants (class, method, line, mutator).

### 24.3 Flagging

🟡 Flag a survived mutant on a line the reviewed change touched — the test
suite runs and passes even though that line's behavior changed, meaning
whatever test covers it isn't actually asserting on the behavior a real
bug there would break. Fix by strengthening the existing test's
assertions (not by adding a redundant new test) so it would fail against
the mutant's changed behavior; re-run the relevant report to confirm.

🟡 Flag a "no coverage" mutant on a line the reviewed change touched — no
test reaches that line at all, a stronger gap than a survived mutant (that
line has neither execution coverage nor an assertion), and often a
`java-unit-test-developer`/`cypress-unit-test-developer` gap worth cross-
referencing against §19's coverage report for the same file.

🟢 Don't chase every survived mutant project-wide in a scoped review —
both reports' scope is already narrow (frontend: a curated ~8-file list;
backend: one architectural layer), so treat every surviving mutant inside
that scope as worth a look, but don't expand the mutation-testing scope
itself as part of an unrelated review.

---

## 25. Secrets Scanning, License Compliance, and Dependency Vulnerabilities (frontend)

Three more report-only metrics, all frontend-scoped tooling but scanning
beyond `frontend/` where relevant:

**Secrets scanning** — `npm run secrets:report`
(`frontend/scripts/secrets-scan-report.js`) scans the whole repository —
backend Java source, k8s manifests, docs, everything — not just
`frontend/` — with [secretlint](https://github.com/secretlint/secretlint)
for accidentally committed API keys, private keys, tokens, and other
high-confidence secret patterns. Uses secretlint's `unix` output formatter
deliberately, not `json` — the json formatter dumps every scanned file's
full unmasked content alongside any findings, both slow and a real
exposure risk if that output is ever mishandled. Every credential-shaped
file in this repo (`JPPhotoManagerWeb/.env`, `JPPhotoManagerWeb/k8s/secret.yaml`,
`JPPhotoManagerWeb/k8s/catalog-volumes.yaml`) is excluded via
`.secretlintignore` at the repo root, on top of secretlint's own
`.gitignore` respect. Writes a dated snapshot to
`docs/reports/secrets-scan/` — file, line, rule, and message per finding.

**License compliance** — `npm run license:report`
(`frontend/scripts/license-compliance-report.js`, via
[license-checker-rseidelsohn](https://github.com/RSeidelsohn/license-checker-rseidelsohn))
checks every resolved npm dependency's declared license, flagging the
copyleft family (GPL/AGPL/LGPL/SSPL/EUPL/CC-BY-SA/OSL/CPAL) and anything
unresolvable — this repo is public, so an unnoticed copyleft dependency is
a real concern. The backend equivalent is `bash backend/scripts/license-report.sh`
(license-maven-plugin's `add-third-party` goal, invoked ad hoc via its full
plugin coordinate — no `pom.xml` changes needed). Writes a dated snapshot
to `docs/reports/license-compliance/`.

**Dependency vulnerabilities (SCA)** — `npm run sca:report`
(`frontend/scripts/sca-report.js`, via `npm audit`) checks every resolved
npm dependency against npm's own advisory database. The backend equivalent
is `bash backend/scripts/sca-report.sh`, which queries
[OSV.dev](https://osv.dev)'s public batch API against the resolved Maven
dependency tree — chosen over OWASP Dependency-Check specifically because
Dependency-Check's first run downloads the full NVD database, rate-limited
to a multi-hour pull without a personally-requested API key. Both write a
dated snapshot to `docs/reports/dependency-vulnerabilities/`.

🔴 Any secrets-scan finding that's a real, live secret is a **stop what
you're doing** situation — rotate/revoke it immediately (git history still
has it even after removal from the working tree).

🔴 Flag any **critical/high** SCA finding with a fix available and no clear
reason it hasn't been applied.

🟡 Flag a copyleft or unknown license on a **runtime** dependency (Maven
compile scope / npm `dependencies`, not devDependencies or test scope) —
most flagged packages will be build/test tooling never distributed with
the app, which doesn't need the same scrutiny.

🟡 Flag a critical/high SCA finding with no fix available yet — worth
checking whether the vulnerable code path is actually reachable (many
advisories are in build tooling, never shipped or executed against
untrusted input) before treating it as urgent.

🟢 A secrets-scan false positive, an `UNKNOWN` license, or a moderate/low
SCA finding is worth a passing mention, not a blocker.
