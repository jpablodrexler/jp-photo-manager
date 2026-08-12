// E2E smoke test for the Login feature - form renders, and a successful
// submit calls the login/me endpoints and redirects to /home.

describe('Login', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('loginPage_pageLoaded_formIsVisible', () => {
    cy.get('input[formControlName="username"]').should('exist');
    cy.get('input[formControlName="password"]').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('loginPage_validCredentialsSubmitted_redirectsToHome', () => {
    cy.intercept('POST', '/api/auth/login', {
      body: { username: 'admin', expiresAt: new Date(Date.now() + 3_600_000).toISOString() },
    }).as('login');
    cy.intercept('GET', '/api/auth/me', { body: { username: 'admin', role: 'ADMIN' } }).as('me');

    cy.get('input[formControlName="username"]').type('admin');
    cy.get('input[formControlName="password"]').type('admin');
    cy.get('button[type="submit"]').click();

    cy.wait('@login');
    cy.wait('@me');
    cy.location('pathname').should('eq', '/home');
  });
});
