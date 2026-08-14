#!/usr/bin/env node
// Reports unused files, exports, types, and dependencies across the
// frontend, via `knip` (configured in knip.jsonc — see that file's comments
// for why Cypress config/spec files and a handful of name-resolved
// devDependencies need explicit entries/ignores to avoid false positives on
// an Angular + Cypress project using the legacy .eslintrc.json format).
// Writes a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/dead-code/, the same convention
// bundle-size-report.js/dependency-staleness-report.js use.
//
// The backend (Java) equivalent is scripts/dead-code-report.sh in
// backend/, which uses `mvn dependency:analyze` instead — Maven's own
// unused/undeclared-dependency detector, since knip is JS/TS-only.
//
// Usage: node scripts/dead-code-report.js (or `npm run dead-code:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

let raw;
try {
  // knip exits non-zero whenever it finds any issue at all — that's
  // expected here, not a failure; only its stdout (the JSON report) matters.
  raw = execSync('npx knip --reporter json', { encoding: 'utf8', maxBuffer: 20 * 1024 * 1024 });
} catch (err) {
  raw = err.stdout ? err.stdout.toString() : null;
  if (!raw) {
    console.error('knip failed to run:');
    console.error(err.stderr || err.message);
    process.exit(1);
  }
}

const { issues } = JSON.parse(raw);

const unusedFiles = [];
const unusedExports = [];
const unusedTypes = [];
const unusedDeps = [];
const unlisted = [];

for (const issue of issues) {
  if (issue.files && issue.files.length) unusedFiles.push(issue.file);
  for (const e of issue.exports || []) unusedExports.push({ file: issue.file, ...e });
  for (const t of issue.types || []) unusedTypes.push({ file: issue.file, ...t });
  for (const d of [...(issue.dependencies || []), ...(issue.devDependencies || [])]) {
    unusedDeps.push({ file: issue.file, name: d.name });
  }
  for (const u of issue.unlisted || []) unlisted.push({ file: issue.file, name: u.name });
}

const totalIssues = unusedFiles.length + unusedExports.length + unusedTypes.length + unusedDeps.length + unlisted.length;

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
lines.push(`# Dead Code Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** frontend (see knip.jsonc for entry points and intentional ignores)');
lines.push('');
lines.push(`**Total findings:** ${totalIssues} (${unusedFiles.length} unused file(s), ${unusedExports.length} unused export(s), ${unusedTypes.length} unused type(s), ${unusedDeps.length} unused dependenc(y/ies), ${unlisted.length} unlisted dependenc(y/ies))`);
lines.push('');

if (unusedFiles.length > 0) {
  lines.push('## Unused files');
  lines.push('');
  lines.push('Never imported/referenced from any entry point.');
  lines.push('');
  for (const f of unusedFiles) lines.push(`- ${f}`);
  lines.push('');
}

if (unusedDeps.length > 0) {
  lines.push('## Unused dependencies');
  lines.push('');
  lines.push('Declared in package.json but never imported — verify still needed before removing (see knip.jsonc for known name-resolved exceptions already excluded).');
  lines.push('');
  lines.push('| Dependency |');
  lines.push('| --- |');
  for (const d of unusedDeps) lines.push(`| ${d.name} |`);
  lines.push('');
}

if (unlisted.length > 0) {
  lines.push('## Unlisted dependencies');
  lines.push('');
  lines.push('Imported/referenced in source or config but missing from package.json (relying on a transitive install, or referencing a package that was never installed at all — check both) — should be declared directly or removed.');
  lines.push('');
  lines.push('| File | Dependency |');
  lines.push('| --- | --- |');
  for (const u of unlisted) lines.push(`| ${u.file} | ${u.name} |`);
  lines.push('');
}

if (unusedExports.length > 0) {
  lines.push('## Unused exports');
  lines.push('');
  lines.push('| File | Line | Export |');
  lines.push('| --- | --- | --- |');
  for (const e of unusedExports) lines.push(`| ${e.file} | ${e.line} | ${e.name} |`);
  lines.push('');
}

if (unusedTypes.length > 0) {
  lines.push('## Unused types');
  lines.push('');
  lines.push('| File | Line | Type |');
  lines.push('| --- | --- | --- |');
  for (const t of unusedTypes) lines.push(`| ${t.file} | ${t.line} | ${t.name} |`);
  lines.push('');
}

if (totalIssues === 0) {
  lines.push('No unused files, exports, types, or dependencies found.');
  lines.push('');
}

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/dead-code');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `DEAD_CODE_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`${totalIssues} finding(s): ${unusedFiles.length} unused file(s), ${unusedExports.length} unused export(s), ${unusedTypes.length} unused type(s), ${unusedDeps.length} unused dependenc(y/ies), ${unlisted.length} unlisted.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
