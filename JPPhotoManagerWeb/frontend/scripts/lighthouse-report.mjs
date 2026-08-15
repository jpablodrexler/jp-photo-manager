#!/usr/bin/env node
// Runs Lighthouse (performance, accessibility, best-practices, SEO) against
// this app's one publicly-reachable route — /login is the only route
// navigable without an authenticated session (every other route is behind
// authGuard; there's no self-registration flow, accounts are admin-created
// via /admin/users), and Lighthouse has no equivalent to the mocked E2E
// tier's cy.intercept-based session fabrication, so authenticated routes
// aren't covered here. Nothing today checks either performance or
// accessibility for this app; Angular Material doesn't guarantee either
// for free, so this is a real gap this report starts closing, even at
// reduced (single-route) scope.
//
// Builds and serves the production bundle itself (like bundle-size-
// report.js does) rather than auditing `ng serve`'s dev build — a dev
// build's unminified, unoptimized output makes the performance score
// meaningless as a real-world signal. The static server is a minimal
// hand-rolled one (Node's http module only, no new dependency) with an
// SPA fallback: any path that isn't a real file under dist/ serves
// index.html, matching how the router's client-side route (/login) would
// behave for any real static host serving this app.
//
// Written as ESM (.mjs, not .js like this project's other report scripts)
// because lighthouse and chrome-launcher are both ESM-only packages
// ("type": "module" in their own package.json) — everything else in
// scripts/ is CommonJS and can stay that way.
//
// Usage: node scripts/lighthouse-report.mjs (or `npm run lighthouse:report`)

import fs from 'fs';
import path from 'path';
import http from 'http';
import { execSync } from 'child_process';
import lighthouse from 'lighthouse';
import * as chromeLauncher from 'chrome-launcher';

const ROUTES = ['/login'];
const CATEGORIES = ['performance', 'accessibility', 'best-practices', 'seo'];
const PORT = 4310; // arbitrary, unlikely-to-collide port for this script's own throwaway static server

// chrome-launcher only auto-detects an actual Google Chrome/Chromium
// install; this machine (and possibly others this report runs on) has
// Microsoft Edge instead, which is Chromium-based and works identically for
// headless auditing. Fall back to it via chrome-launcher's own CHROME_PATH
// override rather than failing with ChromeNotInstalledError.
if (!process.env.CHROME_PATH) {
  const edgeCandidates = [
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  ];
  const edgePath = edgeCandidates.find((p) => fs.existsSync(p));
  if (edgePath) process.env.CHROME_PATH = edgePath;
}

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('..');
  }
}

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
};

function serveDist(browserDir) {
  const server = http.createServer((req, res) => {
    const urlPath = decodeURIComponent(req.url.split('?')[0]);
    let filePath = path.join(browserDir, urlPath);
    if (!filePath.startsWith(browserDir) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      filePath = path.join(browserDir, 'index.html'); // SPA fallback for client-side routes
    }
    const ext = path.extname(filePath);
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
  });
  return new Promise((resolve) => server.listen(PORT, () => resolve(server)));
}

async function auditRoute(chrome, baseUrl, route) {
  const result = await lighthouse(`${baseUrl}${route}`, {
    logLevel: 'error',
    output: 'json',
    onlyCategories: CATEGORIES,
    port: chrome.port,
  });
  const { categories, audits } = result.lhr;
  const scores = Object.fromEntries(CATEGORIES.map((c) => [c, Math.round((categories[c]?.score ?? 0) * 100)]));

  // Accessibility failures are the most actionable finding this report can
  // surface (performance/best-practices/SEO scores are useful trend numbers
  // but rarely point at a single fixable line the way an a11y audit does) —
  // list every failed a11y audit by name, not just the rolled-up score.
  const failedA11yAudits = (categories.accessibility?.auditRefs || [])
    .map((ref) => audits[ref.id])
    .filter((a) => a && a.score !== null && a.score < 1 && a.scoreDisplayMode !== 'notApplicable')
    .map((a) => ({ id: a.id, title: a.title }));

  return { route, scores, failedA11yAudits };
}

async function main() {
  const repoRoot = findRepoRoot();
  const angularJson = JSON.parse(fs.readFileSync(path.resolve('angular.json'), 'utf8'));
  const projectName = Object.keys(angularJson.projects)[0];
  const buildConfig = angularJson.projects[projectName].architect.build;
  const outputPath = buildConfig.options.outputPath || `dist/${projectName}`;
  const distRoot = path.resolve(typeof outputPath === 'string' ? outputPath : outputPath.base);
  const browserDir = fs.existsSync(path.join(distRoot, 'browser')) ? path.join(distRoot, 'browser') : distRoot;

  console.log('Running production build...');
  execSync('npx ng build --configuration production', { stdio: 'inherit' });

  if (!fs.existsSync(browserDir)) {
    console.error(`No build output found at ${browserDir}.`);
    process.exit(1);
  }

  const server = await serveDist(browserDir);
  const baseUrl = `http://localhost:${PORT}`;

  let results;
  const chrome = await chromeLauncher.launch({ chromeFlags: ['--headless=new', '--no-sandbox'] });
  try {
    results = [];
    for (const route of ROUTES) {
      results.push(await auditRoute(chrome, baseUrl, route));
    }
  } finally {
    await chrome.kill();
    server.close();
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
  lines.push(`# Lighthouse Report (frontend) — ${date}`);
  lines.push('');
  lines.push(`**Commit:** ${gitCommit}`);
  lines.push(`**Generated:** ${timestamp}`);
  lines.push('**Build:** production (`ng build --configuration production`), self-hosted — not the `ng serve` dev build, so scores reflect real optimized output.');
  lines.push(`**Scope:** ${ROUTES.join(', ')} — the only route reachable without an authenticated session; every other route requires signing in first, which this report doesn't attempt to simulate.`);
  lines.push('');
  lines.push('| Route | Performance | Accessibility | Best Practices | SEO |');
  lines.push('| --- | --- | --- | --- | --- |');
  for (const r of results) {
    lines.push(`| ${r.route} | ${r.scores.performance} | ${r.scores.accessibility} | ${r.scores['best-practices']} | ${r.scores.seo} |`);
  }
  lines.push('');

  const totalA11yFailures = results.reduce((sum, r) => sum + r.failedA11yAudits.length, 0);
  if (totalA11yFailures > 0) {
    lines.push('## Accessibility audit failures');
    lines.push('');
    lines.push('| Route | Audit |');
    lines.push('| --- | --- |');
    for (const r of results) {
      for (const a of r.failedA11yAudits) {
        lines.push(`| ${r.route} | ${a.title} (\`${a.id}\`) |`);
      }
    }
    lines.push('');
  } else {
    lines.push('No accessibility audit failures on the scanned route.');
    lines.push('');
  }

  const reportDir = path.resolve(repoRoot, 'JPPhotoManagerWeb/docs/reports/lighthouse');
  fs.mkdirSync(reportDir, { recursive: true });
  const reportPath = path.join(reportDir, `LIGHTHOUSE_REPORT_${date}_frontend.md`);
  fs.writeFileSync(reportPath, lines.join('\n'));

  for (const r of results) {
    console.log(`${r.route}: perf=${r.scores.performance} a11y=${r.scores.accessibility} best-practices=${r.scores['best-practices']} seo=${r.scores.seo}`);
  }
  console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
