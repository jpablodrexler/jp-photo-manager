import { defineConfig } from 'cypress';
import codeCoverage from '@cypress/code-coverage/task';

export default defineConfig({
  e2e: {
    // Points at the app deployed to the local Kubernetes cluster (via
    // k8s/ingress.yaml + a "127.0.0.1 photomanager.local" hosts-file
    // entry), not a local `ng serve` dev server — see the e2e-suite
    // skill §1 for why and how to redeploy before running this tier.
    baseUrl: 'http://photomanager.local',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    // The mocked E2E smoke tier (cypress/e2e/mocked/**) runs through its own
    // dedicated cypress.mocked.config.ts, never through this config - but
    // this config's specPattern above would still silently match those files
    // too without this exclude, since '**' covers the mocked/ subdirectory.
    // Same reasoning for cypress/e2e/a11y/** and its own dedicated
    // cypress.a11y.config.ts (see that file) - the a11y audit spec must never
    // run under the real-backend tier.
    excludeSpecPattern: ['cypress/e2e/mocked/**/*.cy.ts', 'cypress/e2e/a11y/**/*.cy.ts'],
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(on, config) {
      // Cypress's Launchpad can switch testing type (E2E -> Component)
      // mid-session without restarting the Node plugin process. If that
      // process booted in E2E mode, a component spec's coverage-collecting
      // cy.task() calls (resetCoverage/combineCoverage/coverageReport) would
      // fail because they were only registered in the component block below.
      // codeCoverage() only registers those task handlers — it wires up no
      // instrumentation or reporting on its own — so calling it here is safe
      // even though E2E specs never send coverage data.
      codeCoverage(on, config);
      return config;
    },
  },
  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack',
      webpackConfig: {
        module: {
          rules: [
            {
              test: /\.(ts|js)$/,
              use: {
                loader: 'babel-loader',
                options: {
                  plugins: ['babel-plugin-istanbul'],
                  presets: [],
                },
              },
              enforce: 'post',
              exclude: /\.(cy|spec)\.(ts|js)$|node_modules/,
            },
          ],
        },
      },
    },
    specPattern: 'src/**/*.cy.ts',
    supportFile: 'cypress/support/component.ts',
    indexHtmlFile: 'cypress/support/component-index.html',
    setupNodeEvents(on, config) {
      codeCoverage(on, config);
      // Forward Stryker's per-mutant env var so cypress/support/component.ts
      // can bridge it into the browser iframe — see that file's comment.
      if (process.env.__STRYKER_ACTIVE_MUTANT__) {
        config.env.STRYKER_ACTIVE_MUTANT = process.env.__STRYKER_ACTIVE_MUTANT__;
      }
      return config;
    },
  },
});
