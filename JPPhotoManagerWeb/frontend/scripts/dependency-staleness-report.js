#!/usr/bin/env node
// Reports how far behind latest each dependency is, not just which ones have
// a known vulnerability (that's security-reviewer's job) or are actively
// broken (dependency-upgrade's job) — this is a standing staleness signal so
// drift is visible before it becomes a security or breakage problem. Writes
// a dated snapshot report under JPPhotoManagerWeb/docs/reports/dependency-staleness/,
// the same convention bundle-size-report.js and the code/database/
// security-reviewer skills use.
//
// Usage: node scripts/dependency-staleness-report.js

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

function compareSemver(a, b) {
  const pa = a.replace(/^[^\d]*/, '').split(/[.\-+]/).map((n) => parseInt(n, 10) || 0);
  const pb = b.replace(/^[^\d]*/, '').split(/[.\-+]/).map((n) => parseInt(n, 10) || 0);
  for (let i = 0; i < 3; i++) {
    if ((pa[i] || 0) !== (pb[i] || 0)) return (pa[i] || 0) - (pb[i] || 0);
  }
  return 0;
}

function classify(current, latest) {
  if (!current || !latest || current === latest) return 'current';
  // npm's "latest" is a dist-tag, not necessarily the highest published
  // version — an installed pre-release/next-track version can be
  // numerically ahead of it. Only classify as behind when current is
  // actually lower.
  if (compareSemver(current, latest) >= 0) return 'current';
  const cur = current.replace(/^[^\d]*/, '').split('.').map((n) => parseInt(n, 10) || 0);
  const lat = latest.replace(/^[^\d]*/, '').split('.').map((n) => parseInt(n, 10) || 0);
  if (cur[0] !== lat[0]) return 'major';
  if (cur[1] !== lat[1]) return 'minor';
  return 'patch';
}

let raw = '{}';
try {
  // npm outdated exits non-zero when it finds outdated packages — that's
  // expected, not a failure; only its stdout matters here.
  raw = execSync('npm outdated --json', { encoding: 'utf8' });
} catch (err) {
  raw = err.stdout ? err.stdout.toString() : '{}';
}

let outdated = {};
try {
  outdated = JSON.parse(raw || '{}');
} catch {
  outdated = {};
}

const pkgJson = JSON.parse(fs.readFileSync(path.resolve('package.json'), 'utf8'));
const totalDeps = Object.keys(pkgJson.dependencies || {}).length + Object.keys(pkgJson.devDependencies || {}).length;

const rows = Object.entries(outdated)
  .map(([name, info]) => ({
    name,
    current: info.current || '(missing)',
    wanted: info.wanted,
    latest: info.latest,
    severity: classify(info.current, info.latest),
  }))
  // Drop entries where the installed version is already at or ahead of the
  // "latest" dist-tag (see classify()'s note) — not meaningfully outdated.
  .filter((r) => r.severity !== 'current')
  .sort((a, b) => {
    const order = { major: 0, minor: 1, patch: 2 };
    return order[a.severity] - order[b.severity] || compareSemver(b.latest || '0', a.latest || '0');
  });

const counts = rows.reduce(
  (acc, r) => {
    acc[r.severity] = (acc[r.severity] || 0) + 1;
    return acc;
  },
  { major: 0, minor: 0, patch: 0 }
);

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();

const lines = [];
lines.push(`# Dependency Staleness Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push(`**Scope:** ${path.basename(process.cwd())}/package.json`);
lines.push('');
lines.push(`**Total dependencies (direct):** ${totalDeps}`);
lines.push(`**Outdated:** ${rows.length} (${counts.major} major, ${counts.minor} minor, ${counts.patch} patch behind)`);
lines.push('');
if (rows.length > 0) {
  lines.push('| Package | Current | Wanted | Latest | Behind by |');
  lines.push('| --- | --- | --- | --- | --- |');
  for (const r of rows) {
    lines.push(`| ${r.name} | ${r.current} | ${r.wanted} | ${r.latest} | ${r.severity} |`);
  }
} else {
  lines.push('Every direct dependency is at its latest published version.');
}
lines.push('');

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('../..');
  }
}

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/dependency-staleness');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `DEPENDENCY_STALENESS_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(lines.join('\n'));
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
