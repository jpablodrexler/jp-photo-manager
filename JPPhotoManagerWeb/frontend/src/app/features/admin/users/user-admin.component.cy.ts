import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { UserAdminComponent } from './user-admin.component';
import { UserAdminService } from '../../../core/services/user-admin.service';
import { UserAdmin } from '../../../core/models/user-admin.model';

describe('UserAdminComponent', () => {
  const mockUsers: UserAdmin[] = [
    { id: '11111111-1111-1111-1111-111111111111', username: 'admin', createdAt: '2024-01-01T00:00:00Z' },
    { id: '22222222-2222-2222-2222-222222222222', username: 'alice', createdAt: '2024-02-01T00:00:00Z' }
  ];

  function mountAdmin(userAdminServiceOverrides: Partial<UserAdminService> = {}) {
    const serviceStub: Partial<UserAdminService> = {
      getUsers: cy.stub().returns(of(mockUsers)),
      deleteUser: cy.stub().returns(of(undefined)),
      createUser: cy.stub().returns(of(mockUsers[0])),
      updatePassword: cy.stub().returns(of(undefined)),
      ...userAdminServiceOverrides
    };
    return cy.mount(UserAdminComponent, {
      providers: [
        { provide: UserAdminService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
  }

  it('should display the list of users on init', () => {
    mountAdmin();
    cy.get('table').should('exist');
    cy.get('tr.mat-mdc-row').should('have.length', 2);
    cy.contains('admin').should('exist');
    cy.contains('alice').should('exist');
  });

  it('should call the delete service when deletion is confirmed in the dialog', () => {
    mountAdmin().then(({ serviceStub }) => {
      cy.get('button[title="Delete user"]').first().click();
      cy.get('mat-dialog-container').contains('button', 'Delete').click();
      cy.wrap(serviceStub.deleteUser).should('have.been.calledWith', mockUsers[0].id);
    });
  });

  it('should not call the delete service when deletion is cancelled in the dialog', () => {
    mountAdmin().then(({ serviceStub }) => {
      cy.get('button[title="Delete user"]').first().click();
      cy.get('mat-dialog-container').contains('button', 'Cancel').click();
      cy.wrap(serviceStub.deleteUser).should('not.have.been.called');
    });
  });

  it('should show an error message when loading users fails', () => {
    mountAdmin({ getUsers: cy.stub().returns(throwError(() => new Error('load failed'))) });
    cy.contains('.error-message', 'Failed to load users.').should('exist');
  });

  it('should show the empty state when there are no users', () => {
    mountAdmin({ getUsers: cy.stub().returns(of([])) });
    cy.contains('.no-users', 'No users found.').should('exist');
  });

  it('should show an error message when deleting a user fails', () => {
    mountAdmin({ deleteUser: cy.stub().returns(throwError(() => new Error('delete failed'))) }).then(() => {
      cy.get('button[title="Delete user"]').first().click();
      cy.get('mat-dialog-container').contains('button', 'Delete').click();
      cy.contains('.error-message', 'Failed to delete user.').should('exist');
    });
  });

  describe('add user form', () => {
    it('should toggle the add-user form when the Add User button is clicked', () => {
      mountAdmin();
      cy.get('form.add-form').should('not.exist');
      cy.contains('button', 'Add User').click();
      cy.get('form.add-form').should('exist');
      cy.contains('button', 'Cancel').click();
      cy.get('form.add-form').should('not.exist');
    });

    it('should not submit the add form when it is invalid', () => {
      mountAdmin().then(({ serviceStub }) => {
        cy.contains('button', 'Add User').click();
        cy.get('form.add-form button[type="submit"]').click();
        cy.wrap(serviceStub.createUser).should('not.have.been.called');
      });
    });

    it('should create a user and reset the form on successful submit', () => {
      const newUser: UserAdmin = { id: '33333333-3333-3333-3333-333333333333', username: 'bob', createdAt: '2024-03-01T00:00:00Z' };
      mountAdmin({ createUser: cy.stub().returns(of(newUser)) }).then(({ serviceStub }) => {
        cy.contains('button', 'Add User').click();
        cy.get('input[formControlName="username"]').type('bob');
        cy.get('input[formControlName="password"]').type('hunter2');
        cy.get('form.add-form button[type="submit"]').click();
        cy.wrap(serviceStub.createUser).should('have.been.calledWith', 'bob', 'hunter2');
        cy.contains('bob').should('exist');
        cy.get('form.add-form').should('not.exist');
      });
    });

    it('should show an error message when creating a user fails', () => {
      mountAdmin({ createUser: cy.stub().returns(throwError(() => new Error('create failed'))) }).then(() => {
        cy.contains('button', 'Add User').click();
        cy.get('input[formControlName="username"]').type('bob');
        cy.get('input[formControlName="password"]').type('hunter2');
        cy.get('form.add-form button[type="submit"]').click();
        cy.contains('.error-message', 'Failed to create user. Username may already exist.').should('exist');
      });
    });
  });

  describe('password change', () => {
    it('should show the password change form when Change password is clicked', () => {
      mountAdmin();
      cy.get('button[title="Change password"]').first().click();
      cy.get('form.inline-form').should('exist');
    });

    it('should cancel the password change form', () => {
      mountAdmin();
      cy.get('button[title="Change password"]').first().click();
      cy.get('form.inline-form').contains('button', 'Cancel').click();
      cy.get('form.inline-form').should('not.exist');
    });

    it('should not submit the password form when it is invalid', () => {
      mountAdmin().then(({ serviceStub }) => {
        cy.get('button[title="Change password"]').first().click();
        cy.get('form.inline-form button[type="submit"]').click();
        cy.wrap(serviceStub.updatePassword).should('not.have.been.called');
      });
    });

    it('should update the password and close the form on successful submit', () => {
      mountAdmin({ updatePassword: cy.stub().returns(of(undefined)) }).then(({ serviceStub }) => {
        cy.get('button[title="Change password"]').first().click();
        // The outline mat-form-field's floating label visually overlaps
        // this input inside the narrow mat-table cell until it's focused,
        // which Cypress's actionability check flags as "covered" — force
        // the type since the input is functionally reachable.
        cy.get('form.inline-form input[formControlName="password"]').type('newpass1', { force: true });
        cy.get('form.inline-form button[type="submit"]').click();
        cy.wrap(serviceStub.updatePassword).should('have.been.calledWith', mockUsers[0].id, 'newpass1');
        cy.get('form.inline-form').should('not.exist');
      });
    });

    it('should show an error message when updating the password fails', () => {
      mountAdmin({ updatePassword: cy.stub().returns(throwError(() => new Error('update failed'))) }).then(() => {
        cy.get('button[title="Change password"]').first().click();
        cy.get('form.inline-form input[formControlName="password"]').type('newpass1', { force: true });
        cy.get('form.inline-form button[type="submit"]').click();
        cy.contains('.error-message', 'Failed to update password.').should('exist');
      });
    });
  });
});
