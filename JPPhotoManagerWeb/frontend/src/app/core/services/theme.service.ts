import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

const STORAGE_KEY = 'photomanager_theme';
const ACCENT_STORAGE_KEY = 'photomanager_accent_color';
const DEFAULT_ACCENT = '#2e7d32';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _isDark$ = new BehaviorSubject<boolean>(
    this.resolveInitialMode() === 'dark'
  );

  readonly isDark$: Observable<boolean> = this._isDark$.asObservable();

  private readonly _accentColor$ = new BehaviorSubject<string>(DEFAULT_ACCENT);
  readonly accentColor$: Observable<string> = this._accentColor$.asObservable();

  init(): void {
    this.applyTheme(this.resolveInitialMode());
    const storedAccent = localStorage.getItem(ACCENT_STORAGE_KEY);
    if (storedAccent) {
      this.setAccentColor(storedAccent);
    }
  }

  setAccentColor(color: string): void {
    document.documentElement.style.setProperty('--accent-color', color);
    document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute('content', color);
    localStorage.setItem(ACCENT_STORAGE_KEY, color);
    this._accentColor$.next(color);
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
