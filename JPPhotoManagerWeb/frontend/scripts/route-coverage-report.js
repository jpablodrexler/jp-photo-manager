#!/usr/bin/env node
// Reports whether every route declared in app.routes.ts has at least one
// literal cy.visit('/path') across either E2E tier (the real-backend suite
// under cypress/e2e/, or the mocked smoke tier under cypress/e2e/mocked/) —
// so a route that quietly lost its only E2E spec is visible as a gap
// instead of a silent omission. Writes a dated snapshot report under
// JPPhotoManagerWeb/docs/reports/route-coverage/, the same convention
// bundle-size-report.js/dependency-staleness-report.js use.
//
// This backend has no RLS-equivalent database mechanism to also report on —
// it enforces authorization via Spring Security's SecurityConfig
// (path-pattern matchers) instead, which has its own analog:
// backend/scripts/auth-coverage-report.sh.
//
// Usage: node scripts/route-coverage-report.js (or
// `npm run route-coverage:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('..');
  }
}

const repoRoot = findRepoRoot();

const routesFile = path.resolve('src/app/app.routes.ts');
const routesSrc = fs.readFileSync(routesFile, 'utf8');
const routePaths = [...routesSrc.matchAll(/path:\s*'([^']*)'/g)]
  .map((m) => m[1])
  .filter((p) => p !== ''); // skip the empty-path redirect-to-home entry, not a distinct route

function walkFiles(dir, pattern, acc) {
  if (!fs.existsSync(dir)) return acc;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walkFiles(full, pattern, acc);
    else if (pattern.test(entry.name)) acc.push(full);
  }
  return acc;
}

const e2eRoot = path.resolve('cypress/e2e');
const mockedRoot = path.resolve('cypress/e2e/mocked');
const allSpecFiles = walkFiles(e2eRoot, /\.cy\.ts$/, []);

// The real-backend tier navigates via a raw cy.visit('/path'); the mocked
// tier wraps that in visitWithSession('/path', role?) (cypress/support/
// mocked/seed-session.ts) to fabricate an authenticated session first —
// both need matching, or every mocked-tier spec looks like it visits
// nothing at all.
function visitedPaths(files) {
  const visited = new Set();
  for (const file of files) {
    const src = fs.readFileSync(file, 'utf8');
    // Only literal string visits are resolvable statically.
    for (const m of src.matchAll(/(?:cy\.visit|visitWithSession)\(\s*['"]([^'"]+)['"]/g)) {
      visited.add(m[1].split('?')[0].replace(/^\//, ''));
    }
  }
  return visited;
}

const realSpecFiles = allSpecFiles.filter((f) => !f.startsWith(mockedRoot));
const mockedSpecFiles = allSpecFiles.filter((f) => f.startsWith(mockedRoot));
const visitedReal = visitedPaths(realSpecFiles);
const visitedMocked = visitedPaths(mockedSpecFiles);

// A parameterized route (albums/:id) never appears verbatim in a cy.visit()
// call — a test visits a real id instead (e.g. cy.visit('albums/abc-123'))
// — so match it by prefix up to the first ':' segment rather than requiring
// an exact string match.
function isRouteVisited(routePath, visitedSet) {
  if (visitedSet.has(routePath)) return true;
  if (routePath.includes(':')) {
    const staticPrefix = routePath.slice(0, routePath.indexOf(':')).replace(/\/$/, '');
    return [...visitedSet].some((v) => v.startsWith(staticPrefix + '/') || v === staticPrefix);
  }
  return false;
}

const routeRows = routePaths
  .map((p) => ({
    path: p,
    real: isRouteVisited(p, visitedReal),
    mocked: isRouteVisited(p, visitedMocked),
  }))
  .sort((a, b) => a.path.localeCompare(b.path));
const uncoveredRoutes = routeRows.filter((r) => !r.real && !r.mocked);

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
} catch {
  // not fatal
}

const date = new Date().toISOString().slice(0, 10);
const timestamp = new Date().toISOString();

const lines = [];
lines.push(`# Route E2E Coverage Report (frontend) — ${date}`);
lines.push('');
lines.push(`**Commit:** ${gitCommit}`);
lines.push(`**Generated:** ${timestamp}`);
lines.push(`**Scope:** ${routePaths.length} declared routes; ${allSpecFiles.length} E2E spec files (${realSpecFiles.length} real-backend, ${mockedSpecFiles.length} mocked)`);
lines.push('');
lines.push('Whether each declared route has at least one literal `cy.visit(...)` in either E2E tier. A route built from a variable rather than a string literal won\'t be detected by this scan; a parameterized route (`albums/:id`) is matched by its static path prefix instead of an exact string.');
lines.push('');
lines.push('| Route | Real-backend tier | Mocked tier |');
lines.push('| --- | --- | --- |');
for (const r of routeRows) {
  lines.push(`| /${r.path} | ${r.real ? '✓' : '—'} | ${r.mocked ? '✓' : '—'} |`);
}
lines.push('');
lines.push(`**Routes with no E2E coverage in either tier:** ${uncoveredRoutes.length === 0 ? 'none' : uncoveredRoutes.map((r) => '/' + r.path).join(', ')}`);
lines.push('');

const reportDir = path.resolve(repoRoot, 'JPPhotoManagerWeb/docs/reports/route-coverage');
fs.mkdirSync(reportDir, { recursive: true });
const reportPath = path.join(reportDir, `ROUTE_COVERAGE_REPORT_${date}_frontend.md`);
fs.writeFileSync(reportPath, lines.join('\n'));

console.log(`${routeRows.length} routes analyzed, ${uncoveredRoutes.length} with no E2E coverage.`);
console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);
