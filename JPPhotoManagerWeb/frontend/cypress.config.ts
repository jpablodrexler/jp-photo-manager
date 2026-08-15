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
    setupNodeEvents(_on, _config) {},
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
