#!/usr/bin/env node
// Runs every frontend dated quality-metrics report script back to back (the
// "Running every metric manually" list in the root README's Quality Metrics
// section), so there's one command instead of copy-pasting each
// `npm run *:report` line by hand. Each underlying script already writes
// its own dated file under docs/reports/<category>/ and is safe to re-run
// on its own — this is just a sequencing convenience, not a new report of
// its own. The backend (Java) equivalent is
// backend/scripts/run-all-quality-reports.sh, which does the same thing
// over the bash report scripts in backend/scripts/.
//
// Two reports need something this script can't provide on its own and are
// skipped by default:
//   - test:e2e:report needs the full app already deployed (k8s — see the
//     e2e-suite skill §1) rather than a local dev server this script could
//     start itself — pass --with-e2e-real to include it once that's set up.
//   - mutation:report reruns the component suite once per mutant and is by
//     far the slowest report here (minutes, not seconds) — pass
//     --with-mutation to include it.
// Everything else (including the mocked E2E tier, which needs no live
// backend) runs by default.
//
// A failing report does not stop the run — every remaining report still
// gets a chance to produce its snapshot. The script exits non-zero at the
// end if anything failed, so it still works as a CI-style gate if wired
// into one later.
//
// Usage: node scripts/run-all-quality-reports.js [--with-e2e-real] [--with-mutation] [--only=key1,key2]
//        (or `npm run reports:all`)

const { execSync } = require('child_process');

const REPORTS = [
  { key: 'type-coverage', label: 'Type coverage', script: 'type-coverage:report' },
  { key: 'complexity', label: 'Complexity / file size', script: 'complexity:report' },
  { key: 'dead-code', label: 'Dead code', script: 'dead-code:report' },
  { key: 'route-coverage', label: 'Route coverage', script: 'route-coverage:report' },
  { key: 'lighthouse', label: 'Lighthouse (perf/a11y)', script: 'lighthouse:report' },
  { key: 'a11y', label: 'Accessibility audit (axe-core)', script: 'a11y:report' },
  { key: 'code-coverage', label: 'Code coverage trend', script: 'coverage:trend-report' },
  { key: 'bundle-size', label: 'Bundle size', script: 'bundle:report' },
  { key: 'dependency-staleness', label: 'Dependency staleness', script: 'deps:staleness' },
  { key: 'e2e-mocked', label: 'E2E run/flakiness (mocked)', script: 'test:e2e:mocked:report' },
  {
    key: 'e2e-real',
    label: 'E2E run/flakiness (real backend)',
    script: 'test:e2e:report',
    flag: 'with-e2e-real',
    skipReason: 'needs the full app already deployed to k8s (see the e2e-suite skill §1) — pass --with-e2e-real to include',
  },
  { key: 'secrets-scan', label: 'Secrets scanning', script: 'secrets:report' },
  { key: 'license-compliance', label: 'License compliance', script: 'license:report' },
  { key: 'dependency-vulnerabilities', label: 'Dependency vulnerabilities (SCA)', script: 'sca:report' },
  {
    key: 'mutation',
    label: 'Mutation testing',
    script: 'mutation:report',
    flag: 'with-mutation',
    skipReason: 'by far the slowest report (reruns the suite once per mutant) — pass --with-mutation to include',
  },
];

const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith('--')).map((a) => a.replace(/^--/, '').split('=')[0]));
const onlyArg = args.find((a) => a.startsWith('--only='));
const only = onlyArg ? new Set(onlyArg.slice('--only='.length).split(',')) : null;

const results = [];

for (const report of REPORTS) {
  if (only && !only.has(report.key)) {
    continue;
  }
  if (report.flag && !flags.has(report.flag) && !only) {
    console.log(`\n=== Skipping: ${report.label} (${report.skipReason}) ===`);
    results.push({ ...report, status: 'skipped' });
    continue;
  }

  console.log(`\n=== Running: ${report.label} (npm run ${report.script}) ===`);
  try {
    execSync(`npm run ${report.script}`, { stdio: 'inherit' });
    results.push({ ...report, status: 'ok' });
  } catch {
    console.error(`\n!!! ${report.label} failed (npm run ${report.script}) — continuing with the rest.`);
    results.push({ ...report, status: 'failed' });
  }
}

console.log('\n=== Frontend quality metrics report summary ===');
for (const r of results) {
  const marker = r.status === 'ok' ? 'OK    ' : r.status === 'failed' ? 'FAILED' : 'SKIP  ';
  console.log(`${marker}  ${r.label}`);
}

const failedCount = results.filter((r) => r.status === 'failed').length;
if (failedCount > 0) {
  console.error(`\n${failedCount} report(s) failed. See output above for details.`);
  process.exit(1);
}

console.log('\nAll requested reports completed. See docs/reports/<category>/ for the dated output files.');
