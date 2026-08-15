#!/usr/bin/env node
// Measures the production build's output size and writes a dated snapshot
// report under JPPhotoManagerWeb/docs/reports/bundle-size/, the same
// dated-report convention code-reviewer/database-reviewer/security-reviewer
// already use (gitignored, ephemeral session output). Run repeatedly over
// time, the reports form a de facto size-over-time history even though no
// single run persists trend data itself.
//
// Runs `ng build` itself (rather than assuming dist/ is already fresh) and
// parses its own stdout for the "Initial total" figure — the authoritative
// number Angular's own budget check (angular.json's `budgets` entries) uses.
// Deliberately does NOT sum every file under dist/ and compare that against
// the "initial" budget: this app lazy-loads most feature routes, so a
// whole-dist sum would always look over budget even when the real initial
// bundle is fine — the file-system walk below is used only for the
// informational "largest chunks" table, never for the budget comparison.
//
// Usage: node scripts/bundle-size-report.js (or `npm run bundle:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const angularJson = JSON.parse(fs.readFileSync(path.resolve('angular.json'), 'utf8'));
const projectName = Object.keys(angularJson.projects)[0];
const buildConfig = angularJson.projects[projectName].architect.build;
const outputPath = buildConfig.options.outputPath || `dist/${projectName}`;
const budgets = (buildConfig.configurations && buildConfig.configurations.production && buildConfig.configurations.production.budgets) || [];
const initialBudget = budgets.find((b) => b.type === 'initial');

console.log('Running production build...');
const buildOutput = execSync('npx ng build --configuration production', { encoding: 'utf8' });
console.log(buildOutput);

function parseSizeToBytes(size) {
  const match = /^([\d.]+)\s*(B|kB|MB)$/i.exec(size.trim());
  if (!match) return null;
  const value = parseFloat(match[1]);
  const unit = match[2].toLowerCase();
  if (unit === 'b') return value;
  if (unit === 'kb') return value * 1000;
  if (unit === 'mb') return value * 1000 * 1000;
  return null;
}

// Angular's own build output prints a line like:
//   | Initial total     | 323.06 kB |                63.89 kB
const initialTotalMatch = /Initial total\s*\|\s*([\d.]+\s*(?:B|kB|MB))/i.exec(buildOutput);
const initialTotalBytes = initialTotalMatch ? parseSizeToBytes(initialTotalMatch[1]) : null;

const distRoot = path.resolve(outputPath);
const browserDir = fs.existsSync(path.join(distRoot, 'browser')) ? path.join(distRoot, 'browser') : distRoot;

if (!fs.existsSync(browserDir)) {
  console.error(`No build output found at ${browserDir}.`);
  process.exit(1);
}

function walk(dir, acc) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, acc);
    } else {
      acc.push(full);
    }
  }
  return acc;
}

const allFiles = walk(browserDir, []);
const jsFiles = allFiles
  .filter((f) => f.endsWith('.js') && !f.endsWith('.js.map'))
  .map((f) => ({ file: path.relative(browserDir, f), bytes: fs.statSync(f).size }))
  .sort((a, b) => b.bytes - a.bytes);

const totalJsBytes = jsFiles.reduce((sum, f) => sum + f.bytes, 0);
const totalDistBytes = allFiles.reduce((sum, f) => sum + fs.statSync(f).size, 0);

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal — report still useful without a commit reference
}

const initialErrorBytes = initialBudget ? parseSizeToBytes(initialBudget.maximumError) : null;
const initialWarningBytes = initialBudget ? parseSizeToBytes(initialBudget.maximumWarning) : null;
let budgetStatus = 'no budget configured, or "Initial total" not found in build output';
if (initialTotalBytes !== null && initialBudget) {
  if (initialErrorBytes !== null && initialTotalBytes > initialErrorBytes) {
    budgetStatus = `OVER ERROR budget (${initialBudget.maximumError})`;
  } else if (initialWarningBytes !== null && initialTotalBytes > initialWarningBytes) {
    budgetStatus = `over WARNING budget (${initialBudget.maximumWarning})`;
  } else {
    budgetStatus = `within budget (warning at ${initialBudget.maximumWarning}, error at ${initialBudget.maximumError})`;
  }
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();

const lines = [];
lines.push(`# Bundle Size Report — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push(`**Output path:** ${outputPath}`);
lines.push('');
lines.push(
  `**Initial bundle size:** ${initialTotalBytes !== null ? (initialTotalBytes / 1000).toFixed(1) + ' kB' : 'unknown'} (main + eagerly-loaded chunks — what every route pays on first load)`
);
lines.push(`**Budget status:** ${budgetStatus}`);
lines.push(`**Total JS shipped (initial + all lazy routes, excluding source maps):** ${(totalJsBytes / 1000).toFixed(1)} kB`);
lines.push(`**Total dist size (all files):** ${(totalDistBytes / 1000).toFixed(1)} kB`);
lines.push('');
lines.push('## Largest 10 JS chunks (initial + lazy combined)');
lines.push('');
lines.push('| File | Size (kB) |');
lines.push('| --- | --- |');
for (const f of jsFiles.slice(0, 10)) {
  lines.push(`| ${f.file} | ${(f.bytes / 1000).toFixed(1)} |`);
}
lines.push('');

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('../..');
  }
}

const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/bundle-size');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `BUNDLE_SIZE_REPORT_${date}.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(lines.join('\n'));
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
