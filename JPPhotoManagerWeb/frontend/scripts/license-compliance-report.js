#!/usr/bin/env node
// Scans every resolved npm dependency's declared license via
// license-checker-rseidelsohn and writes a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/license-compliance/, the same convention
// every other quality-metric script in this repo uses (see
// code-coverage-report.js). The backend (Maven) equivalent is
// backend/scripts/license-report.sh, using license-maven-plugin.
//
// Flags copyleft licenses (GPL/AGPL/LGPL/SSPL/EUPL family — anything that
// could force this project's own source to be redistributed under the
// same terms if shipped) and anything with no resolvable license at all.
// This repo is public, so an unnoticed copyleft dependency is a real
// concern, not just hygiene.
//
// Usage: node scripts/license-compliance-report.js (or `npm run license:report`)

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

console.log('Checking dependency licenses with license-checker-rseidelsohn...');

let jsonOutput = '';
try {
  jsonOutput = execSync('npx license-checker-rseidelsohn --json --excludePrivatePackages', {
    encoding: 'utf8',
    maxBuffer: 1024 * 1024 * 16,
  });
} catch (err) {
  jsonOutput = err.stdout ? err.stdout.toString() : '';
  if (!jsonOutput) {
    console.error('license-checker-rseidelsohn produced no output.');
    console.error(err.stderr ? err.stderr.toString() : err.message);
    process.exit(1);
  }
}

let packages;
try {
  packages = JSON.parse(jsonOutput);
} catch {
  console.error('Could not parse license-checker-rseidelsohn JSON output.');
  process.exit(1);
}

const COPYLEFT_RE = /(^|[^A-Za-z])(A?GPL|LGPL|SSPL|EUPL|CC-BY-SA|OSL|CPAL)([^A-Za-z]|-\d|$)/i;

const rows = Object.entries(packages).map(([nameVersion, info]) => ({
  nameVersion,
  licenses: info.licenses || 'UNKNOWN',
  repository: info.repository || '',
}));

const flagged = rows.filter(
  (r) => r.licenses === 'UNKNOWN' || COPYLEFT_RE.test(String(r.licenses)),
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
lines.push(`# License Compliance Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** `frontend/` npm dependency tree (license-checker-rseidelsohn, excludes private/unpublished packages)');
lines.push('');
lines.push(`**${rows.length} package(s) scanned, ${flagged.length} flagged** (copyleft license or no resolvable license).`);
lines.push('');

if (flagged.length > 0) {
  lines.push('| Package | License | Repository |');
  lines.push('| --- | --- | --- |');
  for (const r of flagged.sort((a, b) => a.nameVersion.localeCompare(b.nameVersion))) {
    lines.push(`| ${r.nameVersion} | ${r.licenses} | ${r.repository} |`);
  }
  lines.push('');
  lines.push('A copyleft license here is worth a second look before shipping — most flagged packages will be devDependencies (build/test tooling never distributed with the app), which is generally fine regardless of license family; a *runtime* `dependencies` entry with a copyleft or unknown license is the case that actually matters. `UNKNOWN` usually means the package has no `license` field and no detectable `LICENSE` file — check its repository directly rather than assuming the worst.');
} else {
  lines.push('No copyleft or unresolvable licenses found among scanned dependencies.');
}
lines.push('');

const reportDir = path.join(repoRoot, 'JPPhotoManagerWeb/docs/reports/license-compliance');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `LICENSE_COMPLIANCE_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`\n${rows.length} package(s) scanned, ${flagged.length} flagged.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);

process.exit(0);
