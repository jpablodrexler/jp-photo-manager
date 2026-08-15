import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Observable, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { PreferenceService } from './preference.service';
import { SessionInfo } from '../models/auth.model';

const SESSION_KEY = 'photomanager_session';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  function configure(preferenceLoad: () => Observable<void> = () => of(undefined)): void {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        { provide: PreferenceService, useValue: { load: preferenceLoad } },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => {
    localStorage.removeItem(SESSION_KEY);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(SESSION_KEY);
    service.clearSession();
  });

  describe('scheduleProactiveRefresh', () => {
    it('should schedule a timer when the session has a future expiry', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 10 * 60 * 1000,
      }));

      service.scheduleProactiveRefresh();

      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.not.be.null;
    });

    it('should clear a previously scheduled timer before scheduling a new one', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 10 * 60 * 1000,
      }));

      service.scheduleProactiveRefresh();
      const firstTimer = (service as unknown as { refreshTimer: unknown }).refreshTimer;

      service.scheduleProactiveRefresh();
      const secondTimer = (service as unknown as { refreshTimer: unknown }).refreshTimer;

      expect(secondTimer).to.not.be.null;
      expect(secondTimer).to.not.equal(firstTimer);
    });

    it('should do nothing when no session is stored', () => {
      configure();

      service.scheduleProactiveRefresh();

      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.be.null;
    });

    it('should immediately trigger a refresh when the session is already past its expiry', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() - 1000,
        role: 'VIEWER',
      }));

      service.scheduleProactiveRefresh();

      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.be.null;

      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).to.equal('POST');
      // Flush an expiry far enough in the future (beyond the 5-minute proactive
      // window) so the refresh()-internal scheduleProactiveRefresh() call it
      // triggers schedules a setTimeout instead of recursing into another
      // immediate refresh() — which would leave a second, unflushed request.
      req.flush({ username: 'alice', expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() });
    });

    it('should silently ignore malformed session JSON', () => {
      configure();
      localStorage.setItem(SESSION_KEY, 'not-json');

      expect(() => service.scheduleProactiveRefresh()).to.not.throw();
      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.be.null;
    });
  });

  describe('clearSession', () => {
    it('should cancel the refresh timer and remove stored session on clear', () => {
      configure();
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

  describe('login', () => {
    it('should store the session and schedule a refresh on success', () => {
      configure();

      let completed = false;
      service.login('alice', 'hunter2').subscribe(() => (completed = true));

      const loginReq = httpMock.expectOne('/api/auth/login');
      expect(loginReq.request.method).to.equal('POST');
      expect(loginReq.request.body).to.deep.equal({ username: 'alice', password: 'hunter2' });
      loginReq.flush({ username: 'alice', expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() });

      const meReq = httpMock.expectOne('/api/auth/me');
      expect(meReq.request.method).to.equal('GET');
      meReq.flush({ username: 'alice', role: 'ADMIN' });

      expect(completed).to.be.true;
      const stored = JSON.parse(localStorage.getItem(SESSION_KEY) as string);
      expect(stored.username).to.equal('alice');
      expect(stored.role).to.equal('ADMIN');
      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.not.be.null;
    });

    it('should swallow a preference load failure and still complete', () => {
      configure(() => throwError(() => new Error('preferences unavailable')));

      let completed = false;
      service.login('alice', 'hunter2').subscribe(() => (completed = true));

      httpMock.expectOne('/api/auth/login').flush({
        username: 'alice',
        expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      });
      httpMock.expectOne('/api/auth/me').flush({ username: 'alice', role: 'VIEWER' });

      expect(completed).to.be.true;
    });
  });

  describe('refresh', () => {
    it('should store the refreshed session preserving the previously stored role', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 1000,
        role: 'ADMIN',
      }));

      service.refresh().subscribe();

      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).to.equal('POST');
      req.flush({ username: 'alice', expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() });

      const stored = JSON.parse(localStorage.getItem(SESSION_KEY) as string);
      expect(stored.role).to.equal('ADMIN');
      expect((service as unknown as { refreshTimer: unknown }).refreshTimer).to.not.be.null;
    });

    it('should default the role to VIEWER when no session was previously stored', () => {
      configure();

      service.refresh().subscribe();

      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({ username: 'alice', expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() });

      const stored = JSON.parse(localStorage.getItem(SESSION_KEY) as string);
      expect(stored.role).to.equal('VIEWER');
    });

    it('should default the role to VIEWER when the previously stored session JSON is malformed', () => {
      configure();
      localStorage.setItem(SESSION_KEY, 'not-json');

      service.refresh().subscribe();

      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({ username: 'alice', expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() });

      const stored = JSON.parse(localStorage.getItem(SESSION_KEY) as string);
      expect(stored.role).to.equal('VIEWER');
    });
  });

  describe('logout', () => {
    it('should clear the local session immediately and POST /api/auth/logout', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 60000,
        role: 'VIEWER',
      }));

      service.logout().subscribe();

      expect(localStorage.getItem(SESSION_KEY)).to.be.null;
      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).to.equal('POST');
      req.flush(null);
    });
  });

  describe('sessions', () => {
    it('should GET /api/auth/sessions', () => {
      configure();
      const mockSessions: SessionInfo[] = [
        { id: 1, deviceHint: 'Chrome on Windows', lastUsedAt: '2024-01-01T00:00:00', current: true },
      ];

      service.getSessions().subscribe(sessions => {
        expect(sessions).to.deep.equal(mockSessions);
      });

      const req = httpMock.expectOne('/api/auth/sessions');
      expect(req.request.method).to.equal('GET');
      req.flush(mockSessions);
    });

    it('should DELETE /api/auth/sessions/:id to revoke a single session', () => {
      configure();

      service.revokeSession(7).subscribe();

      const req = httpMock.expectOne('/api/auth/sessions/7');
      expect(req.request.method).to.equal('DELETE');
      req.flush(null);
    });

    it('should DELETE /api/auth/sessions to revoke all other sessions', () => {
      configure();

      service.revokeAllOtherSessions().subscribe();

      const req = httpMock.expectOne('/api/auth/sessions');
      expect(req.request.method).to.equal('DELETE');
      req.flush(null);
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when the stored session has a future expiry', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 60000,
        role: 'VIEWER',
      }));

      expect(service.isLoggedIn()).to.be.true;
    });

    it('should return false when no session is stored', () => {
      configure();

      expect(service.isLoggedIn()).to.be.false;
    });

    it('should return false and clear the session when it has already expired', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() - 1000,
        role: 'VIEWER',
      }));

      expect(service.isLoggedIn()).to.be.false;
      expect(localStorage.getItem(SESSION_KEY)).to.be.null;
    });

    it('should return false and clear the session when the stored JSON is malformed', () => {
      configure();
      localStorage.setItem(SESSION_KEY, 'not-json');

      expect(service.isLoggedIn()).to.be.false;
      expect(localStorage.getItem(SESSION_KEY)).to.be.null;
    });
  });

  describe('isAdmin', () => {
    it('should return true when the stored role is ADMIN', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 60000,
        role: 'ADMIN',
      }));

      expect(service.isAdmin()).to.be.true;
    });

    it('should return false when the stored role is not ADMIN', () => {
      configure();
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        username: 'alice',
        expiresAt: Date.now() + 60000,
        role: 'VIEWER',
      }));

      expect(service.isAdmin()).to.be.false;
    });

    it('should return false when no session is stored', () => {
      configure();

      expect(service.isAdmin()).to.be.false;
    });

    it('should return false when the stored JSON is malformed', () => {
      configure();
      localStorage.setItem(SESSION_KEY, 'not-json');

      expect(service.isAdmin()).to.be.false;
    });
  });
});
