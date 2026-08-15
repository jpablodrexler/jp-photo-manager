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
    excludeSpecPattern: 'cypress/e2e/mocked/**/*.cy.ts',
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
      return config;
    },
  },
});
