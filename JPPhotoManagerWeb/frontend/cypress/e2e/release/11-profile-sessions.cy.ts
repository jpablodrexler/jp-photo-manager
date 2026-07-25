// Real-backend E2E: Profile Sessions. Intentionally non-destructive — never
// clicks "Revoke" or "Sign out everywhere else", since doing so would log
// this suite's own session out mid-run and break every later spec.

import { realLogin } from '../../support/e2e-release-helpers';

describe('Profile Sessions (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/profile/sessions');
  });

  it('sessionsPage_afterLogin_listsCurrentSessionAsThisDevice', () => {
    cy.get('.sessions-table tr', { timeout: 10000 }).should('have.length.greaterThan', 1); // header + >=1 row
    cy.contains('.current-badge', 'This device').should('exist');
  });

  it('sessionsPage_currentSessionRow_hasRevokeDisabled', () => {
    cy.contains('tr', 'This device')
      .find('button[title="Revoke session"]')
      .should('be.disabled');
  });
});
