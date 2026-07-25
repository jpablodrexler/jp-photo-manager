// Real-backend E2E: User Administration. Creates and deletes its own
// throwaway user — never touches the real `admin` account used to log in
// for this whole suite.

import { realLogin } from '../../support/e2e-release-helpers';

// Must satisfy the real password-strength policy (>=12 chars, uppercase,
// special character) enforced server-side — see PasswordPolicyException.
const TEST_USERNAME = `e2e-release-user-${Date.now()}`;
const TEST_PASSWORD = 'E2eRelease!2026Test';
const NEW_PASSWORD = 'E2eRelease!2026Rotated';

describe('Admin: User Administration (real backend)', () => {
  beforeEach(() => {
    realLogin();
    cy.visit('/admin/users');
  });

  it('createUser_withCompliantPassword_appearsInUsersTable', () => {
    cy.get('button').contains('Add User').click();
    cy.get('input[formControlName="username"]').type(TEST_USERNAME);
    cy.get('input[formControlName="password"]').type(TEST_PASSWORD);
    cy.contains('button[type="submit"]', 'Create').click();
    cy.contains('.users-table tr', TEST_USERNAME, { timeout: 10000 }).should('exist');
  });

  it('changePassword_forTestUser_succeedsWithoutError', () => {
    cy.contains('.users-table tr', TEST_USERNAME)
      .find('button[title="Change password"]')
      .click();
    // force: true -- same mat-form-field label/input overlap race as
    // 03-gallery.cy.ts's search input, here right after the change-password
    // dialog's open animation.
    cy.get('input[formControlName="password"]').type(NEW_PASSWORD, { force: true });
    cy.contains('button[type="submit"]', 'Save').click();
    cy.get('.error-message').should('not.exist');
  });

  it('deleteUser_removesTestUserFromTable', () => {
    cy.contains('.users-table tr', TEST_USERNAME)
      .find('button[title="Delete user"]')
      .click();
    // ConfirmDialogComponent — confirm the deletion.
    cy.get('mat-dialog-container').contains('button', 'Delete').click();
    cy.contains('.users-table tr', TEST_USERNAME).should('not.exist');
  });
});
