// Real-backend E2E: authentication. No cy.intercept — every request here
// hits the live backend. See cypress/support/e2e-release-helpers.ts.

import { realLogin, logout, ADMIN_USERNAME } from '../../support/e2e-release-helpers';

describe('Auth (real backend)', () => {
  it('login_validCredentials_redirectsToHome', () => {
    realLogin();
    cy.url().should('include', '/home');
    cy.get('.app-title').should('contain.text', 'JP Photo Manager');
  });

  it('login_invalidCredentials_showsErrorAndStaysOnLoginPage', () => {
    cy.visit('/login');
    cy.get('input[formControlName="username"]').type(ADMIN_USERNAME);
    cy.get('input[formControlName="password"]').type('definitely-not-the-real-password');
    cy.get('button[type="submit"]').click();
    cy.get('.error-message').should('contain.text', 'Invalid username or password');
    cy.url().should('include', '/login');
  });

  it('unauthenticatedNavigation_toProtectedRoute_redirectsToLogin', () => {
    cy.visit('/gallery');
    cy.url().should('include', '/login');
  });

  it('logout_fromHome_redirectsToLoginAndBlocksProtectedRoutes', () => {
    realLogin();
    logout();
    cy.visit('/home');
    cy.url().should('include', '/login');
  });
});
