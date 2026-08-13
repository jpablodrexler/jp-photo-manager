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
