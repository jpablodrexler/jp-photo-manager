#!/usr/bin/env node
// Reports what fraction of the frontend's TypeScript identifiers have a
// real (non-`any`) type, using the `type-coverage` package. Complements the
// Cypress code-coverage report: that one measures how much code executes
// under test, this one measures how strictly the code that runs is typed —
// relevant here specifically because this project's own conventions ban
// `any` unless unavoidable (see CLAUDE.md's "Strict TypeScript" note), but
// nothing today measures whether that convention actually holds
// project-wide. Writes a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/type-coverage/, the same convention
// bundle-size-report.js/dependency-staleness-report.js use.
//
// Usage: node scripts/type-coverage-report.js (or `npm run type-coverage:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

let raw;
try {
  // type-coverage exits non-zero when --at-least/--is is set and unmet;
  // none of those flags are passed here, so a non-zero exit means the CLI
  // itself failed (a real problem) rather than "coverage too low".
  raw = execSync(
    'npx type-coverage --project tsconfig.app.json --detail --json-output --show-relative-path',
    { encoding: 'utf8', maxBuffer: 20 * 1024 * 1024 }
  );
} catch (err) {
  console.error('type-coverage failed to run:');
  console.error(err.stdout || err.message);
  process.exit(1);
}

const result = JSON.parse(raw);
const correctCount = result.totalCount - result.details.length;
const percent = ((correctCount / result.totalCount) * 100).toFixed(2);

// Group uncovered (any-typed) identifiers by file to surface hotspots —
// a handful of files with many `any` spots matter more than one-off cases
// scattered across the tree.
const byFile = new Map();
for (const d of result.details) {
  const key = d.filePath.replace(/\\/g, '/');
  byFile.set(key, (byFile.get(key) || 0) + 1);
}
const hotspots = [...byFile.entries()].sort((a, b) => b[1] - a[1]);

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
lines.push(`# Type Coverage Report — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** frontend/tsconfig.app.json (application source, excludes *.cy.ts/*.spec.ts)');
lines.push('');
lines.push(`**Type coverage:** ${percent}% (${correctCount} / ${result.totalCount} identifiers have a non-\`any\` type)`);
lines.push(`**Untyped (\`any\`) identifiers:** ${result.details.length}, across ${hotspots.length} file(s)`);
lines.push('');
if (hotspots.length > 0) {
  lines.push('## Files with the most `any`-typed identifiers');
  lines.push('');
  lines.push('| File | Count |');
  lines.push('| --- | --- |');
  for (const [file, count] of hotspots.slice(0, 15)) {
    lines.push(`| ${file} | ${count} |`);
  }
  lines.push('');
  lines.push('## Every uncovered location');
  lines.push('');
  lines.push('| File | Line | Identifier |');
  lines.push('| --- | --- | --- |');
  for (const d of result.details) {
    lines.push(`| ${d.filePath.replace(/\\/g, '/')} | ${d.line} | \`${d.text}\` |`);
  }
} else {
  lines.push('Every identifier in scope has a non-`any` type.');
}
lines.push('');

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/type-coverage');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `TYPE_COVERAGE_REPORT_${date}.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`Type coverage: ${percent}% (${correctCount} / ${result.totalCount})`);
console.log(`${result.details.length} untyped identifier(s) across ${hotspots.length} file(s).`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
