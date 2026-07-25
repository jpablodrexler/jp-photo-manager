import { defineConfig } from 'cypress';
import codeCoverage from '@cypress/code-coverage/task';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    // The release-e2e-suite specs (cypress/e2e/release/**) hit a real
    // backend with real login and real data seeding/teardown — they must
    // never run against whatever's on localhost:4200 (this config's default
    // baseUrl) just because someone ran a bare `npm run test:e2e`/
    // `cypress open --e2e` locally. Excluded here from both the default run
    // AND from explicit --spec (Cypress applies excludeSpecPattern before
    // spec selection either way, so --spec can't bypass it) — the release
    // suite is instead run through its own dedicated
    // cypress.release.config.ts, never through this file.
    excludeSpecPattern: 'cypress/e2e/release/**/*.cy.ts',
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
