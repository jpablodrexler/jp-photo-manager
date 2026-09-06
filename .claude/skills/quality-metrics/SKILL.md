---
name: quality-metrics
description: Runs every periodic quality-metric sweep (bash scripts/run-all-quality-reports.sh from JPPhotoManagerWeb/ — the frontend's npm report scripts plus the backend's bash report scripts — covering type coverage, complexity, dead code, route coverage, auth coverage, lighthouse, accessibility, code coverage, bundle size, dependency staleness, mocked E2E run, secrets scan, license compliance, dependency vulnerabilities, plus opt-in mutation testing and real-backend E2E) and reports trends by reading every committed historical report per category under JPPhotoManagerWeb/docs/reports/<category>/ — not just the immediately preceding one — and comparing the freshly generated value against that whole series. TRIGGER when the user asks for a quality metrics report, how the quality metrics are trending, to run/refresh the quality metrics, or similar — this is the quality-metrics counterpart to bugs-status/features-status, but for the report categories in the root README's Quality Metrics table rather than the backlog files.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
---

Runs the full periodic quality-metric sweep and reports trends — each
category's newly generated number against **every** committed historical
value for that category, not just the one immediately before it. These 15
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
sides, real-backend E2E on the frontend side) — see step 2. No input
required otherwise.

---

## Steps

### 1. Read the full committed history — before running anything

For each category below, find **every** existing report file matching its
pattern under `JPPhotoManagerWeb/docs/reports/<category>/` — not just the
latest one. A category shared by both halves of the stack (complexity,
dead code, code coverage, dependency staleness, license compliance,
dependency vulnerabilities, mutation) writes two independent series in
the same folder, disambiguated by a `_frontend.md` / `_backend.md` suffix
— track those as two separate series, never merged into one.

| Category (folder under `JPPhotoManagerWeb/docs/reports/`) | Side(s) | File pattern |
| --- | --- | --- |
| type-coverage | frontend only | `TYPE_COVERAGE_REPORT_*.md` |
| complexity | both | `COMPLEXITY_REPORT_*_frontend.md` / `COMPLEXITY_REPORT_*_backend.md` |
| dead-code | both | `DEAD_CODE_REPORT_*_frontend.md` / `DEAD_CODE_REPORT_*_backend.md` |
| route-coverage | frontend only | `ROUTE_COVERAGE_REPORT_*_frontend.md` |
| auth-coverage | backend only | `AUTH_COVERAGE_REPORT_*_backend.md` |
| lighthouse | frontend only | `LIGHTHOUSE_REPORT_*_frontend.md` |
| a11y | frontend only | `A11Y_REPORT_*_frontend.md` |
| code-coverage | both | `CODE_COVERAGE_REPORT_*_frontend.md` / `CODE_COVERAGE_REPORT_*_backend.md` |
| bundle-size | frontend only | `BUNDLE_SIZE_REPORT_*.md` |
| dependency-staleness | both | `DEPENDENCY_STALENESS_REPORT_*_frontend.md` / `DEPENDENCY_STALENESS_REPORT_*_backend.md` |
| e2e-run (mocked) | frontend only | `E2E_RUN_REPORT_*_mocked.md` |
| e2e-run (real) — only if `--full` | frontend only | `E2E_RUN_REPORT_*_real.md` |
| mutation | both | `MUTATION_REPORT_*_frontend.md` / `MUTATION_REPORT_*_backend.md` (both only if `--full`) |
| secrets-scan | whole repo | `SECRETS_SCAN_REPORT_*.md` |
| license-compliance | both | `LICENSE_COMPLIANCE_REPORT_*_frontend.md` / `LICENSE_COMPLIANCE_REPORT_*_backend.md` |
| dependency-vulnerabilities | both | `SCA_REPORT_*_frontend.md` / `SCA_REPORT_*_backend.md` |

**Order every file by its embedded `**Generated:**` ISO timestamp, never
by filename or filesystem mtime.** The filename's date fragment doesn't
sort correctly once a same-day rerun adds a `-2`/`-3` suffix (most of
these scripts actually overwrite a same-day file in place instead — see
step 3 — but not all of them do, so don't rely on that either way), and
mtime is actively unreliable here: git does not preserve original file
modification times, so a fresh clone or a `git checkout` stamps every
file with the checkout time, making "sort by mtime" meaningless for
anyone who didn't generate every report in the same uninterrupted working
copy. Every report template writes a `**Generated:** <ISO timestamp>`
line near the top specifically because it's the one reliable ordering
key — use it.

Read each file's metrics per the extraction table in step 4 as you go, so
each category-and-side ends this step as a chronological list of
`(generated timestamp, extracted values)` pairs — the series' full trend
prior to this run. If a pattern matches nothing, that category/side has
no history yet; say so rather than fabricating a series.

**Efficiency note:** don't `Read` every historical file in full. For a
category whose headline lives on one `**Label:** value` line, one `Grep`
call across the whole folder with a pattern alternating `\*\*Generated:\*\*`
and the headline pattern (e.g. `\*\*Generated:\*\*|\*\*Type coverage:\*\*`)
returns both fields, one match per line, for every file in a single pass —
pair each file's two matches back up by its path in the Grep output. For a
table-based category (lighthouse's/route-coverage's per-route rows,
a11y's impact-breakdown table, auth-coverage's endpoint table), grep the
specific row pattern instead of a bold-line pattern — see step 4 for which
categories need this.

### 2. Run the sweep

Touch a marker file, then run `bash scripts/run-all-quality-reports.sh`
from `JPPhotoManagerWeb/` (it runs both the frontend's `npm run
reports:all` and the backend's `bash scripts/run-all-quality-reports.sh`
in sequence) so every fresh report's mtime can be told apart from the
history read in step 1 (mtime is fine for this — it only needs to
distinguish "the file this run just wrote" from "everything older,"
within one uninterrupted working copy, not to order history across a
clone or checkout):

```
cd JPPhotoManagerWeb
touch /tmp/quality-metrics-marker  # or the session scratchpad, same effect
bash scripts/run-all-quality-reports.sh
```

This skips two reports by default: mutation testing on both sides (a full
PIT/Stryker run per mutant — minutes, not seconds) and the frontend's
real-backend E2E tier (needs the full app already deployed to k8s — see
the `e2e-suite` skill §1 for that precondition). Budget several minutes
for the full run (the frontend build, Lighthouse, mocked E2E, and axe-core
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
categories failed; their "current" value in step 6 is really last run's
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

### 3. Capture the fresh snapshot

```
find JPPhotoManagerWeb/docs/reports -newer /tmp/quality-metrics-marker -name '*.md'
```

This lists every newly written report file across all categories in one
shot. Match each one back to its category (and frontend/backend side, by
filename suffix) and extract its key metrics per the same extraction
table in step 4, appending it as the newest point on that series from
step 1. Most of these scripts overwrite a same-day file in place rather
than adding a `-2`/`-3` suffix — a second same-day run of this skill will
often show the same filename in this list as step 1's history read, just
with a newer `**Generated:**` timestamp inside it. That's expected: the
morning's intermediate value is simply gone from disk once overwritten
unless it was committed in between.

### 4. Extract each category's key metrics

Applies to every historical file from step 1 and every fresh file from
step 3 alike — the same fields, the same patterns per category, so each
file becomes one point on its series. Pull these fields via `Grep`/pattern
match — most of these report scripts write consistent bold-line phrasing,
but the backend's Maven-CLI-wrapping scripts (`dead-code` and
`dependency-staleness`, backend side) don't produce a clean summary line
at all, so those two need a best-effort line-count instead:

| Category / side | Fields to extract | Where in the file |
| --- | --- | --- |
| type-coverage | Type coverage %; untyped (`any`) count | `**Type coverage:** X%` / `**Untyped (\`any\`) identifiers:** N` |
| complexity (frontend) | Files scanned; functions analyzed; avg complexity; avg file size | `**Files scanned:** N`; `**Functions analyzed:** N (average complexity: X...)`; `**Average file size:** N lines` |
| complexity (backend) | Files scanned; methods analyzed; avg complexity; avg file size | same shape, `**Methods analyzed:**` instead of `**Functions analyzed:**` |
| dead-code (frontend) | Total findings + sub-counts | `**Total findings:** N (a unused file(s), b unused export(s), c unused type(s), d unused dependenc(y/ies), e unlisted dependenc(y/ies))` |
| dead-code (backend) | Unused/undeclared dependency counts (best-effort) | no bold summary — this is raw `mvn dependency:analyze` output; count lines under the "Unused declared dependencies" and "Used undeclared dependencies" sections, and note that every `spring-boot-starter-*` entry is documented as expected noise, not a real finding — don't count those toward a "regression" |
| route-coverage | Routes-with-no-E2E-coverage count | `**Routes with no E2E coverage in either tier:** ...` (count the comma-separated list, or "none") |
| auth-coverage | Total endpoints tracked; count resolving to `permitAll()` | no bold summary — this is a raw endpoint table; count table rows (`^\| [A-Z]+ \|`) for the total, and count rows containing `permitAll()` in the Governing rule column for the second figure |
| lighthouse | Performance/Accessibility/Best Practices/SEO per route | the results table |
| a11y | Total violations; Critical/Serious/Moderate/Minor breakdown | `**Total violations:** N`; the impact-count table |
| code-coverage (frontend) | Statements/Branches/Functions/Lines %; files-below-80% count | `**Statements:** X% (a/b)` (same shape for the other 3); `## N file(s) below 80% on at least one metric` |
| code-coverage (backend) | Lines/Branches/Methods % (JaCoCo) | `**Lines:** X% (a/b)` (same shape for `**Branches:**`/`**Methods:**`) |
| bundle-size | Initial bundle size (kB); budget status; total JS shipped (kB); total dist size (kB) | the four `**...:**` lines near the top |
| dependency-staleness (frontend) | Total direct deps; outdated count + major/minor/patch breakdown | `**Total dependencies (direct):** N`; `**Outdated:** N (a major, b minor, c patch behind)` |
| dependency-staleness (backend) | Outdated dependency count (best-effort) | no bold summary — this is raw `mvn versions:display-dependency-updates` output; count `[INFO]   <groupId>:<artifactId> ... -> ...` lines |
| e2e-run (mocked/real) | Tests total/passed/failed/skipped; flaky count; total duration | `**Tests:** N (p passed, f failed, s skipped)`; `**Flaky tests...:** N`; `**Total duration:** Xs` |
| mutation (frontend) | Overall mutation score %; killed/total mutants | `**Overall mutation score:** X% (k/t tested mutants killed)` |
| mutation (backend) | Line coverage %; mutation coverage %; test strength % (PIT) | `**Line coverage:** X%`; `**Mutation coverage:** X%`; `**Test strength:** X%` |
| secrets-scan | Findings count | `**Findings:** N potential secret(s)` |
| license-compliance (frontend) | Packages scanned; flagged count | `**N package(s) scanned, M flagged**` |
| license-compliance (backend) | Packages scanned; needing action; previously-accepted count | `**N package(s) scanned, M need action, K previously reviewed and accepted.**` |
| dependency-vulnerabilities (frontend) | Vulnerable package count; critical/high/moderate/low/info breakdown | `**N vulnerable package(s):** c critical, h high, m moderate, l low, i info` |
| dependency-vulnerabilities (backend) | Known-vulnerability count (OSV.dev) | `**N known vulnerabilities across the resolved dependency tree.**` |

### 5. Describe the trend across the full series

Each category-and-side now has a chronological list of values (every
historical report plus the fresh one). Don't collapse this to a single
previous-vs-current delta — that's exactly the "can't tell a steady drift
from noise" problem committing the history was meant to fix. For each
metric:

- **State the most-recent delta** (fresh value vs. the point immediately
  before it) — still the single most actionable number, and the one that
  answers "did today's run change anything."
- **Characterize the shape of the whole series** in one short phrase,
  judged across every point, not just the last two:
  - **Flat** — values cluster tightly with no consistent direction (the
    normal case for a healthy, stable metric).
  - **Steadily improving / steadily declining** — most consecutive points
    move the same direction, or the latest point continues a run of
    several same-direction moves.
  - **Volatile** — swings both directions across the series with no
    consistent pattern; a single-point delta here would be misleading
    either way.
  - **One-off** — the series was flat/stable and the fresh point is a
    single outlier that breaks the pattern for the first time — worth
    flagging even though it's only one data point, precisely *because*
    the history shows it's a break from an established baseline rather
    than ordinary noise.
  - **First data point** / **too little history** — fewer than 3 points
    total (including the fresh one). Say so plainly rather than
    describing a "shape" from 1–2 points; a shape claim needs at least 3
    to distinguish a real pattern from a single delta.
- **Judge direction** using the same good-thing/bad-thing classification:
  - **Higher is better:** type coverage %, coverage % (all sides/metrics),
    lighthouse scores, mutation/line/test-strength %, license packages
    scanned (informational, not good/bad), E2E tests passed.
  - **Lower is better (ideally 0):** untyped count, dead-code findings,
    route-coverage gaps, auth-coverage endpoints with no explicit rule (if
    tracked that way — judge case by case, since `permitAll()` on a
    genuinely public endpoint like login isn't a defect), a11y violations,
    files-below-80%-coverage count, dependency-staleness outdated counts,
    E2E failed/flaky count, secrets findings, license flagged/needs-action
    count, vulnerability counts (overall and per severity), average
    complexity.
  - **Informational only (track, don't judge):** files/functions/methods
    scanned, average file size, bundle size in kB vs. its budget (flag
    only if it crosses from "within budget" to "over budget" or vice
    versa), test duration, total dependency count, total endpoints
    tracked (a growing API surface isn't itself good or bad).

Use ▲ for improved, ▼ for regressed, → for flat/unchanged, and — for "not
enough history yet."

### 6. Display the trend report

One table per category group, headline metric(s) with a compact
**History** column (every value in the series, oldest→newest) plus the
most-recent delta, the shape description from step 5, and a trend marker;
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

### 7. Note next steps — never auto-commit

The freshly generated files are new uncommitted changes under the tracked
`JPPhotoManagerWeb/docs/reports/<category>/` folders. Point this out and
suggest committing them (so the trend history in git actually grows), but
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
  drift from ordinary noise. Every category lookup in step 1 must glob
  every matching file, not just the newest.
- **Order history by the embedded `**Generated:**` timestamp, never by
  filename or filesystem mtime.** Filenames don't sort correctly once a
  same-day rerun adds a `-2`/`-3` suffix (and most of these scripts don't
  even do that — they overwrite in place), and git does not preserve
  original mtimes — a clone or checkout stamps every file with the
  checkout time, silently scrambling any mtime-based ordering for anyone
  who didn't generate the whole history in one uninterrupted working copy.
  Mtime is still the right tool for step 2–3's marker-file/`find -newer`
  diff, since that only needs to isolate "the file this run just wrote"
  within the current session — a different, narrower job than ordering
  history.
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
