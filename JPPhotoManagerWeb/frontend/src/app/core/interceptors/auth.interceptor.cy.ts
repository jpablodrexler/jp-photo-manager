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

  it('on401FromNonAuthEndpoint_callsRefreshOnce', () => {
    setup(throwError(() => new Error('refresh failed')));

    http.get('/api/assets').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(refreshStub).should('have.been.calledOnce');
  });

  it('refreshFailure_callsClearSessionAndNavigatesToLogin', () => {
    setup(throwError(() => new Error('refresh failed')));

    http.get('/api/assets').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/assets');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(clearSessionStub).should('have.been.calledOnce');
    cy.wrap(navigateStub).should('have.been.calledWith', '/login');
  });

  it('on401FromRefreshEndpoint_doesNotCallRefreshAndCallsClearSession', () => {
    setup(of(undefined));

    http.post('/api/auth/refresh', {}).subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/auth/refresh');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    cy.wrap(refreshStub).should('not.have.been.called');
    cy.wrap(clearSessionStub).should('have.been.calledOnce');
  });
});
