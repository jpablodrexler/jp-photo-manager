#!/usr/bin/env node
// Scans the whole repo (not just frontend/) for accidentally-committed
// secrets — API keys, private keys, tokens, high-entropy strings — using
// secretlint (node_modules/.bin/secretlint), and writes a dated snapshot
// report under JPPhotoManagerWeb/docs/reports/secrets-scan/, the same
// convention every other quality-metric script in this repo uses (see
// code-coverage-report.js).
//
// Uses secretlint (npm-installable, no external binary) rather than
// gitleaks/truffleHog specifically so it needs no separate download/PATH
// setup beyond `npm install` — no Go toolchain in this repo already, and
// no reason to introduce one just for this.
//
// SECURITY: this script uses secretlint's `unix` formatter, NOT `json`.
// The json formatter's output includes each scanned file's full, UNMASKED
// sourceContent alongside any findings — not just the findings themselves
// — which on a repo this size produces tens of megabytes of output and,
// worse, means any code path that mishandles that output (a parse-error
// fallback, a truncated buffer) risks echoing real file content. The unix
// formatter (@textlint/linter-formatter's formatters/unix.ts, which
// secretlint's CLI reuses) instead emits one line per *finding* —
// `path:line:col: message [Error/ruleId]` — and nothing at all for a file
// with no findings, so there is no file content in this script's own
// process at any point. Never switch this back to --format json.
//
// Defense in depth beyond the formatter choice: every credential-shaped
// file (JPPhotoManagerWeb/.env, JPPhotoManagerWeb/k8s/secret.yaml,
// JPPhotoManagerWeb/k8s/catalog-volumes.yaml) is also listed explicitly in
// the repo-root .secretlintignore, rather than relying solely on
// secretlint's .gitignore-cascade respect (unreliable when this script's
// cwd differs from the scanned path's own directory tree).
//
// Usage: node scripts/secrets-scan-report.js (or `npm run secrets:report`)

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
const configPath = path.resolve(__dirname, '..', '.secretlintrc.json');
const ignorePath = path.resolve(repoRoot, '.secretlintignore');

console.log('Scanning the repository for secrets with secretlint...');

let output = '';
try {
  // cwd stays frontend/ (where secretlint and its rule preset are actually
  // installed) — npx/module resolution is cwd-based, not glob-target-based,
  // so running this from repoRoot would fail to resolve the preset unless
  // it were also installed there.
  output = execSync(
    `npx secretlint --secretlintrc "${configPath}" --secretlintignore "${ignorePath}" --format unix "${repoRoot.replace(/\\/g, '/')}/**/*"`,
    { encoding: 'utf8', cwd: path.resolve(__dirname, '..'), maxBuffer: 1024 * 1024 * 16 },
  );
} catch (err) {
  // secretlint exits non-zero when it finds anything — that's the normal
  // "findings exist" case, not a crash.
  output = err.stdout ? err.stdout.toString() : '';
}

// unix-formatter line shape: "<path>:<line>:<col>: <message> [Error/<ruleId>]"
const LINE_RE = /^(.+):(\d+):(\d+): (.+) \[(?:Error|Warning|Info)(?:\/(.+))?\]$/;
const findings = [];
for (const line of output.split('\n')) {
  const m = LINE_RE.exec(line);
  if (m) {
    const [, filePath, lineNo, , message, ruleId] = m;
    findings.push({ filePath, line: lineNo, message, ruleId: ruleId || 'unknown' });
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
lines.push(`# Secrets Scan Report — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push('**Scope:** whole repository (secretlint, `.gitignore`-respecting — see `JPPhotoManagerWeb/frontend/.secretlintrc.json` / `.secretlintignore`)');
lines.push('');
lines.push(`**Findings:** ${findings.length} potential secret(s).`);
lines.push('');

if (findings.length > 0) {
  lines.push('| File | Line | Rule | Message |');
  lines.push('| --- | --- | --- | --- |');
  for (const f of findings) {
    const relPath = path.relative(repoRoot, f.filePath).replace(/\\/g, '/');
    lines.push(`| ${relPath} | ${f.line} | ${f.ruleId} | ${f.message.replace(/\|/g, '\\|')} |`);
  }
  lines.push('');
  lines.push('Every finding here needs a human look — secretlint flags high-confidence patterns (AWS keys, private key headers, generic high-entropy tokens, etc.) but a false positive (a placeholder, a test fixture, a documentation example) is possible. Rotate and remove anything real immediately; for a confirmed false positive, scope an ignore rule to that specific file/rule in `JPPhotoManagerWeb/frontend/.secretlintrc.json` rather than disabling the rule project-wide.');
} else {
  lines.push('No secrets detected.');
}
lines.push('');

const reportDir = path.join(repoRoot, 'JPPhotoManagerWeb/docs/reports/secrets-scan');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `SECRETS_SCAN_REPORT_${date}.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`\n${findings.length} potential secret(s) found.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);

process.exit(0);
