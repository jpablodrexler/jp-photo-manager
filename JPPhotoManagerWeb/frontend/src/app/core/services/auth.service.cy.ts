import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

const SESSION_KEY = 'photomanager_session';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.removeItem(SESSION_KEY);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(SESSION_KEY);
    service.clearSession();
  });

  it('should schedule a timer when the session has a future expiry', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify({
      username: 'alice',
      expiresAt: Date.now() + 10 * 60 * 1000,
    }));

    service.scheduleProactiveRefresh();

    expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.not.be.null;
  });

  it('should cancel the refresh timer and remove stored session on clear', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify({
      username: 'alice',
      expiresAt: Date.now() + 10 * 60 * 1000,
    }));

    service.scheduleProactiveRefresh();
    expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.not.be.null;

    service.clearSession();

    expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.be.null;
    expect(localStorage.getItem(SESSION_KEY)).to.be.null;
  });
});
