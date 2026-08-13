// e2e support file — loaded automatically before every e2e spec
// (both the real-backend suite and the mocked tier - see cypress.config.ts
// vs cypress.mocked.config.ts). Custom commands (cy.login/cy.logout) are
// only used by the real-backend suite, but wiring them here rather than in
// a real-backend-only file keeps this the single shared support entrypoint,
// same as the mocked tier's own support file re-use for its shared commands.
import './commands';
