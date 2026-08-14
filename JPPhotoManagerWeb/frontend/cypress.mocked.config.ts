import { defineConfig } from 'cypress';

// Dedicated Cypress config for the mocked E2E smoke tier
// (cypress/e2e/mocked/**) - deliberately separate from cypress.config.ts's
// own `e2e` block. cypress.config.ts's specPattern ('cypress/e2e/**/*.cy.ts')
// already matches this mocked/ subdirectory too, and Cypress applies
// excludeSpecPattern *before* spec selection either way - excluding
// cypress/e2e/mocked/** there would also silently block a targeted
// `--spec cypress/e2e/mocked/**` run against that same config. A separate
// config file avoids that trap entirely.
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/mocked/**/*.cy.ts',
    // Reuses the same support file as the real-backend e2e block - it only
    // wires shared custom commands, none of which require a live backend.
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(_on, _config) {},
  },
});
