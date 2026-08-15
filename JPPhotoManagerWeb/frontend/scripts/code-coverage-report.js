#!/usr/bin/env node
// Wraps the Cypress Component Testing suite's code-coverage run into a
// dated snapshot report under JPPhotoManagerWeb/docs/reports/code-coverage/,
// the same convention bundle-size-report.js/dependency-staleness-report.js
// use. `npm run coverage:check` (nyc check-coverage, reading .nycrc.json,
// code-reviewer skill §19.1) is a pass/fail gate against a fixed 80%
// threshold — useful in CI, but it doesn't leave a record of the *actual*
// percentage over time, or which specific files are furthest from the
// threshold on which metric. This captures both.
//
// The backend (Java) equivalent is scripts/coverage-report.sh in backend/,
// which parses JaCoCo's XML report instead — see code-reviewer skill §19.2
// for the backend gate this trending report complements.
//
// Runs the full component-test suite itself (like bundle-size-report.js
// runs its own `ng build`), so this is self-contained — no need to have
// already run `npm run test:coverage` first.
//
// Usage: node scripts/code-coverage-report.js (or `npm run coverage:trend-report`)
//
// IMPORTANT: this script's package.json key must never be the literal
// string "coverage:report" — @cypress/code-coverage's Node task
// (node_modules/@cypress/code-coverage/dist/lib/task.js) hardcodes that
// exact name as its own custom-report-script convention and will `npm run`
// it automatically after every spec file if present. Since this script
// itself re-runs the whole Cypress suite, that collision causes unbounded
// recursive Cypress invocations (confirmed: hung/crashed real CI runs on
// both this project and pablo-web, which independently picked the same
// script name). Keep the key as "coverage:trend-report" or anything else
// that isn't exactly "coverage:report".

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('Running component test suite with coverage instrumentation...');
try {
  execSync('npx cypress run --component --env coverage=true', { stdio: 'inherit' });
} catch (err) {
  // A failing test still produces a coverage report worth capturing — only
  // bail out if nyc never got to write anything at all (checked below).
  console.warn('\nComponent test run reported failures — continuing to report whatever coverage was captured.');
}

execSync('npx nyc report --reporter=json-summary --reporter=text', { stdio: 'inherit' });

const summaryPath = path.resolve('coverage/coverage-summary.json');
if (!fs.existsSync(summaryPath)) {
  console.error(`No coverage summary found at ${summaryPath} — the test run may have failed before producing any coverage data.`);
  process.exit(1);
}

const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
const { total } = summary;
const METRICS = ['statements', 'branches', 'functions', 'lines'];
const THRESHOLD = 80;

// Model-only files (interfaces/types with no runtime code) always read 0/0
// — exclude them from the per-file "below threshold" callouts, they were
// never executable in the first place and aren't a real gap.
const fileRows = Object.entries(summary)
  .filter(([file]) => file !== 'total')
  .map(([file, metrics]) => ({
    file: path.relative(process.cwd(), file).replace(/\\/g, '/'),
    ...Object.fromEntries(METRICS.map((m) => [m, metrics[m].pct])),
  }))
  .filter((r) => !(r.file.endsWith('.model.ts') && METRICS.every((m) => r[m] === 0)));

const belowThreshold = fileRows
  .filter((r) => METRICS.some((m) => r[m] < THRESHOLD))
  .sort((a, b) => Math.min(...METRICS.map((m) => a[m])) - Math.min(...METRICS.map((m) => b[m])));

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('..');
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
lines.push(`# Code Coverage Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** Cypress Component Testing suite (frontend/src/app/**/*.ts, see .nycrc.json)');
lines.push(`**Gate:** \`npm run coverage:check\` enforces ${THRESHOLD}% minimum on all four metrics, project-wide (code-reviewer skill §19.1)`);
lines.push('');
lines.push(`**Statements:** ${total.statements.pct}% (${total.statements.covered}/${total.statements.total})`);
lines.push(`**Branches:** ${total.branches.pct}% (${total.branches.covered}/${total.branches.total})`);
lines.push(`**Functions:** ${total.functions.pct}% (${total.functions.covered}/${total.functions.total})`);
lines.push(`**Lines:** ${total.lines.pct}% (${total.lines.covered}/${total.lines.total})`);
lines.push('');

if (belowThreshold.length > 0) {
  lines.push(`## ${belowThreshold.length} file(s) below ${THRESHOLD}% on at least one metric`);
  lines.push('');
  lines.push('| File | Statements | Branches | Functions | Lines |');
  lines.push('| --- | --- | --- | --- | --- |');
  for (const r of belowThreshold) {
    const fmt = (m) => (r[m] < THRESHOLD ? `**${r[m]}%**` : `${r[m]}%`);
    lines.push(`| ${r.file} | ${fmt('statements')} | ${fmt('branches')} | ${fmt('functions')} | ${fmt('lines')} |`);
  }
  lines.push('');
} else {
  lines.push(`Every file (excluding type-only \`.model.ts\` files) is at or above ${THRESHOLD}% on all four metrics.`);
  lines.push('');
}

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/code-coverage');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `CODE_COVERAGE_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`\nStatements ${total.statements.pct}% / Branches ${total.branches.pct}% / Functions ${total.functions.pct}% / Lines ${total.lines.pct}%`);
console.log(`${belowThreshold.length} file(s) below ${THRESHOLD}% on at least one metric.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
