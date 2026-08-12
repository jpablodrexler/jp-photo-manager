import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Profile Sessions feature.

describe('Profile sessions', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/auth/sessions', {
      body: [
        { id: 1, deviceHint: 'Chrome on Windows', lastUsedAt: '2024-06-01T10:00:00Z', current: true },
        { id: 2, deviceHint: 'Mobile Safari', lastUsedAt: '2024-05-30T08:00:00Z', current: false },
      ],
    }).as('getSessions');

    visitWithSession('/profile/sessions');
    cy.wait('@getSessions');
  });

  it('profileSessionsPage_pageLoaded_sessionRowsAreRendered', () => {
    cy.get('tr.mat-mdc-row').should('have.length', 2);
  });

  it('profileSessionsPage_revokeConfirmed_sessionRowIsRemoved', () => {
    cy.intercept('DELETE', '/api/auth/sessions/2', { statusCode: 204 }).as('revoke');

    cy.get('tr.mat-mdc-row').eq(1).find('button[title="Revoke session"]').click();
    cy.wait('@revoke');

    cy.get('tr.mat-mdc-row').should('have.length', 1);
    cy.get('.mat-mdc-snack-bar-label').should('contain', 'Session revoked');
  });
});
