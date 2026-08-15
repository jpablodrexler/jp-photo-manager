#!/usr/bin/env node
// Runs a Cypress E2E tier via the Module API (rather than the plain CLI) so
// this can capture structured per-test timing and retry-attempt data —
// runtime and flakiness aren't visible from the CLI's console output alone.
// Retries are enabled only for this invocation (via a config override), not
// baked into the shared cypress config files the CI-facing test:e2e* scripts
// use — a test that needs more than one attempt to pass is what "flaky"
// means here, and without retries enabled Cypress never gives it that
// second attempt to observe.
//
// Usage: node scripts/e2e-run-report.js --mocked   (cypress.mocked.config.ts)
//        node scripts/e2e-run-report.js --real      (cypress.config.ts — needs
//        the real backend/Postgres/etc. running, see the e2e-testing skill)
// Expects the frontend to already be reachable at the configured baseUrl —
// the test:e2e*:report npm scripts wrap the mocked tier with
// start-server-and-test; the real tier is invoked directly, matching how
// test:e2e already works in this project.

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const cypress = require('cypress');

const tier = process.argv.includes('--real') ? 'real' : 'mocked';
const configFile = tier === 'real' ? 'cypress.config.ts' : 'cypress.mocked.config.ts';

function findRepoRoot() {
  try {
    return execSync('git rev-parse --show-toplevel', { encoding: 'utf8' }).trim();
  } catch {
    return path.resolve('../..');
  }
}

async function main() {
  const result = await cypress.run({
    configFile,
    config: { retries: { runMode: 2, openMode: 0 } },
  });

  if (result.status === 'failed') {
    console.error(`Cypress itself failed to run: ${result.message}`);
    process.exit(1);
  }

  const specs = (result.runs || []).map((run) => {
    // Flaky = eventually passed but needed more than one attempt. A test
    // that fails on every attempt isn't flaky, it's just failing — don't
    // conflate the two.
    const flakyTests = run.tests.filter((t) => t.state === 'passed' && (t.attempts || []).length > 1);
    return {
      spec: run.spec.relative,
      duration: run.stats.duration,
      tests: run.stats.tests,
      passes: run.stats.passes,
      failures: run.stats.failures,
      flaky: flakyTests.map((t) => ({ title: t.title.join(' > '), attempts: t.attempts.length })),
    };
  });

  const totalFlaky = specs.reduce((sum, s) => sum + s.flaky.length, 0);
  const slowest = [...specs].sort((a, b) => b.duration - a.duration).slice(0, 5);

  let gitCommit = 'unknown';
  try {
    gitCommit = execSync('git rev-parse --short HEAD', { encoding: 'utf8' }).trim();
  } catch {
    // not fatal
  }

  const date = new Date().toISOString().slice(0, 10);
  const timestamp = new Date().toISOString();

  const lines = [];
  lines.push(`# E2E Run Report (${tier}) — ${date}`);
  lines.push('');
  lines.push(`**Commit:** ${gitCommit}`);
  lines.push(`**Generated:** ${timestamp}`);
  lines.push(`**Total duration:** ${(result.totalDuration / 1000).toFixed(1)}s`);
  lines.push(
    `**Tests:** ${result.totalTests} (${result.totalPassed} passed, ${result.totalFailed} failed, ${result.totalSkipped} skipped)`
  );
  lines.push(`**Flaky tests (needed a retry to pass):** ${totalFlaky}`);
  lines.push('');
  lines.push('## Slowest 5 specs');
  lines.push('');
  lines.push('| Spec | Duration (s) | Tests |');
  lines.push('| --- | --- | --- |');
  for (const s of slowest) {
    lines.push(`| ${s.spec} | ${(s.duration / 1000).toFixed(1)} | ${s.tests} |`);
  }
  lines.push('');
  if (totalFlaky > 0) {
    lines.push('## Flaky tests');
    lines.push('');
    lines.push('| Spec | Test | Attempts needed |');
    lines.push('| --- | --- | --- |');
    for (const s of specs) {
      for (const f of s.flaky) {
        lines.push(`| ${s.spec} | ${f.title} | ${f.attempts} |`);
      }
    }
    lines.push('');
  }

  const reportDir = path.resolve(findRepoRoot(), 'JPPhotoManagerWeb/docs/reports/e2e-run');
  fs.mkdirSync(reportDir, { recursive: true });
  const reportPath = path.join(reportDir, `E2E_RUN_REPORT_${date}_${tier}.md`);
  fs.writeFileSync(reportPath, lines.join('\n'));

  console.log(lines.join('\n'));
  console.log(`\nReport written to ${path.relative(process.cwd(), reportPath)}`);

  if (result.totalFailed > 0) {
    process.exitCode = 1;
  }
}

main();
