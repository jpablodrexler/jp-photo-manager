import { visitWithSession } from '../../support/mocked/seed-session';

// E2E smoke test for the Admin Users feature.

describe('Admin users', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/admin/users', {
      body: [
        { id: '11111111-1111-1111-1111-111111111111', username: 'admin', createdAt: '2024-01-01T00:00:00Z' },
        { id: '22222222-2222-2222-2222-222222222222', username: 'alice', createdAt: '2024-02-01T00:00:00Z' },
      ],
    }).as('getUsers');

    visitWithSession('/admin/users', 'ADMIN');
    cy.wait('@getUsers');
  });

  it('adminUsersPage_pageLoaded_userRowsAreRendered', () => {
    cy.get('tr.mat-mdc-row').should('have.length', 2);
    cy.contains('admin').should('exist');
    cy.contains('alice').should('exist');
  });

  it('adminUsersPage_deleteConfirmed_userRowIsRemoved', () => {
    cy.intercept('DELETE', '/api/admin/users/22222222-2222-2222-2222-222222222222', { statusCode: 204 }).as('deleteUser');

    cy.get('button[title="Delete user"]').eq(1).click();
    cy.get('mat-dialog-container').contains('button', 'Delete').click();
    cy.wait('@deleteUser');

    cy.get('tr.mat-mdc-row').should('have.length', 1);
  });
});
