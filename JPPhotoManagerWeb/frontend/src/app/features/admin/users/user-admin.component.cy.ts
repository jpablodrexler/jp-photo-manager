import { mount } from 'cypress/angular';
import { of } from 'rxjs';
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
    return mount(UserAdminComponent, {
      providers: [
        { provide: UserAdminService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
  }

  it('ngOnInit_displaysUsers', () => {
    mountAdmin();
    cy.get('table').should('exist');
    cy.get('tr.mat-mdc-row').should('have.length', 2);
    cy.contains('admin').should('exist');
    cy.contains('alice').should('exist');
  });

  it('deleteUser_callsServiceDelete', () => {
    cy.on('window:confirm', () => true);
    mountAdmin().then(({ serviceStub }) => {
      cy.get('button[title="Delete user"]').first().click();
      cy.wrap(serviceStub.deleteUser).should('have.been.calledWith', mockUsers[0].id);
    });
  });
});
