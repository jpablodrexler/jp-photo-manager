#!/usr/bin/env node
// Measures the size of this project's CLAUDE.md files (lines, words, bytes)
// and writes a dated snapshot report under docs/reports/claude-md-size/, the
// same convention every other quality-metric script here uses (see
// code-coverage-report.js). Tracked as its own metric because CLAUDE.md is
// the primary onboarding/context document Claude Code reads every session —
// unlike code files, nothing else measures its growth, and an unchecked
// creep here (a new paragraph added per feature, nothing ever trimmed) is
// easy to miss precisely because it never fails a build or a lint rule.
//
// This repo has two CLAUDE.md files: the repo-root one documents the legacy
// WPF desktop app (JPPhotoManager/) and rarely changes; JPPhotoManagerWeb/CLAUDE.md
// documents this web rewrite and is the one that actually grows with feature
// work (it's the counterpart pablo-web's CLAUDE.md is paired with — see the
// skills-docs-sync skill). The headline metric is JPPhotoManagerWeb/CLAUDE.md;
// the root one is reported alongside for completeness, not tracked as the
// headline trend.
//
// Deliberately no "size limit"/pass-fail threshold: this is an
// informational trend metric (see quality-metrics skill's "Informational
// only" bucket), not a gate — a size increase can be entirely justified by
// real new architecture. The value is in the trend, not a single reading.
//
// Usage: node scripts/claude-md-size-report.js (or `npm run claude-md-size:report`)

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

function measure(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const bytes = Buffer.byteLength(content, 'utf8');
  const lines = content.split('\n').length;
  const words = content.trim().length === 0 ? 0 : content.trim().split(/\s+/).length;
  const headingCount = (content.match(/^#{1,6} /gm) || []).length;
  return { lines, words, bytes, headingCount };
}

const repoRoot = findRepoRoot();
const webClaudeMdPath = path.join(repoRoot, 'JPPhotoManagerWeb', 'CLAUDE.md');
const rootClaudeMdPath = path.join(repoRoot, 'CLAUDE.md');

const web = measure(webClaudeMdPath);
const root = fs.existsSync(rootClaudeMdPath) ? measure(rootClaudeMdPath) : null;

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();

const lineOut = [];
lineOut.push(`# CLAUDE.md Size Report — ${date}`);
lineOut.push('');
lineOut.push(`**Commit:** ${gitCommit}`);
lineOut.push(`**Generated:** ${timestamp}`);
lineOut.push('**Scope:** `JPPhotoManagerWeb/CLAUDE.md` (headline — this web project\'s guidance file) and the repo-root `CLAUDE.md` (the legacy WPF desktop app\'s guidance file, reported for completeness)');
lineOut.push('');
lineOut.push(`**Lines:** ${web.lines}`);
lineOut.push(`**Words:** ${web.words}`);
lineOut.push(`**Size:** ${web.bytes} bytes (${(web.bytes / 1024).toFixed(2)} KB)`);
lineOut.push(`**Headings:** ${web.headingCount}`);
lineOut.push('');
if (root) {
  lineOut.push('### Repo-root CLAUDE.md (WPF desktop app, informational only)');
  lineOut.push('');
  lineOut.push(`**Lines:** ${root.lines}`);
  lineOut.push(`**Words:** ${root.words}`);
  lineOut.push(`**Size:** ${root.bytes} bytes (${(root.bytes / 1024).toFixed(2)} KB)`);
  lineOut.push(`**Headings:** ${root.headingCount}`);
  lineOut.push('');
}
lineOut.push('This is an informational trend metric with no pass/fail threshold — see the quality-metrics skill\'s "Informational only" bucket. A steady climb across several runs with no corresponding trim is the signal worth a look, not any single reading: it usually means detail that belongs in `docs/*.md` got left inline instead.');
lineOut.push('');

const reportDir = path.join(repoRoot, 'JPPhotoManagerWeb', 'docs', 'reports', 'claude-md-size');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `CLAUDE_MD_SIZE_REPORT_${date}.md`);
fs.writeFileSync(reportPath, lineOut.join('\n'));

console.log(`JPPhotoManagerWeb/CLAUDE.md: ${web.lines} lines, ${web.words} words, ${web.bytes} bytes.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);

process.exit(0);
