#!/usr/bin/env node
// Reports the top cyclomatic-complexity and file-size (LOC) hotspots across
// the frontend source tree, as a trending snapshot rather than the pass/fail
// gate cyclomatic-complexity.js already runs (via `npm run complexity`, used
// by the code-reviewer skill's max-15-per-function check, §18.1). Even when
// every function is under threshold, this shows which files/functions are
// closest to it and growing, before they become an actual violation. File
// size is tracked alongside complexity because a large file is often where
// the next complex function gets added. Writes a dated snapshot report
// under JPPhotoManagerWeb/docs/reports/complexity/, the same convention
// bundle-size-report.js/dependency-staleness-report.js use.
//
// The backend (Java) equivalent is scripts/complexity-report.sh in
// backend/, which does the same thing via a report-only PMD ruleset.
//
// Usage: node scripts/complexity-report.js (or `npm run complexity:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const { analyzeComplexity } = require('./cyclomatic-complexity');

const root = path.resolve('src/app');
const { files, results } = analyzeComplexity(root);

const TOP_N = 20;
const topComplexity = results.slice(0, TOP_N);

const fileSizes = files
  .map((file) => {
    const lineCount = fs.readFileSync(file, 'utf8').split('\n').length;
    return { file: path.relative(process.cwd(), file).replace(/\\/g, '/'), lines: lineCount };
  })
  .sort((a, b) => b.lines - a.lines);

const topFileSizes = fileSizes.slice(0, TOP_N);

const avgComplexity = results.length > 0 ? (results.reduce((sum, r) => sum + r.complexity, 0) / results.length).toFixed(2) : '0';
const avgFileSize = fileSizes.length > 0 ? Math.round(fileSizes.reduce((sum, f) => sum + f.lines, 0) / fileSizes.length) : 0;

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
lines.push(`# Complexity & File-Size Hotspots Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** frontend/src/app (excludes *.cy.ts, *.spec.ts)');
lines.push('');
lines.push(`**Files scanned:** ${files.length}`);
lines.push(`**Functions analyzed:** ${results.length} (average complexity: ${avgComplexity}; gate threshold: 15, see \`npm run complexity\`)`);
lines.push(`**Average file size:** ${avgFileSize} lines`);
lines.push('');
lines.push(`## Top ${topComplexity.length} functions by cyclomatic complexity`);
lines.push('');
lines.push('| Complexity | File | Line | Function |');
lines.push('| --- | --- | --- | --- |');
for (const r of topComplexity) {
  lines.push(`| ${r.complexity} | ${r.file} | ${r.line} | ${r.name} |`);
}
lines.push('');
lines.push(`## Top ${topFileSizes.length} files by line count`);
lines.push('');
lines.push('| Lines | File |');
lines.push('| --- | --- |');
for (const f of topFileSizes) {
  lines.push(`| ${f.lines} | ${f.file} |`);
}
lines.push('');

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/complexity');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `COMPLEXITY_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`Analyzed ${results.length} functions across ${files.length} files. Average complexity: ${avgComplexity}, average file size: ${avgFileSize} lines.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
