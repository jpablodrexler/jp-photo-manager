#!/usr/bin/env node
// Wraps a StrykerJS mutation-testing run into a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/mutation/, the same convention
// code-coverage-report.js/complexity-report.js use.
//
// Cypress Component Testing has no native Stryker test-runner plugin, so
// stryker.conf.mjs uses the generic "command" runner (commandRunner.command
// runs `npm run test:mutation`, a narrower `cypress run --component --spec`
// invocation covering only the specs for the curated `mutate` file list —
// see that config's comment for why a scoped spec list is used instead of
// the full `npm run test` suite: with concurrency 1 and one full Cypress
// process spawned per mutant, the full ~650-test suite would make even a
// modest mutant count take hours; the scoped spec list still exercises
// every mutated file's own tests, just without the unrelated ~47 other spec
// files' dead weight).
//
// IMPORTANT (read before touching stryker.conf.mjs or cypress/support/component.ts):
// Stryker's command runner communicates which mutant is "active" via the
// env var __STRYKER_ACTIVE_MUTANT__, read through `process.env` — but the
// mutated code under test here runs inside a Cypress *browser* iframe, which
// has no Node `process` global. Without a bridge, every mutant silently
// "survives" (the switch never activates, original code always runs,
// mutation score is permanently stuck at 0% regardless of test quality —
// this was confirmed empirically while building this script: manually
// mutating a file and running its spec directly caught the bug immediately,
// while the identical mutation via `npx stryker run` did not). The fix
// lives in two places: cypress.config.ts's component `setupNodeEvents`
// forwards `process.env.__STRYKER_ACTIVE_MUTANT__` into `config.env`, and
// cypress/support/component.ts reads it via `Cypress.env(...)` and assigns
// `globalThis.process = { env: { __STRYKER_ACTIVE_MUTANT__: ... } }` before
// any spec file's own imports evaluate (ES module evaluation order
// guarantees the support file's top-level code — which isn't wrapped in a
// hook — runs before a later-imported spec module's top-level code). Both
// are guarded by the env var actually being set, so they're a no-op outside
// a Stryker run and don't affect the normal `npm run test`/`test:coverage`
// suites. If mutation scores are ever suspiciously and uniformly ~0% again,
// check that bridge first before assuming the tests themselves are weak.
//
// Also note: stryker.conf.mjs's `files` array explicitly excludes
// `.angular/**` (among other build-output dirs) — frontend/ has no
// `.gitignore` of its own (only the repo-root one, two directories up),
// so Stryker's default git-ignore-based file selection doesn't find it and
// was copying the entire Angular/Vite build cache into every sandbox. That
// didn't turn out to be the actual cause of the 0%-survival bug above, but
// it's wasted I/O worth keeping excluded regardless.
//
// Usage: node scripts/mutation-report.js (or `npm run mutation:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('Running StrykerJS mutation testing (curated core/ file scope, command runner)...');
try {
  execSync('npx stryker run', { stdio: 'inherit' });
} catch (err) {
  // Stryker exits non-zero if a configured score threshold isn't met (none
  // is configured here) or if the run itself errored — either way, still
  // try to report whatever reports/mutation/mutation.json was produced.
  console.warn('\nStryker run reported a non-zero exit — continuing to report whatever mutation data was captured.');
}

const jsonPath = path.resolve('reports/mutation/mutation.json');
if (!fs.existsSync(jsonPath)) {
  console.error(`No mutation report found at ${jsonPath} — the run may have failed before producing any report.`);
  process.exit(1);
}

const report = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const STATUS_COUNTS = ['Killed', 'Survived', 'Timeout', 'NoCoverage', 'RuntimeError', 'CompileError', 'Ignored'];

const fileRows = Object.entries(report.files).map(([file, data]) => {
  const counts = Object.fromEntries(STATUS_COUNTS.map((s) => [s, 0]));
  for (const mutant of data.mutants) {
    counts[mutant.status] = (counts[mutant.status] || 0) + 1;
  }
  const total = data.mutants.length;
  const scorable = total - counts.Ignored - counts.CompileError;
  const detected = counts.Killed + counts.Timeout + counts.RuntimeError;
  const score = scorable > 0 ? (100 * detected) / scorable : null;
  return { file, total, score, ...counts };
});

const totals = Object.fromEntries(STATUS_COUNTS.map((s) => [s, 0]));
for (const row of fileRows) {
  for (const s of STATUS_COUNTS) totals[s] += row[s];
}
const totalMutants = fileRows.reduce((sum, r) => sum + r.total, 0);
const totalScorable = totalMutants - totals.Ignored - totals.CompileError;
const totalDetected = totals.Killed + totals.Timeout + totals.RuntimeError;
const overallScore = totalScorable > 0 ? (100 * totalDetected) / totalScorable : 0;

const sortedRows = [...fileRows].sort((a, b) => (a.score ?? 0) - (b.score ?? 0));

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    // `git rev-parse` can fail in some spawned/background shells even when
    // available interactively (seen in practice while building this
    // script). The sibling report scripts' fallback of `path.resolve('..')`
    // is only one directory up from frontend/ (-> JPPhotoManagerWeb/), one
    // level short of the actual repo root two directories up
    // (-> jp-photo-manager/) that findRepoRoot() is expected to return here
    // — that mismatch silently wrote a report to a doubled
    // JPPhotoManagerWeb/JPPhotoManagerWeb/docs/reports/mutation/ path the
    // first time this ran. Walk upward looking for .git instead, so this
    // doesn't depend on cwd always being exactly one level below the repo
    // root.
    let dir = process.cwd();
    while (true) {
      if (fs.existsSync(path.join(dir, '.git'))) return dir;
      const parent = path.dirname(dir);
      if (parent === dir) return path.resolve('..', '..');
      dir = parent;
    }
  }
}

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();

const lines = [];
lines.push(`# Mutation Testing Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** curated core/ files with genuine branching logic, see stryker.conf.mjs `mutate` (thin CRUD-wrapper services excluded)');
lines.push('**Tool:** StrykerJS command runner (`npx stryker run`), driving a scoped `cypress run --component --spec` invocation per mutant');
lines.push('');
lines.push(`**Overall mutation score:** ${overallScore.toFixed(2)}% (${totalDetected}/${totalScorable} detected, excluding ${totals.Ignored} ignored + ${totals.CompileError} compile errors)`);
lines.push(`**Total mutants generated:** ${totalMutants}`);
lines.push(`**Killed:** ${totals.Killed}  **Survived:** ${totals.Survived}  **Timeout:** ${totals.Timeout}  **No coverage:** ${totals.NoCoverage}  **Runtime errors:** ${totals.RuntimeError}  **Compile errors:** ${totals.CompileError}  **Ignored:** ${totals.Ignored}`);
lines.push('');
lines.push('A survived mutant means the test suite ran and passed even though the mutated line changed the code\'s behavior — the tests exercise that code path but don\'t actually assert on the behavior a bug there would break. A "no coverage" mutant means no test reaches that line at all.');
lines.push('');
lines.push('## Per-file results (worst mutation score first)');
lines.push('');
lines.push('| File | Score | Total | Killed | Survived | Timeout | No cov | Runtime err | Compile err | Ignored |');
lines.push('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |');
for (const r of sortedRows) {
  const scoreStr = r.score === null ? 'n/a' : `${r.score.toFixed(2)}%`;
  lines.push(`| ${r.file} | ${scoreStr} | ${r.total} | ${r.Killed} | ${r.Survived} | ${r.Timeout} | ${r.NoCoverage} | ${r.RuntimeError} | ${r.CompileError} | ${r.Ignored} |`);
}
lines.push('');

const survivedMutants = [];
for (const [file, data] of Object.entries(report.files)) {
  for (const m of data.mutants) {
    if (m.status === 'Survived' || m.status === 'NoCoverage') {
      survivedMutants.push({
        file,
        line: m.location.start.line,
        mutator: m.mutatorName,
        replacement: m.replacement,
        status: m.status,
      });
    }
  }
}
survivedMutants.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line);

if (survivedMutants.length > 0) {
  lines.push(`## ${survivedMutants.length} surviving / uncovered mutant(s)`);
  lines.push('');
  lines.push('| File | Line | Mutator | Replacement | Status |');
  lines.push('| --- | --- | --- | --- | --- |');
  for (const m of survivedMutants) {
    lines.push(`| ${m.file} | ${m.line} | ${m.mutator} | \`${m.replacement}\` | ${m.status} |`);
  }
  lines.push('');
} else {
  lines.push('No surviving or uncovered mutants — every mutant in scope was killed or timed out.');
  lines.push('');
}

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/mutation');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `MUTATION_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`\nOverall mutation score: ${overallScore.toFixed(2)}% (${totalDetected}/${totalScorable} detected)`);
console.log(`Killed ${totals.Killed} / Survived ${totals.Survived} / Timeout ${totals.Timeout} / No coverage ${totals.NoCoverage}`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
