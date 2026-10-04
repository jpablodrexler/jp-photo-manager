# Code Reviewer — Cyclomatic Complexity & Code Coverage

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 18. Cyclomatic Complexity (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a McCabe cyclomatic
complexity pass, in addition to the manual checklists above. Complexity is
measured per method/function (each starts at 1; +1 for each `if`, ternary,
loop, `catch`/switch-`case`, and short-circuit operator — `&&`, `||`, `??`
on the frontend; PMD's equivalent counting on the backend). **Max allowed
complexity is 15 per method/function**, in both sub-projects.

Don't count this by hand — each sub-project has its own checked-in analyzer.

### 18.1 Frontend (TypeScript)

Run from `frontend/`:

```
npm run complexity
```

(equivalent to `node scripts/cyclomatic-complexity.js src/app`, which walks
every non-`.cy.ts` `.ts` file under a given directory via the TypeScript
compiler API — see `frontend/scripts/cyclomatic-complexity.js`, same
decision-point rules as ESLint's built-in `complexity` rule). It exits
non-zero and lists every offending function (file, line, name, complexity)
when anything exceeds the threshold. For a scoped review, either run it
against the whole tree and filter the output to the changed files, or pass a
narrower directory directly, e.g.
`node scripts/cyclomatic-complexity.js src/app/features/gallery`.

### 18.2 Backend (Java)

Run from `backend/`:

```
mvn pmd:check
```

This invokes the `maven-pmd-plugin` (configured in `backend/pom.xml`,
version 3.28.0, bundling PMD 7.17.0) against `backend/pmd-complexity-ruleset.xml`,
which enables only PMD's built-in `CyclomaticComplexity` rule with
`methodReportLevel` set to 16 (PMD reports a violation when complexity is
**greater than or equal to** the configured level, so 16 is what flags
"over 15") and `classReportLevel` effectively disabled — this check is
scoped to individual methods, not a class's combined total. The plugin is
declared with no `<executions>` binding, so it never runs as part of the
normal build/test/CI lifecycle (`mvn verify`, `mvn package`, ...) — it's
opt-in, invoked only when this check is run, the same way the frontend's
`npm run complexity` isn't part of `npm run build`/`test`. `mvn pmd:check`
fails the command (non-zero exit) and writes `target/pmd.xml` when anything
exceeds the threshold — read that file, or the console output, for the
offending class/method/line.

### 18.3 Flagging

🟡 Flag any method/function reported over complexity 15 — this is a
maintainability problem (deep, hard-to-test branching), not a correctness
bug, so it's a WARNING rather than CRITICAL, but it should be fixed: extract
guard clauses, split the method/function by responsibility, or replace a
long `if`/`else if` chain with a lookup table/strategy map (backend:
consider a `switch` on an enum, a `Map<Key, Handler>`, or splitting the
use-case into smaller collaborators respecting §1.1/§1.2's port boundaries).
Note the reported complexity number and location in the finding so a fix
can be verified by re-running the relevant command.

🟢 A method/function in the 10–15 range is worth a passing mention if an
obvious, low-effort split exists, but isn't required to be flagged — the
threshold that matters is 15.

### 18.4 Trending snapshots

For a full-codebase sweep, both the pass/fail gates above have a trending
companion that ranks every function/method by complexity instead of only
flagging the ones over threshold — useful for spotting something climbing
toward 15 before it becomes an actual violation.

- **Frontend:** `npm run complexity:report` (`scripts/complexity-report.js`)
  — dated snapshot under `docs/reports/complexity/`, top 20 functions by
  complexity plus top 20 files by line count.
- **Backend:** `bash scripts/complexity-report.sh` (run from `backend/`) —
  same shape, under `JPPhotoManagerWeb/docs/reports/complexity/`. Uses a
  second, report-only `maven-pmd-plugin` execution
  (`pmd-complexity-report-ruleset.xml`, `methodReportLevel=1` so PMD
  reports every method instead of only violations) — never touches the
  real gate's `pmd-complexity-ruleset.xml` or its threshold.

---

## 19. Code Coverage (both sub-projects)

Every reviewed scope — backend, frontend, or both — gets a line-coverage
pass, in addition to the manual checklists above. **Minimum line coverage is
80%**, in both sub-projects, whether the check runs over the whole project
or is scoped to just the files a change touched.

Don't estimate this by eye — each sub-project already has a coverage tool
wired (`cypress-unit-test-developer` §1.3; `java-unit-test-developer` §1);
this section only adds the enforced threshold and the two ways to scope the
check.

### 19.1 Frontend (TypeScript)

Run from `frontend/` (collect coverage, then check the threshold):

```
npm run test:coverage
npm run coverage:check
```

`test:coverage` (`cypress run --component --env coverage=true`) re-runs the
component suite with `babel-plugin-istanbul` instrumentation active and
writes `html`/`lcov`/text-summary reports to `coverage/` (gitignored).
`coverage:check` (`nyc check-coverage`) reads the `lines`/`branches`/
`functions`/`statements` thresholds (80 each) from `.nycrc.json` and fails
(non-zero exit) if any falls short.

For a **scoped review** (a single component, service, or feature directory
rather than the whole frontend), don't rely on the whole-project number —
narrow the check with `--include`, which filters the already-collected
coverage map down to matching paths before the threshold is evaluated:

```
npx nyc check-coverage --include "src/app/features/albums/**" --lines 80 --branches 80 --functions 80 --statements 80
```

(`test:coverage` still needs to have run first — `--include` only filters
which already-collected files count toward the ratio, it doesn't limit
which specs execute.)

### 19.2 Backend (Java)

Run from `backend/` (populate `target/jacoco.exec`, then check the threshold):

```
mvn test
mvn jacoco:check
```

This invokes the `jacoco-maven-plugin` (configured in `backend/pom.xml`)
against its default rule — a `BUNDLE`-level `LINE` `COVEREDRATIO` minimum
of `${jacoco.check.minimum}` (80%) — reading the exec data `mvn test`
already produced via the plugin's existing `prepare-agent` execution. Like
`mvn pmd:check` (§18.2), the `check` goal has no `<executions>` binding in
the pom, so it never runs as part of the normal build/test lifecycle
(`mvn test`, `mvn verify`, `mvn package`) — it's opt-in, invoked only when
this check is run. `mvn jacoco:check` fails the command (non-zero exit) and
prints the offending counter/ratio when coverage is under threshold;
`target/site/jacoco/index.html` (from the existing `report` execution)
shows the breakdown per package/class.

For a **scoped review**, override `jacoco.check.includes` (default `**/*`,
the whole project) to the package(s) the change touched, so the ratio is
computed only over those classes instead of the whole backend:

```
mvn jacoco:check -Djacoco.check.includes=com/jpablodrexler/photomanager/application/usecase/album/**
```

### 19.3 Flagging

🟡 Flag any coverage run — whole-project or scoped to the reviewed change —
that reports under 80% line coverage. This is a test-adequacy problem, not
a correctness bug, so it's a WARNING rather than CRITICAL, matching how
§18's complexity threshold is treated — but it should be fixed before the
review is considered clean: add the missing test cases for the uncovered
lines/branches the report lists (`java-unit-test-developer` for backend
gaps, `cypress-unit-test-developer` for frontend gaps), then re-run the
check to confirm it now clears 80%.

🟢 A scope in the 75–80% range is worth a passing mention if the gap is a
small, easily-covered handful of lines, but isn't required to be flagged —
the threshold that matters is 80%.

### 19.4 Trending snapshots

For a full-codebase sweep, both `npm run coverage:trend-report`
(`scripts/code-coverage-report.js`, frontend) and `bash
scripts/coverage-report.sh` (run from `backend/`, backend) wrap the same
suite runs the gates above use into a dated snapshot under
`docs/reports/code-coverage/`/`JPPhotoManagerWeb/docs/reports/code-coverage/`
— the project-wide percentages plus a table of every file/class still
below 80%. Unlike `coverage:check`/`jacoco:check`, these do not fail the
command; they exist purely to leave a written record of the actual number
over time, so a file that quietly backslid is visible even while the
project-wide number stays above threshold. The backend script parses
`target/site/jacoco/jacoco.xml` directly (via an inline Perl snippet, since
the file is single-line, deeply-nested XML that plain `grep`/`awk` cannot
reliably disambiguate between method/class/package/report-level counters
sharing the same tag name) rather than adding a second JaCoCo plugin
execution the way §18.4's backend complexity report needed — JaCoCo's
existing `report` execution (bound to the `test` phase) already produces
everything this needs.

---
