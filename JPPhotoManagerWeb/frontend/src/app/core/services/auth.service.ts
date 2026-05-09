import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

const SESSION_KEY = 'photomanager_session';

interface LoginResponse {
  username: string;
  expiresAt: string;
}

interface Session {
  username: string;
  expiresAt: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap(res => this.storeSession(res.username, res.expiresAt)),
      tap(() => this.scheduleProactiveRefresh()),
      map(() => undefined)
    );
  }

  refresh(): Observable<void> {
    return this.http.post<LoginResponse>('/api/auth/refresh', {}).pipe(
      tap(res => {
        this.storeSession(res.username, res.expiresAt);
        this.scheduleProactiveRefresh();
      }),
      map(() => undefined)
    );
  }

  scheduleProactiveRefresh(): void {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return;
    try {
      const session: Session = JSON.parse(raw);
      const delay = session.expiresAt - Date.now() - 5 * 60 * 1000;
      if (delay > 0) {
        if (this.refreshTimer) clearTimeout(this.refreshTimer);
        this.refreshTimer = setTimeout(() => this.refresh().subscribe(), delay);
      }
    } catch {
      // ignore
    }
  }

  logout(): void {
    this.http.post<void>('/api/auth/logout', {}).subscribe();
    this.clearSession();
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
      return session.expiresAt > Date.now();
    } catch {
      return false;
    }
  }

  private storeSession(username: string, expiresAt: string): void {
    const session: Session = { username, expiresAt: new Date(expiresAt).getTime() };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}
