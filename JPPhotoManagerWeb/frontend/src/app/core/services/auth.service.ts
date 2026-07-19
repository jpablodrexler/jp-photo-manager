import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { PreferenceService } from './preference.service';
import { LoginResponse, MeResponse, Session } from '../models/auth.model';

const SESSION_KEY = 'photomanager_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private http: HttpClient,
    private preferenceService: PreferenceService
  ) {}

  login(username: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      switchMap(loginRes =>
        this.http.get<MeResponse>('/api/auth/me').pipe(
          tap(me => this.storeSession(loginRes.username, loginRes.expiresAt, me.role))
        )
      ),
      tap(() => this.scheduleProactiveRefresh()),
      switchMap(() => this.preferenceService.load().pipe(catchError(() => of(undefined)))),
      map(() => undefined)
    );
  }

  refresh(): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/refresh', {}).pipe(
      tap(res => {
        this.storeSession(res.username, res.expiresAt, this.getStoredRole());
        this.scheduleProactiveRefresh();
      }),
      map(() => undefined)
    );
  }

  scheduleProactiveRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return;
    try {
      const session: Session = JSON.parse(raw);
      const delay = session.expiresAt - Date.now() - 5 * 60 * 1000;
      if (delay > 0) {
        this.refreshTimer = setTimeout(
          () => this.refresh().subscribe({ error: () => {} }),
          delay
        );
      } else {
        this.refresh().subscribe({ error: () => {} });
      }
    } catch {
      // ignore
    }
  }

  logout(): Observable<void> {
    this.clearSession();
    return this.http.post<void>('/api/auth/logout', {});
  }

  clearSession(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
    localStorage.removeItem(SESSION_KEY);
  }

  isLoggedIn(): boolean {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return false;
    try {
      const session: Session = JSON.parse(raw);
      if (session.expiresAt > Date.now()) {
        return true;
      }
      this.clearSession();
      return false;
    } catch {
      this.clearSession();
      return false;
    }
  }

  isAdmin(): boolean {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return false;
    try {
      const session: Session = JSON.parse(raw);
      return session.role === 'ADMIN';
    } catch {
      return false;
    }
  }

  private getStoredRole(): string {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return 'VIEWER';
    try {
      const session: Session = JSON.parse(raw);
      return session.role ?? 'VIEWER';
    } catch {
      return 'VIEWER';
    }
  }

  private storeSession(username: string, expiresAt: string, role: string): void {
    const session: Session = { username, expiresAt: new Date(expiresAt).getTime(), role };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}
