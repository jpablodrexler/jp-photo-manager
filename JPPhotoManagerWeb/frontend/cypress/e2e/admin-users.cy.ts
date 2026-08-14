import { E2E_PREFIX, strongPassword, uniqueSuffix } from '../support/commands';

// Real-backend E2E for admin/users - a secondary, throwaway user only.
//
// SAFETY: never operate on the seeded `admin` account itself. Confirmed via
// DeleteUserUseCaseImpl that the backend has no safeguard against deleting
// the last admin or the currently-logged-in user - the frontend UI is the
// only thing preventing an accidental self-lockout, so this suite must not
// bypass it by targeting `admin`.

describe('Admin users', () => {
  // The backend normalizes usernames to lowercase on creation (confirmed by
  // running this spec: a mixed-case E2E_PREFIX round-tripped as lowercase),
  // so build the value already lowercase - what's typed must match what's
  // rendered back for every later assertion in this file.
  const username = `${E2E_PREFIX.toLowerCase()}_user_${uniqueSuffix()}`;
  const password = strongPassword();
  const newPassword = strongPassword();
  let userId: string | undefined;

  beforeEach(() => {
    cy.login(); // admin
    cy.visit('/admin/users');
  });

  after(() => {
    // Safety net only - test 3 deletes the user via the real UI and clears
    // userId once that succeeds.
    if (userId) {
      cy.request({ method: 'DELETE', url: `/api/admin/users/${userId}`, failOnStatusCode: false });
    }
  });

  it('adminUsers_createUser_appearsInTable', () => {
    cy.contains('button', 'Add User').click();
    cy.get('input[formControlName="username"]').type(username);
    cy.get('input[formControlName="password"]').type(password);
    cy.contains('button[type="submit"]', 'Create').click();

    cy.contains('td', username).should('be.visible');

    cy.request('/api/admin/users').then((response) => {
      const created = (response.body as Array<{ id: string; username: string }>).find(
        (user) => user.username === username,
      );
      expect(created, 'created user present in GET /api/admin/users').to.exist;
      userId = created?.id;
    });
  });

  it('adminUsers_changePassword_succeeds', () => {
    cy.contains('tr.mat-mdc-row', username).find('button[title="Change password"]').click();
    // { force: true }: this field's control is required, so Angular
    // Material renders an aria-hidden "*" marker in the label that
    // geometrically overlaps the resting input and trips Cypress's
    // actionability check even though a real click/type passes through it
    // fine - see the e2e-suite skill's §7 note on required-field markers.
    cy.get('input[formControlName="password"]').type(newPassword, { force: true });
    cy.contains('button[type="submit"]', 'Save').click();

    cy.contains('tr.mat-mdc-row', username).should('be.visible');
  });

  it('adminUsers_deleteUser_removedFromTable', () => {
    cy.contains('tr.mat-mdc-row', username).find('button[title="Delete user"]').click();
    cy.get('mat-dialog-container').contains('button', 'Delete').click();

    cy.contains('td', username).should('not.exist');
    userId = undefined;
  });
});
