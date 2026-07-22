import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GlobalErrorHandler } from './global-error-handler';

describe('GlobalErrorHandler', () => {
  let snackBarOpenStub: sinon.SinonStub;
  let handler: GlobalErrorHandler;

  beforeEach(() => {
    snackBarOpenStub = cy.stub();

    TestBed.configureTestingModule({
      providers: [
        GlobalErrorHandler,
        { provide: MatSnackBar, useValue: { open: snackBarOpenStub } as Partial<MatSnackBar> },
      ],
    });

    handler = TestBed.inject(GlobalErrorHandler);
  });

  it('should show the error message in a snackbar when the error has a message', () => {
    handler.handleError(new Error('Something broke'));

    cy.wrap(snackBarOpenStub).should('have.been.calledWith', 'Something broke', 'Dismiss', { duration: 5000 });
  });

  it('should show a generic message when the error has no message', () => {
    handler.handleError('a raw string error');

    cy.wrap(snackBarOpenStub).should(
      'have.been.calledWith',
      'An unexpected error occurred',
      'Dismiss',
      { duration: 5000 }
    );
  });

  it('should not swallow the error silently', () => {
    handler.handleError(new Error('Unhandled'));

    cy.wrap(snackBarOpenStub).should('have.been.calledOnce');
  });
});
