---
name: quality-metrics
description: Runs every periodic quality-metric sweep (bash scripts/run-all-quality-reports.sh from JPPhotoManagerWeb/ — the frontend's npm report scripts plus the backend's bash report scripts — covering type coverage, complexity, dead code, route coverage, auth coverage, lighthouse, accessibility, code coverage, bundle size, dependency staleness, mocked E2E run, secrets scan, license compliance, dependency vulnerabilities, CLAUDE.md size, plus opt-in mutation testing and real-backend E2E) and reports trends by reading every committed historical report per category under JPPhotoManagerWeb/docs/reports/<category>/ — not just the immediately preceding one — and comparing the freshly generated value against that whole series. TRIGGER when the user asks for a quality metrics report, how the quality metrics are trending, to run/refresh the quality metrics, or similar — this is the quality-metrics counterpart to bugs-status/features-status, but for the report categories in the root README's Quality Metrics table rather than the backlog files.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.3"
---

Runs the full periodic quality-metric sweep and reports trends — each
category's newly generated number against **every** committed historical
value for that category, not just the one immediately before it. These 16
categories are committed to git specifically so this full-history
comparison is possible — a single previous-vs-current delta can't tell a
steady drift apart from ordinary day-to-day noise, which is the whole
reason the reports live in git instead of being regenerated and discarded
locally. The per-fix audit categories (`code-review/`, `security-review/`,
`spec-compliance/`, and any similar review-skill output that lands under
the same `docs/reports/` tree) are out of scope for this skill — they're
gitignored, scoped to a single change, and have no trend to compute.

**Input**: optional `--full` to also run the two sweeps
`run-all-quality-reports.sh` skips by default (mutation testing on both
sides, real-backend E2E on the frontend side) — see step 1. No input
required otherwise.

---

## Script

`scripts/trend_report.py` owns the full mechanical pipeline across all
~23 category/side series (16 category folders, 7 of which split into
independent `_frontend`/`_backend` series): globbing **every** committed
report file per category/side — old and freshly-generated alike, ordered
by the embedded `**Generated:**` timestamp (never filename/mtime) —
extracting each one's key metrics, including the two Maven-CLI-wrapped
backend categories (`dead-code`, `dependency-staleness`) that have no
clean summary line and need a best-effort line count instead, and
`auth-coverage`'s raw per-endpoint table, and classifying each series'
shape (Flat / Steadily improving|declining / Volatile / One-off / First
data point / Too little history) against its own higher-is-better/
lower-is-better direction. Run it **after** step 1's sweep has produced
fresh report files, and use its output for step 2 rather than
re-deriving deltas/shapes by hand:

```
python3 .claude/skills/quality-metrics/scripts/trend_report.py <repo-root> [--full]
```

Pass `--full` when the sweep also ran with `--with-mutation`/
`--with-e2e-real`, so the mutation/real-E2E categories are included. Add
`--json` for the raw per-category series if you need to reason about a
specific field. The script's shape classification is a fixed heuristic
(tolerance-banded flat detection, majority-sign-of-diffs for
"steadily") — it is a starting point for step 2's write-up, not a
verdict to parrot verbatim: sanity-check an ambiguous or borderline case
(e.g. a "One-off"/"Volatile" call on a small series, or a regression
that reads oddly against the category's own Notes) against the
underlying values before writing the prose. It never decides anything is
worth fixing, never judges a `permitAll()` count or a
spring-boot-starter-* dead-code line as good or bad on its own (both
need the human-judgment guardrails below) — that judgment, and all of
the Guardrails below, stay yours. See the script's own docstring/comments
(the `CATEGORIES` dict and each `ext_*` extractor) for exactly which
field pattern it pulls from which file/side — that table used to be
duplicated here as a manual step and is now the script's source of
truth, not this file's.

---

## Steps

### 1. Run the sweep

```
cd JPPhotoManagerWeb
bash scripts/run-all-quality-reports.sh
```

This runs both the frontend's `npm run reports:all` and the backend's
`bash scripts/run-all-quality-reports.sh` in sequence, and skips two
reports by default: mutation testing on both sides (a full PIT/Stryker
run per mutant — minutes, not seconds) and the frontend's real-backend
E2E tier (needs the full app already deployed to k8s — see the
`e2e-suite` skill §1 for that precondition). Budget several minutes for
the full run (the frontend build, Lighthouse, mocked E2E, and axe-core
are the slow steps on that side; JaCoCo/PMD/dependency-analyze runs add
more on the backend side); use a generous Bash timeout rather than letting
it get cut off, and prefer running it in the background and waiting for
completion over polling a fixed number of times.

If the user passed `--full` (or explicitly asked to include mutation
and/or the real-backend E2E check), re-run with:

```
bash scripts/run-all-quality-reports.sh --with-mutation --with-e2e-real
```

A failing individual report does not stop the run — the script prints an
`OK`/`FAILED`/`SKIP` summary for each half at the end. Note which
categories failed; their "current" value in step 2 is really last run's
data, not a fresh one, and the report should say so rather than silently
treating it as current.

**Don't mistake a long, noisy run for a hang.** The code-coverage and
mutation reports drive a full test suite (Cypress component tests on the
frontend, JUnit/PIT on the backend) and can print a great deal of
intermediate output — build warnings, per-spec results, framework
diagnostics — while genuinely still progressing rather than stuck. Don't
kill a run just because the console looks busy or repetitive; confirm a
step actually failed by checking the script's own final `OK`/`FAILED`/
`SKIP` summary, or whether the category's report file exists, rather than
by eyeballing console noise mid-run.

### 2. Display the trend report

One table per category group, headline metric(s) with a compact
**History** column (every value in the series, oldest→newest) plus the
most-recent delta, the shape from the script's output, and a trend marker;
secondary/breakdown numbers folded into a Notes column rather than their
own row (keeps this scannable across roughly 20 category/side series):

```
## Quality Metrics Trend Report — <today's date>

**Sweep:** bash scripts/run-all-quality-reports.sh[ --with-mutation --with-e2e-real] (commit <short hash>)
[If any category failed to refresh:] **Note:** <category> (<side>) failed this run — showing its last available snapshot, not a fresh one.

### Coverage & correctness

| Category | Headline | History (oldest→newest) | Latest Δ | Shape | Notes |
| --- | --- | --- | --- | --- | --- |
| Component coverage (frontend) | Branches | … | … | … | Lines/Fn/Stmt moved similarly; N files below 80% |
| Backend coverage (JaCoCo) | Lines | … | … | … | Branches/Methods moved similarly |
| Type coverage | Type coverage | … | … | … | Untyped count |
| Mutation (frontend) [only if run] | Score | … | … | … | k/t mutants killed |
| Mutation (backend, PIT) [only if run] | Mutation coverage | … | … | … | Line coverage / test strength |
| E2E (mocked) | Passed/Failed | … | … | … | Flaky count, duration |
| E2E (real) [only if run] | Passed/Failed | … | … | … | … |

### Code health

| Category | Headline | History (oldest→newest) | Latest Δ | Shape | Notes |
| --- | --- | --- | --- | --- | --- |
| Complexity (frontend) | Avg complexity | … | … | … | files scanned, avg size |
| Complexity (backend) | Avg complexity | … | … | … | files scanned, avg size |
| Dead code (frontend) | Total findings | … | … | … | breakdown |
| Dead code (backend) | Unused/undeclared dep lines | … | … | … | spring-boot-starter-* noise excluded |
| Bundle size | Initial bundle | … | … | … | budget status, total JS/dist |
| CLAUDE.md size | Lines (JPPhotoManagerWeb/CLAUDE.md) | … | … | … | Words/bytes moved similarly; root CLAUDE.md reported alongside, informational only |

### Security & dependencies

| Category | Headline | History (oldest→newest) | Latest Δ | Shape | Notes |
| --- | --- | --- | --- | --- | --- |
| Dependency vulnerabilities (frontend) | Vulnerable packages | … | … | … | severity breakdown |
| Dependency vulnerabilities (backend, OSV) | Known vulnerabilities | … | … | … | |
| Dependency staleness (frontend) | Outdated | … | … | … | major/minor/patch |
| Dependency staleness (backend) | Outdated (Maven) | … | … | … | |
| Secrets scan | Findings | … | … | … | |
| License compliance (frontend) | Flagged | … | … | … | |
| License compliance (backend) | Needs action | … | … | … | previously-accepted count |
| Route coverage | Uncovered routes | … | … | … | which routes |
| Auth coverage | Endpoints tracked | … | … | … | count on permitAll() |

### Accessibility & performance

| Category | Headline | History (oldest→newest) | Latest Δ | Shape | Notes |
| --- | --- | --- | --- | --- | --- |
| Lighthouse | Performance (per route) | … | … | … | Accessibility/Best Practices/SEO |
| Accessibility audit (a11y) | Total violations | … | … | … | impact breakdown |

### ⚠ Regressions

[List every metric that moved the wrong direction, most severe first — weight a "one-off" break from a flat series higher than a move within an already-volatile one. "None." if clean.]

### First-time / too little history

[List any category/side with fewer than 3 points total — say what IS known (the values that exist) without claiming a shape.]
```

### 3. Note next steps — never auto-commit

The freshly generated files are new uncommitted changes under the tracked
`JPPhotoManagerWeb/docs/reports/<category>/` folders — `git status`/
`git diff --stat` shows exactly which ones. Point this out and suggest
committing them (so the trend history in git actually grows), but
per this project's standing convention (`CLAUDE.md`, and every other
skill here), **never commit without being asked** — end the turn with the
trend report and let the user decide.

---

## Guardrails

- **Report only — never fix.** If a regression shows up (a new
  vulnerability, a coverage drop, a bundle-budget breach, an endpoint that
  quietly lost its explicit auth rule), report it; don't start editing
  source, tests, `SecurityConfig`, or the bug backlog in response. That's
  a separate, explicit follow-up (`bug-report`/`bug-fix` for a real
  defect, `dependency-upgrade` for stale/vulnerable packages).
- **Never fabricate a delta or a shape.** If a category/side has no
  history yet, say so plainly instead of inventing a "previous" value. If
  it has fewer than 3 points, report the values that exist but don't
  claim a "flat"/"volatile"/"steadily improving" shape from 1–2 points —
  that's a delta claim wearing a trend-shape costume.
- **Always read the full committed history, not just the latest file.**
  Comparing only the immediately preceding value defeats the entire point
  of committing these reports — it can't distinguish a steady multi-run
  drift from ordinary noise. The script globs every matching file per
  category/side, not just the newest — never narrow that yourself by only
  checking the latest file.
- **Order history by the embedded `**Generated:**` timestamp, never by
  filename or filesystem mtime.** Filenames don't sort correctly once a
  same-day rerun adds a `-2`/`-3` suffix (and most of these scripts don't
  even do that — they overwrite in place), and git does not preserve
  original mtimes — a clone or checkout stamps every file with the
  checkout time, silently scrambling any mtime-based ordering for anyone
  who didn't generate the whole history in one uninterrupted working copy.
  This is why the script orders by the embedded timestamp instead of
  either — never patch around a script bug by re-sorting its output by
  filename or mtime.
- **Two independent series share a folder for shared categories.**
  Complexity, dead code, code coverage, dependency staleness, license
  compliance, dependency vulnerabilities, and mutation each have a
  `_frontend`/`_backend` pair in the same `docs/reports/<category>/`
  directory — never merge them into one series or diff a frontend value
  against a backend one.
- **The backend's Maven-CLI-wrapped reports have no clean summary line.**
  `dead-code` and `dependency-staleness` on the backend side are raw
  `mvn dependency:analyze` / `mvn versions:display-dependency-updates`
  output — extracting a trend number means counting matching lines, which
  is inherently more approximate than the bold-line categories. Say so
  in the report rather than presenting a Maven-CLI-derived count with the
  same confidence as a clean `**Label:** value` extraction.
- **`spring-boot-starter-*` entries in the backend dead-code report are
  documented as expected noise, not findings** — don't count them toward
  a regression, and don't flag their continued presence as a "flat"
  series worth worrying about.
- **Never treat `permitAll()` on an endpoint as inherently a problem.**
  Auth-coverage's per-endpoint table records the governing Spring Security
  rule as a factual snapshot, not a pass/fail gate — login/refresh-style
  endpoints are correctly `permitAll()`. Track the count as informational
  and flag a *change* (a previously-restricted endpoint newly resolving to
  `permitAll()` or the `anyRequest` fallback) rather than treating the
  bare count as good or bad on its own.
- **Never commit the new report files.** Generate and report on them; only
  stage/commit if the user explicitly asks, same as every other skill in
  this repo.
- If `scripts/run-all-quality-reports.sh` itself fails to run at all (not
  just one sub-report), don't fabricate a trend report — surface the
  failure and stop.
