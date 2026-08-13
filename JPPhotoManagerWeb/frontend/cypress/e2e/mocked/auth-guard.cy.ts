// E2E smoke test for authGuard: visiting a guarded route without a session
// redirects to /login. No localStorage session is seeded here (unlike every
// other mocked spec) - that's the whole point of the test.

describe('Auth guard', () => {
  it('authGuard_noSession_redirectsToLogin', () => {
    cy.visit('/gallery');
    cy.location('pathname').should('eq', '/login');
    cy.location('search').should('include', 'returnUrl');
  });
});
