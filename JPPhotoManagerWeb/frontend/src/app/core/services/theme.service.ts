import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

const STORAGE_KEY = 'photomanager_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _isDark$ = new BehaviorSubject<boolean>(
    this.resolveInitialMode() === 'dark'
  );

  readonly isDark$: Observable<boolean> = this._isDark$.asObservable();

  init(): void {
    this.applyTheme(this.resolveInitialMode());
  }

  applyTheme(mode: 'dark' | 'light'): void {
    const el = document.documentElement;
    el.classList.remove('theme-dark', 'theme-light');
    el.classList.add(`theme-${mode}`);
    localStorage.setItem(STORAGE_KEY, mode);
    this._isDark$.next(mode === 'dark');
  }

  toggle(): 'dark' | 'light' {
    const newMode: 'dark' | 'light' = this._isDark$.value ? 'light' : 'dark';
    this.applyTheme(newMode);
    return newMode;
  }

  private resolveInitialMode(): 'dark' | 'light' {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'dark' || stored === 'light') {
      return stored;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
