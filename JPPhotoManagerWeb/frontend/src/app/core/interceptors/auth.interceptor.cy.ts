import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;
  let refreshStub: sinon.SinonStub;
  let clearSessionStub: sinon.SinonStub;
  let navigateStub: sinon.SinonStub;

  function setup(refreshReturns: unknown) {
    refreshStub = cy.stub().returns(refreshReturns);
    clearSessionStub = cy.stub();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { refresh: refreshStub, clearSession: clearSessionStub } as Partial<AuthService>,
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
});
