// StrykerJS config for mutation testing the frontend's genuinely-branching
// core/ business logic — see scripts/mutation-report.js for the full
// write-up of the two non-obvious things this config exists to work
// around. Quick summary of both, since they explain nearly every unusual
// setting below:
//
// 1. Cypress Component Testing has no native Stryker test-runner plugin
//    (only Jest/Mocha/Karma/Jasmine/Vitest are supported that way), so this
//    uses the generic "command" runner. That runner's mutant-switch
//    instrumentation reads `process.env.__STRYKER_ACTIVE_MUTANT__`, but our
//    code under test runs inside a Cypress *browser* iframe with no Node
//    `process` global — without the bridge in cypress.config.ts +
//    cypress/support/component.ts, every mutant silently "survives" and the
//    mutation score is permanently stuck at 0% regardless of test quality.
// 2. `commandRunner.command` deliberately does NOT run the full
//    `npm run test` suite (~650 tests across ~50 spec files). The command
//    runner reruns the whole configured command per mutant, so `test:mutation`
//    (see package.json) instead runs only the spec files for the files
//    listed in `mutate` below — still exercises every mutated file's own
//    tests, just without ~45 unrelated spec files' dead weight per mutant.
//    Note: Stryker's live "remaining time" estimate during a run is
//    unreliable in the first few percent (it initially extrapolates from
//    the slow first mutant, which pays one-time Electron/npx warm-up cost
//    the rest don't) — don't use an early ETA to judge whether a run is
//    worth letting finish. The full 8-file scope below (553 mutants)
//    actually completed in under 14 minutes end to end, despite an early
//    in-run estimate north of 8 hours.
export default {
  packageManager: 'npm',
  testRunner: 'command',
  commandRunner: {
    command: 'npm run test:mutation',
  },
  // The command runner doesn't support per-test coverage analysis (it has
  // no way to know which tests a given mutant's line is even reachable
  // from beyond just running the whole configured command) — 'off' is the
  // only valid value for this test runner.
  coverageAnalysis: 'off',
  concurrency: 1,
  // Baseline (dry run) took ~50-60s in practice — command runner spawns a
  // full Cypress/Electron process per mutant, so timeoutFactor gives real
  // per-mutant variance (Electron startup jitter) headroom without
  // individually tuning every mutant.
  timeoutMS: 60000,
  timeoutFactor: 3,
  reporters: ['json', 'clear-text', 'progress'],
  // frontend/ has no .gitignore of its own (only the repo-root one, two
  // directories up) — Stryker's default git-ignore-based file selection
  // doesn't find it, and was copying the entire Angular/Vite build cache
  // (.angular/**) into every sandbox. Explicit exclusions here avoid that
  // wasted I/O regardless of what else does or doesn't have a nearby
  // .gitignore.
  files: [
    '**/*',
    '!.angular/**',
    '!coverage/**',
    '!dist/**',
    '!reports/**',
    '!.nyc_output/**',
    '!.stryker-tmp/**',
    '!cypress/screenshots/**',
    '!cypress/videos/**',
    '!node_modules/**',
  ],
  // Curated to genuinely-branching business-logic files under core/ —
  // deliberately excludes core/'s other ~10 services (album, analytics,
  // convert, home, preference, recycle-bin, search-preset, sync, tag,
  // user-admin), which are thin HTTP CRUD wrappers with no real
  // conditionals per a manual scan (cyclomatic-complexity.js's
  // top-functions output plus a grep for if/catch/switch/&&/||/?. density
  // per file — see the code-reviewer skill's mutation-testing section).
  mutate: [
    'src/app/core/services/auth.service.ts',
    'src/app/core/services/asset.service.ts',
    'src/app/core/services/media-player.service.ts',
    'src/app/core/services/background-sync.service.ts',
    'src/app/core/services/theme.service.ts',
    'src/app/core/services/folder.service.ts',
    'src/app/core/interceptors/auth.interceptor.ts',
    'src/app/core/error-handler/global-error-handler.ts',
  ],
  tempDirName: '.stryker-tmp',
  cleanTempDir: true,
};
