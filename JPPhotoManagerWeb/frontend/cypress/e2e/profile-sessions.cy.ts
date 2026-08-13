// Real-backend E2E for profile/sessions - scoped to what a single Cypress
// cookie jar can actually verify. True concurrent-session revocation can't
// be tested from one browser's single cookie jar (a second cy.request login
// would just overwrite the first session's cookies, not run alongside it),
// so this only covers: the current session is flagged/revoke-protected, and
// "sign out everywhere else" completes without breaking it. See the
// e2e-suite skill for this limitation spelled out in full.

describe('Profile sessions', () => {
  beforeEach(() => {
    cy.login();
    cy.visit('/profile/sessions');
  });

  it('profileSessions_currentSession_isFlaggedAndRevokeDisabled', () => {
    cy.contains('td', 'This device').should('be.visible');
    cy.get('button[title="Revoke session"]').should('be.disabled');
  });

  it('profileSessions_signOutEverywhereElse_completesAndCurrentSessionSurvives', () => {
    cy.contains('button', 'Sign out everywhere else').click();
    cy.get('mat-dialog-container').contains('button', 'Sign out').click();

    cy.contains('td', 'This device').should('be.visible');
    cy.request('/api/auth/me').its('status').should('eq', 200);
  });
});
