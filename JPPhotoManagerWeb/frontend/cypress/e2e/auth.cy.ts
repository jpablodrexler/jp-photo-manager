import { E2E_PREFIX } from '../support/commands';

// Real-backend E2E: login/logout mechanics and the auth guard. Doesn't use
// cy.login()'s cached session for the "valid login" case specifically,
// since that IS the flow under test here - every other spec in this suite
// uses cy.login() to skip straight past this.

describe('Auth', () => {
  it('auth_unauthenticatedUser_guardedRouteRedirectsToLogin', () => {
    cy.visit('/albums');
    cy.location('pathname').should('eq', '/login');
  });

  it('auth_validCredentials_reachesHomeAndMeReflectsUser', () => {
    cy.visit('/login');
    cy.get('input[formControlName="username"]').type('admin');
    cy.get('input[formControlName="password"]').type('admin');
    cy.get('button[type="submit"]').click();

    cy.location('pathname').should('eq', '/home');
    cy.request('/api/auth/me').its('body').should('deep.include', { username: 'admin', role: 'ADMIN' });
  });

  it('auth_invalidCredentials_showsErrorAndStaysOnLogin', () => {
    cy.visit('/login');
    cy.get('input[formControlName="username"]').type(`${E2E_PREFIX}_nouser`);
    cy.get('input[formControlName="password"]').type('wrong-password');
    cy.get('button[type="submit"]').click();

    cy.get('.error-message').should('be.visible');
    cy.location('pathname').should('eq', '/login');
  });

  it('auth_loggedInUser_logoutClearsSessionAndRedirects', () => {
    cy.login();
    cy.logout();
    cy.visit('/albums');
    cy.location('pathname').should('eq', '/login');
  });
});
