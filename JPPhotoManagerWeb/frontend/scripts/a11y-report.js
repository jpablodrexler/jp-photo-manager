#!/usr/bin/env node
// Runs the deep, per-route accessibility (a11y) audit
// (cypress/e2e/a11y/a11y-audit.cy.ts) and writes a dated markdown report —
// the same convention every other scripts/*-report.js in this project
// follows. This is the complement to scripts/lighthouse-report.mjs's
// Lighthouse accessibility SCORE (a single aggregate number for /login
// only): this report gets the actual WCAG rule violated, impact level,
// affected selector, and help URL, for every authenticated route.
//
// Deliberately named "a11y:report" in package.json, NOT "coverage:report" —
// @cypress/code-coverage's Node task hardcodes that exact npm-script name as
// its own auto-invoked custom-report-script convention, so a script named
// "coverage:report" gets auto-run by the plugin after every spec. That
// caused a real ~2-hour CI outage in this repo before being traced back to
// the name collision; this script's name avoids it entirely.
//
// Expects the frontend to already be reachable at cypress.a11y.config.ts's
// baseUrl (http://localhost:4200) — same prerequisite as the mocked E2E
// tier (`npm start` in another terminal, or wrap this script's invocation
// with start-server-and-test yourself), not self-served like
// lighthouse-report.mjs/bundle-size-report.js are, because this audit needs
// cypress-axe driving a real Cypress browser session, not a static-file
// server.
//
// Usage: node scripts/a11y-report.js (or `npm run a11y:report`)

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ROUTES = [
  '/home',
  '/gallery',
  '/sync',
  '/convert',
  '/duplicates',
  '/admin/users',
  '/albums',
  '/albums/1',
  '/recycle-bin',
  '/analytics',
  '/profile/sessions',
];

const RESULTS_PATH = path.resolve('cypress/a11y-results/raw.json');
const IMPACT_ORDER = ['critical', 'serious', 'moderate', 'minor', 'unknown'];

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('../..');
  }
}

function runAuditSpec() {
  // A failing spec (e.g. skipFailures didn't apply to a Cypress-level error,
  // or the dev server wasn't reachable) is still a finding worth reporting,
  // not a reason to crash this script — the try/catch below is deliberate,
  // not an oversight. cy.checkA11y's own skipFailures:true already keeps a
  // real a11y violation from failing the spec; this catch is the outer
  // safety net for anything else that could make Cypress exit non-zero.
  try {
    execSync('npx cypress run --e2e --config-file cypress.a11y.config.ts', { stdio: 'inherit' });
  } catch (err) {
    console.warn(`\nCypress exited non-zero (${err.status ?? 'unknown status'}) — continuing to read whatever results were written.`);
  }
}

function readResults() {
  if (!fs.existsSync(RESULTS_PATH)) {
    console.error(`No results file found at ${RESULTS_PATH} — the audit spec did not run to completion.`);
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(RESULTS_PATH, 'utf8'));
  } catch (err) {
    console.error(`Failed to parse ${RESULTS_PATH}: ${err.message}`);
    return null;
  }
}

function main() {
  console.log('Running accessibility audit spec (cypress/e2e/a11y/a11y-audit.cy.ts)...');
  runAuditSpec();

  const results = readResults() || [];

  const countsByImpact = Object.fromEntries(IMPACT_ORDER.map((i) => [i, 0]));
  let totalViolations = 0;
  for (const routeResult of results) {
    for (const v of routeResult.violations) {
      const impact = IMPACT_ORDER.includes(v.impact) ? v.impact : 'unknown';
      countsByImpact[impact] += 1;
      totalViolations += 1;
    }
  }

  let gitCommit = 'unknown';
  try {
    gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
  } catch {
    // not fatal — report still useful without a commit reference
  }

  const date = new Date().toISOString().slice(0, 10);
  const timestamp = new Date().toISOString();

  const lines = [];
  lines.push(`# Accessibility Audit Report (frontend) — ${date}`);
  lines.push('');
  lines.push(`**Commit:** ${gitCommit}`);
  lines.push(`**Generated:** ${timestamp}`);
  lines.push(
    `**Scope:** ${ROUTES.length} authenticated routes (cypress-axe/axe-core, mocked session + intercepted \`/api/**\` calls — no live backend), the deep complement to \`npm run lighthouse:report\`'s Lighthouse accessibility score for \`/login\`: ${ROUTES.join(', ')}.`
  );
  lines.push('');

  if (results.length === 0) {
    lines.push(
      'No results were captured — the audit spec did not finish (see the console output above for the Cypress failure). This is itself a finding: re-run `npm run a11y:report` with the dev server (`npm start`) reachable at http://localhost:4200 first.'
    );
    lines.push('');
  } else {
    lines.push(`**Total violations:** ${totalViolations}`);
    lines.push('');
    lines.push('## Violations by impact');
    lines.push('');
    lines.push('| Impact | Count |');
    lines.push('| --- | --- |');
    for (const impact of IMPACT_ORDER) {
      if (countsByImpact[impact] > 0 || impact !== 'unknown') {
        lines.push(`| ${impact} | ${countsByImpact[impact]} |`);
      }
    }
    lines.push('');

    if (totalViolations === 0) {
      lines.push('No accessibility violations found on any audited route.');
      lines.push('');
    } else {
      lines.push('## Violations by route');
      lines.push('');
      for (const routeResult of results) {
        if (routeResult.violations.length === 0) continue;
        lines.push(`### \`${routeResult.route}\` (${routeResult.violations.length})`);
        lines.push('');
        lines.push('| Rule | Impact | Nodes | Description | Help |');
        lines.push('| --- | --- | --- | --- | --- |');
        for (const v of routeResult.violations) {
          const targetsPreview = v.targets.slice(0, 3).join('; ') + (v.targets.length > 3 ? `; +${v.targets.length - 3} more` : '');
          lines.push(
            `| \`${v.id}\` | ${v.impact} | ${v.nodeCount} (${targetsPreview}) | ${v.description} | [${v.help}](${v.helpUrl}) |`
          );
        }
        lines.push('');
      }

      const cleanRoutes = results.filter((r) => r.violations.length === 0).map((r) => r.route);
      if (cleanRoutes.length > 0) {
        lines.push(`## Routes with no violations`);
        lines.push('');
        lines.push(cleanRoutes.map((r) => `\`${r}\``).join(', '));
        lines.push('');
      }
    }
  }

  const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/a11y');
  fs.mkdirSync(reportDir, { recursive: true });
  const reportPath = path.join(reportDir, `A11Y_REPORT_${date}_frontend.md`);
  fs.writeFileSync(reportPath, lines.join('\n'));

  console.log(`\nTotal violations: ${totalViolations}`);
  console.log(`Report written to ${path.relative(process.cwd(), reportPath)}`);

  if (results.length === 0) {
    process.exitCode = 1;
  }
}

main();
