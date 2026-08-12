import '@cypress/code-coverage/support';
import { mount, MountConfig } from 'cypress/angular';
import { provideZoneChangeDetection } from '@angular/core';
export { MockEventSource } from './mock-event-source';

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount;
    }
  }
}

// cy.mount() creates each fixture through its own isolated TestBed module,
// which doesn't inherit app.config.ts's providers - so without this, Angular
// 22's now-zoneless-by-default TestBed bootstrap made component fixtures
// behave zoneless in tests even though the real app still boots zone.js-based
// (provideZoneChangeDetection in app.config.ts, zone.js in angular.json's
// polyfills - a deliberate choice, not an oversight, since this app has no
// OnPush components yet). That mismatch surfaced as spurious NG0100
// ExpressionChangedAfterItHasBeenCheckedError failures in tests that mutate
// component state from outside Angular's normal event handlers (SSE message
// handlers, subscribe callbacks after a delete action) - zoneless's stricter
// checkNoChanges pass doesn't get the zone-triggered blanket re-check that
// papered over the same timing gap under zone.js. Providing this here once,
// globally, keeps every component test's change detection consistent with
// the real app instead of duplicating it into all 40+ *.cy.ts files.
Cypress.Commands.add('mount', (component, config?: MountConfig<unknown>) =>
  mount(component, {
    ...config,
    providers: [provideZoneChangeDetection({ eventCoalescing: true }), ...(config?.providers ?? [])],
  }),
);
