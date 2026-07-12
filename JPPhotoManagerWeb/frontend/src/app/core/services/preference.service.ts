import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { ThemeService } from './theme.service';

interface PreferenceResponse {
  themeMode: 'dark' | 'light';
}

@Injectable({ providedIn: 'root' })
export class PreferenceService {
  private readonly baseUrl = '/api/preferences';

  constructor(
    private readonly http: HttpClient,
    private readonly themeService: ThemeService
  ) {}

  load(): Observable<void> {
    return this.http.get<PreferenceResponse>(this.baseUrl).pipe(
      tap(res => this.themeService.applyTheme(res.themeMode)),
      map(() => undefined),
      catchError(() => of(undefined))
    );
  }

  save(themeMode: 'dark' | 'light'): Observable<void> {
    return this.http.put<void>(this.baseUrl, { themeMode });
  }
}
