#!/usr/bin/env node
// Runs `npm audit` against the resolved frontend dependency tree and
// writes a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/dependency-vulnerabilities/, the same
// convention every other quality-metric script in this repo uses (see
// code-coverage-report.js). The backend (Maven) equivalent queries OSV.dev
// directly — see backend/scripts/sca-report.sh.
//
// Distinct from dependency-staleness-report.js: staleness tracks *outdated*
// packages (a newer version exists), this tracks *vulnerable* ones (a
// known CVE/advisory applies to the resolved version) — a package can be
// current and vulnerable, or stale and perfectly safe. Uses `npm audit`
// (built into npm, no extra devDependency) — npm's own advisory database
// is authoritative for npm packages and needs no separate account/API key.
//
// Usage: node scripts/sca-report.js (or `npm run sca:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('..', '..');
  }
}

const repoRoot = findRepoRoot();

console.log('Running npm audit...');

let jsonOutput = '';
try {
  jsonOutput = execSync('npm audit --json', { encoding: 'utf8', maxBuffer: 1024 * 1024 * 16 });
} catch (err) {
  jsonOutput = err.stdout ? err.stdout.toString() : '';
  if (!jsonOutput) {
    console.error('npm audit produced no output.');
    console.error(err.stderr ? err.stderr.toString() : err.message);
    process.exit(1);
  }
}

let report;
try {
  report = JSON.parse(jsonOutput);
} catch {
  console.error('Could not parse npm audit JSON output.');
  process.exit(1);
}

const counts = report.metadata?.vulnerabilities || {};
const vulnerabilities = report.vulnerabilities || {};

const SEVERITY_ORDER = { critical: 0, high: 1, moderate: 2, low: 3, info: 4 };
const rows = Object.values(vulnerabilities)
  .map((v) => ({
    name: v.name,
    severity: v.severity,
    range: v.range,
    fixAvailable: v.fixAvailable === false ? 'no' : v.fixAvailable === true ? 'yes' : (v.fixAvailable?.name ? `yes (${v.fixAvailable.name}@${v.fixAvailable.version})` : 'yes'),
    via: (v.via || [])
      .map((entry) => (typeof entry === 'string' ? entry : entry.title || entry.name))
      .filter(Boolean)
      .join('; '),
  }))
  .sort((a, b) => (SEVERITY_ORDER[a.severity] ?? 9) - (SEVERITY_ORDER[b.severity] ?? 9));

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();
const total = counts.total ?? rows.length;

const lines = [];
lines.push(`# Dependency Vulnerability (SCA) Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** `frontend/` npm dependency tree (`npm audit`, npm\'s own advisory database)');
lines.push('');
lines.push(`**${total} vulnerable package(s):** ${counts.critical ?? 0} critical, ${counts.high ?? 0} high, ${counts.moderate ?? 0} moderate, ${counts.low ?? 0} low, ${counts.info ?? 0} info.`);
lines.push('');

if (rows.length > 0) {
  lines.push('| Package | Severity | Vulnerable range | Fix available | Advisory |');
  lines.push('| --- | --- | --- | --- | --- |');
  for (const r of rows) {
    lines.push(`| ${r.name} | ${r.severity} | ${r.range} | ${r.fixAvailable} | ${r.via.replace(/\|/g, '\\|')} |`);
  }
  lines.push('');
  lines.push('Critical/high severity with a fix available is the priority — `npm audit fix` (or `--force` for a breaking major bump, review the changelog first) resolves those directly. A vulnerability with no fix available needs a judgment call: is the vulnerable code path actually reachable in this app (many advisories are in a devDependency\'s own build tooling, never shipped or executed against untrusted input), and if so, is there an interim mitigation until upstream publishes a fix.');
} else {
  lines.push('No known vulnerabilities in the resolved dependency tree.');
}
lines.push('');

const reportDir = path.join(repoRoot, 'JPPhotoManagerWeb/docs/reports/dependency-vulnerabilities');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `SCA_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`\n${total} vulnerable package(s): ${counts.critical ?? 0} critical, ${counts.high ?? 0} high, ${counts.moderate ?? 0} moderate, ${counts.low ?? 0} low.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);

process.exit(0);
