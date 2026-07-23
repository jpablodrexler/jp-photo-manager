import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;
  let refreshStub: sinon.SinonStub;
  let clearSessionStub: sinon.SinonStub;
  let navigateStub: sinon.SinonStub;
  let snackBarOpenStub: sinon.SinonStub;

  function setup(refreshReturns: unknown) {
    refreshStub = cy.stub().returns(refreshReturns);
    clearSessionStub = cy.stub();
    snackBarOpenStub = cy.stub();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { refresh: refreshStub, clearSession: clearSessionStub } as Partial<AuthService>,
        },
        {
          provide: MatSnackBar,
          useValue: { open: snackBarOpenStub } as Partial<MatSnackBar>,
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    navigateStub = cy.stub(TestBed.inject(Router), 'navigateByUrl').returns(Promise.resolve(true));
  }

  afterEach(() => {
    httpMock.verify();
  });

  it('should call refresh once on a 401 from a non-auth endpoint', () => {
    setup(throwError(() => new Error('refresh failed')));

    http.get('/api/assets').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(refreshStub).should('have.been.calledOnce');
  });

  it('should clear the session and navigate to login when refresh fails', () => {
    setup(throwError(() => new Error('refresh failed')));

    http.get('/api/assets').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(clearSessionStub).should('have.been.calledOnce');
    cy.wrap(navigateStub).should('have.been.calledWith', '/login');
  });

  it('should not call refresh and should clear the session on a 401 from the refresh endpoint', () => {
    setup(of(undefined));

    http.post('/api/auth/refresh', {}).subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/auth/refresh');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(refreshStub).should('not.have.been.called');
    cy.wrap(clearSessionStub).should('have.been.calledOnce');
  });

  it('should show the backend message in a snackbar when the error body has a message field', () => {
    setup(of(undefined));

    http.get('/api/assets/1').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets/1');
    req.flush({ status: 404, message: 'Asset not found', timestamp: new Date().toISOString() }, {
      status: 404,
      statusText: 'Not Found',
    });

    cy.wrap(snackBarOpenStub).should('have.been.calledWith', 'Asset not found', 'Dismiss', { duration: 5000 });
  });

  it('should show a generic snackbar message when the error body has no message field', () => {
    setup(of(undefined));

    http.get('/api/assets/1').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets/1');
    req.flush(null, { status: 500, statusText: 'Internal Server Error' });

    cy.wrap(snackBarOpenStub).should(
      'have.been.calledWith',
      'An unexpected error occurred',
      'Dismiss',
      { duration: 5000 }
    );
  });

  it('should append the request ID to the snackbar message when the X-Request-ID header is present', () => {
    setup(of(undefined));

    http.get('/api/assets/1').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets/1');
    req.flush({ status: 404, message: 'Asset not found', timestamp: new Date().toISOString() }, {
      status: 404,
      statusText: 'Not Found',
      headers: { 'X-Request-ID': 'abc-123' },
    });

    cy.wrap(snackBarOpenStub).should(
      'have.been.calledWith',
      'Asset not found [Request ID: abc-123]',
      'Dismiss',
      { duration: 5000 }
    );
  });

  it('should not show a snackbar for a login failure, since the login form already shows its own inline error', () => {
    setup(of(undefined));

    http.post('/api/auth/login', {}).subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ status: 401, message: 'Invalid username or password.' }, {
      status: 401,
      statusText: 'Unauthorized',
    });

    cy.wrap(snackBarOpenStub).should('not.have.been.called');
  });
});
