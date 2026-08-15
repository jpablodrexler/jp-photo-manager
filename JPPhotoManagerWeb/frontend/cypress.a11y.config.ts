import { defineConfig } from 'cypress';

// Dedicated Cypress config for the accessibility (a11y) audit tier
// (cypress/e2e/a11y/**) - mirrors cypress.mocked.config.ts's shape exactly
// (same baseUrl, same shared support file, same "separate config file"
// reasoning: cypress.config.ts's own specPattern is broad enough to match
// this subdirectory too, and excludeSpecPattern there keeps it out - see
// cypress.config.ts's comment). Kept as its own config file, rather than
// folded into cypress.mocked.config.ts's specPattern, so this tier can be
// run/targeted independently (`cypress run --e2e --config-file
// cypress.a11y.config.ts`) without also re-running the mocked smoke suite,
// and vice versa.
//
// This tier needs no live backend: it drives `ng serve`'s dev server (same
// as the mocked tier) and seeds the same 'photomanager_session' localStorage
// key + cy.intercept stubs via cypress/support/mocked/seed-session.ts to
// reach every authenticated route without a real login.
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/a11y/**/*.cy.ts',
    // Reuses the same support file as the real-backend and mocked tiers -
    // it only wires shared custom commands, none of which require a live
    // backend.
    supportFile: 'cypress/support/e2e.ts',
    setupNodeEvents(_on, _config) {},
  },
});
