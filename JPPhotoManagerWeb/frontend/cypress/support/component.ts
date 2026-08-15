import '@cypress/code-coverage/support';
import { mount } from 'cypress/angular';
export { MockEventSource } from './mock-event-source';

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount;
    }
  }
}

Cypress.Commands.add('mount', mount);

// Bridge for StrykerJS's "command" test runner (see stryker.conf.json + the
// mutation-report.js script): Stryker's mutant-switch instrumentation checks
// `globalThis.process.env.__STRYKER_ACTIVE_MUTANT__` to decide which mutant
// is "active" for the current test run, but that instrumented code executes
// inside this component test's browser iframe, which has no Node `process`
// global — so without this bridge every mutant silently "survives" (the
// switch never activates, the original code always runs, mutation score is
// permanently stuck at 0% regardless of test quality). cypress.config.ts's
// component setupNodeEvents forwards Stryker's env var into `Cypress.env()`,
// which IS available in the browser; this file's own imports/statements
// above are guaranteed (ES module evaluation order) to finish before any
// spec file's imports (e.g. `import { FolderService } from './folder.service'`)
// are evaluated, so this runs before the mutated module's own module-level
// `stryNS_...()` call caches an inactive mutant forever. A no-op outside a
// Stryker run, since Cypress.env('STRYKER_ACTIVE_MUTANT') is otherwise unset.
const strykerActiveMutant = Cypress.env('STRYKER_ACTIVE_MUTANT');
if (strykerActiveMutant) {
  (globalThis as typeof globalThis & { process?: { env: Record<string, string> } }).process = {
    env: { __STRYKER_ACTIVE_MUTANT__: String(strykerActiveMutant) },
  };
}
