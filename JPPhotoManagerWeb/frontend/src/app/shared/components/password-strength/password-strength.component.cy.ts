import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PasswordStrengthComponent } from './password-strength.component';

function mountStrength(password = '') {
  return cy.mount(PasswordStrengthComponent, {
    componentProperties: { password },
    providers: [provideNoopAnimations()],
  });
}

describe('PasswordStrengthComponent', () => {
  it('should mark all four rules as failing and show a red bar for an empty password', () => {
    mountStrength('');

    cy.get('.password-strength').should('have.class', 'password-strength--weak');
    cy.get('.strength-bar__fill--weak').should('exist');
    cy.get('.rule-list__item').should('have.length', 4);
    cy.get('.rule-list__item--pass').should('have.length', 0);
  });

  it('should mark 3 rules passing and show a yellow bar for a 12+ character password missing only a digit', () => {
    // 15 characters, has uppercase and a special char, no digit -> length/uppercase/special pass, digit fails.
    mountStrength('StrongPassword@');

    cy.get('.password-strength').should('have.class', 'password-strength--medium');
    cy.get('.strength-bar__fill--medium').should('exist');
    cy.get('.rule-list__item--pass').should('have.length', 3);
    cy.contains('.rule-list__item', 'At least one digit')
      .should('not.have.class', 'rule-list__item--pass');
  });

  it('should mark all four rules as passing and show a green bar when all rules are satisfied', () => {
    mountStrength('StrongP@ssw0rd!');

    cy.get('.password-strength').should('have.class', 'password-strength--strong');
    cy.get('.strength-bar__fill--strong').should('exist');
    cy.get('.rule-list__item--pass').should('have.length', 4);
  });

  it('should treat a password shorter than 12 characters as failing the length rule', () => {
    mountStrength('Short1!');

    cy.contains('.rule-list__item', 'At least 12 characters')
      .should('not.have.class', 'rule-list__item--pass');
  });
});
