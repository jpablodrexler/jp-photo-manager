import { defineConfig } from 'cypress';

// Dedicated Cypress config for the release-e2e-suite skill's real-backend
// regression pass — deliberately separate from cypress.config.ts, whose e2e
// block excludes cypress/e2e/release/** entirely. Two different configs for
// two different philosophies (mocked local dev smoke test vs. real-backend
// release regression) is clearer and less fragile than trying to make one
// config's specPattern/excludeSpecPattern conditionally include a directory
// only for this one command — that was tried first and doesn't work here:
// Cypress applies excludeSpecPattern before spec selection regardless of
// whether specs were requested via specPattern or an explicit --spec glob,
// so excluding cypress/e2e/release/** from cypress.config.ts also silently
// blocks `--spec "cypress/e2e/release/**/*.cy.ts"` against that same file.
//
// baseUrl below is a placeholder — release-e2e-suite always overrides it via
// `--config baseUrl=http://localhost:14200` (the port-forwarded frontend
// Service), matching e2e-testing's port convention. Never run this config
// against the default baseUrl.
//
// Usage (see the release-e2e-suite skill):
//   npx cypress run --config-file cypress.release.config.ts \
//     --config baseUrl=http://localhost:14200
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:14200',
    specPattern: 'cypress/e2e/release/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(_on, _config) {},
  },
});
