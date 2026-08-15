import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { SessionsComponent } from './sessions.component';
import { AuthService } from '../../../core/services/auth.service';
import { SessionInfo } from '../../../core/models/auth.model';

describe('SessionsComponent', () => {
  const mockSessions: SessionInfo[] = [
    { id: 1, deviceHint: 'Chrome on Windows', lastUsedAt: '2024-06-01T10:00:00Z', current: true },
    { id: 2, deviceHint: 'Mobile Safari', lastUsedAt: '2024-05-30T08:00:00Z', current: false }
  ];

  function mountSessions(authServiceOverrides: Partial<AuthService> = {}) {
    const serviceStub: Partial<AuthService> = {
      getSessions: cy.stub().returns(of(mockSessions)),
      revokeSession: cy.stub().returns(of(undefined)),
      revokeAllOtherSessions: cy.stub().returns(of(undefined)),
      ...authServiceOverrides
    };
    return cy.mount(SessionsComponent, {
      providers: [
        { provide: AuthService, useValue: serviceStub },
        provideNoopAnimations(),
        provideRouter([])
      ]
    }).then(({ component }) => ({ component, serviceStub }));
  }

  it('should display the list of sessions on init', () => {
    mountSessions();
    cy.get('table').should('exist');
    cy.get('tr.mat-mdc-row').should('have.length', 2);
    cy.contains('Chrome on Windows').should('exist');
    cy.contains('Mobile Safari').should('exist');
  });

  it('should disable the revoke button for the current session', () => {
    mountSessions();
    cy.get('tr.mat-mdc-row').first().find('button[title="Revoke session"]').should('be.disabled');
  });

  it('should enable the revoke button for a non-current session', () => {
    mountSessions();
    cy.get('tr.mat-mdc-row').eq(1).find('button[title="Revoke session"]').should('not.be.disabled');
  });

  it('should call revokeSession and show a confirmation snackbar when revoking a session', () => {
    mountSessions().then(({ serviceStub }) => {
      cy.get('tr.mat-mdc-row').eq(1).find('button[title="Revoke session"]').click();
      cy.wrap(serviceStub.revokeSession).should('have.been.calledWith', 2);
      cy.get('.mat-mdc-snack-bar-label').should('contain', 'Session revoked');
    });
  });

  it('should remove the revoked session row from the table', () => {
    mountSessions();
    cy.get('tr.mat-mdc-row').eq(1).find('button[title="Revoke session"]').click();
    cy.get('tr.mat-mdc-row').should('have.length', 1);
  });

  it('should call revokeAllOtherSessions when sign-out-everywhere is confirmed in the dialog', () => {
    mountSessions().then(({ serviceStub }) => {
      cy.contains('button', 'Sign out everywhere else').click();
      cy.get('mat-dialog-container').contains('button', 'Sign out').click();
      cy.wrap(serviceStub.revokeAllOtherSessions).should('have.been.calledOnce');
    });
  });

  it('should not call revokeAllOtherSessions when the confirmation dialog is cancelled', () => {
    mountSessions().then(({ serviceStub }) => {
      cy.contains('button', 'Sign out everywhere else').click();
      cy.get('mat-dialog-container').contains('button', 'Cancel').click();
      cy.wrap(serviceStub.revokeAllOtherSessions).should('not.have.been.called');
    });
  });

  it('should show an error message when loading sessions fails', () => {
    mountSessions({ getSessions: cy.stub().returns(throwError(() => new Error('network error'))) });
    cy.contains('Failed to load sessions.').should('exist');
  });

  it('should display a message when there are no active sessions', () => {
    mountSessions({ getSessions: cy.stub().returns(of([])) });
    cy.contains('No active sessions found.').should('exist');
  });
});
