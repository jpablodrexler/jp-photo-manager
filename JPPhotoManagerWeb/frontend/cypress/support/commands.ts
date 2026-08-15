// Shared custom commands for the real-backend E2E suite (cypress/e2e/*.cy.ts,
// excluding cypress/e2e/mocked/**). Not used by the mocked tier, which seeds
// its own session via cypress/support/mocked/seed-session.ts instead.

export const E2E_PREFIX = 'zzE2E';

// Timestamp + short random string - makes every piece of test data this
// suite creates unique and easy to spot (and safe to hand-delete) if a run
// crashes before its own cleanup.
export function uniqueSuffix(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

// Satisfies PasswordStrengthComponent's 4 rules (>=12 chars, uppercase,
// digit, special character) - see password-strength.component.ts. Every
// admin-users test needs one for the secondary account it creates.
export function strongPassword(): string {
  return `Zz9!${uniqueSuffix()}`;
}

declare global {
  namespace Cypress {
    interface Chainable {
      /** Logs in through the real UI, caching the session (cookies) via cy.session()
       *  so only the first call per username in a run actually drives the login form. */
      login(username?: string, password?: string): Chainable<void>;
      /** Clicks the toolbar Logout button and waits for the /login redirect. */
      logout(): Chainable<void>;
    }
  }
}

Cypress.Commands.add('login', (username = 'admin', password = 'admin') => {
  cy.session(
    `${username}-session`,
    () => {
      cy.visit('/login');
      cy.get('input[formControlName="username"]').type(username);
      cy.get('input[formControlName="password"]').type(password);
      cy.get('button[type="submit"]').click();
      cy.location('pathname').should('eq', '/home');
    },
    {
      // The jwt/refreshToken cookies cy.session cached are still valid as
      // long as GET /api/auth/me succeeds with them attached.
      validate: () => {
        cy.request('/api/auth/me').its('status').should('eq', 200);
      },
    },
  );
  cy.visit('/home');
});

Cypress.Commands.add('logout', () => {
  cy.contains('button', 'Logout').click();
  cy.location('pathname').should('eq', '/login');
});
