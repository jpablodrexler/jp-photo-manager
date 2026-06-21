## 1. CSS foundation — introduce `--accent-color` variable

- [x] 1.1 In `JPPhotoManagerWeb/frontend/src/styles.scss`, add `--accent-color: #2e7d32;` as the last custom-property declaration inside `html.theme-dark { … }` (after `--input-placeholder`).
- [x] 1.2 In `styles.scss`, add `--accent-color: #2e7d32;` as the last custom-property declaration inside `html.theme-light { … }` (after `--input-placeholder`).
- [x] 1.3 In `JPPhotoManagerWeb/frontend/src/app/app.component.scss`, replace `background-color: #2e7d32 !important;` with `background-color: var(--accent-color) !important;`.

## 2. `ThemeService` — accent colour management

- [x] 2.1 In `JPPhotoManagerWeb/frontend/src/app/core/services/theme.service.ts`, add the constant `const ACCENT_STORAGE_KEY = 'photomanager_accent_color';` and `const DEFAULT_ACCENT = '#2e7d32';` directly below the existing `STORAGE_KEY` constant.
- [x] 2.2 Add a private field `private readonly _accentColor$ = new BehaviorSubject<string>(DEFAULT_ACCENT);` and a public `readonly accentColor$: Observable<string> = this._accentColor$.asObservable();`.
- [x] 2.3 Implement `setAccentColor(color: string): void` that:
  - Calls `document.documentElement.style.setProperty('--accent-color', color)`
  - Updates the PWA meta tag: `document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute('content', color)`
  - Persists to storage: `localStorage.setItem(ACCENT_STORAGE_KEY, color)`
  - Emits: `this._accentColor$.next(color)`
- [x] 2.4 In the existing `init()` method, after `this.applyTheme(this.resolveInitialMode())`, add accent colour initialisation:
  ```typescript
  const storedAccent = localStorage.getItem(ACCENT_STORAGE_KEY);
  if (storedAccent) { this.setAccentColor(storedAccent); }
  ```

## 3. `AccentColorPickerComponent` — swatch UI

- [x] 3.1 Create directory `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/`.
- [x] 3.2 Create `accent-color-picker.component.ts` as a standalone component (`selector: 'app-accent-color-picker'`); import `CommonModule`, `MatButtonModule`, `MatIconModule`, `AsyncPipe`; inject `ThemeService`; expose `accentColor$ = this.themeService.accentColor$`; define the palette as a `readonly` class constant:
  ```typescript
  readonly PALETTE = [
    { label: 'Forest Green', value: '#2e7d32' },
    { label: 'Ocean Blue',   value: '#1565c0' },
    { label: 'Deep Purple',  value: '#6a1b9a' },
    { label: 'Teal',         value: '#00695c' },
    { label: 'Rust Orange',  value: '#bf360c' },
    { label: 'Slate Grey',   value: '#37474f' },
    { label: 'Crimson Red',  value: '#b71c1c' },
    { label: 'Indigo',       value: '#283593' },
  ];
  ```
  Implement `select(color: string): void` that calls `this.themeService.setAccentColor(color)`.
- [x] 3.3 Create `accent-color-picker.component.html`:
  - Outer `<div class="swatch-row">` containing an `@for` loop over `PALETTE`
  - Each iteration renders `<button class="swatch" [style.background-color]="swatch.value" [attr.aria-label]="swatch.label" (click)="select(swatch.value)" [class.active]="(accentColor$ | async) === swatch.value">`
  - Inside the button: `<mat-icon class="check-icon">check</mat-icon>` — visible only when the class `active` is applied
- [x] 3.4 Create `accent-color-picker.component.scss`:
  - `.swatch-row`: `display: flex; gap: 8px; padding: 8px;`
  - `.swatch`: `width: 28px; height: 28px; border-radius: 50%; border: 2px solid transparent; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0;`
  - `.swatch.active`: `border-color: white; box-shadow: 0 0 0 2px rgba(0,0,0,0.4);`
  - `.check-icon`: `font-size: 18px; width: 18px; height: 18px; color: white; display: none;`
  - `.swatch.active .check-icon`: `display: block;`

## 4. `AppComponent` — toolbar integration

- [x] 4.1 In `JPPhotoManagerWeb/frontend/src/app/app.component.ts`, add `AccentColorPickerComponent` to the `imports` array of the component decorator.
- [x] 4.2 In `app.component.html` (desktop block, `@if (!isMobile)`), add immediately before the existing dark/light toggle `<button mat-icon-button (click)="toggleTheme()">`:
  ```html
  <button mat-icon-button [matMenuTriggerFor]="colorMenu" aria-label="Change accent color">
    <mat-icon>palette</mat-icon>
  </button>
  <mat-menu #colorMenu="matMenu">
    <app-accent-color-picker />
  </mat-menu>
  ```
- [x] 4.3 In `app.component.html` (mobile block, inside `<mat-menu #navMenu="matMenu">`), add a non-button container immediately after the existing theme-toggle `<button mat-menu-item (click)="toggleTheme()">`:
  ```html
  <div style="padding: 0 8px;">
    <app-accent-color-picker />
  </div>
  ```

## 5. Cypress tests — `ThemeService` accent colour

- [x] 5.1 Create `JPPhotoManagerWeb/frontend/src/app/core/services/theme.service.cy.ts` (or add to it if already existing) using `cy.mount` with a minimal blank fixture component that injects `ThemeService`.
- [x] 5.2 Test `setAccentColor_validHex_setsCSSVariable`: call `service.setAccentColor('#1565c0')`; assert `document.documentElement.style.getPropertyValue('--accent-color')` equals `'#1565c0'`.
- [x] 5.3 Test `setAccentColor_validHex_updatesMetaThemeColor`: call `service.setAccentColor('#1565c0')`; assert `document.querySelector('meta[name="theme-color"]')?.getAttribute('content')` equals `'#1565c0'`.
- [x] 5.4 Test `setAccentColor_validHex_persistsToLocalStorage`: call `service.setAccentColor('#6a1b9a')`; assert `localStorage.getItem('photomanager_accent_color')` equals `'#6a1b9a'`.
- [x] 5.5 Test `setAccentColor_validHex_emitsOnAccentColor$`: subscribe to `service.accentColor$`; call `service.setAccentColor('#00695c')`; assert emitted value equals `'#00695c'`.
- [x] 5.6 Test `init_storedAccentColor_appliesSavedColor`: set `localStorage.setItem('photomanager_accent_color', '#bf360c')` before calling `service.init()`; assert `document.documentElement.style.getPropertyValue('--accent-color')` equals `'#bf360c'`.
- [x] 5.7 Test `init_noStoredAccentColor_usesDefault`: clear `localStorage`; call `service.init()`; assert CSS variable equals `'#2e7d32'` (default from stylesheet, not inline style — the property should not be set).

## 6. Cypress tests — `AccentColorPickerComponent`

- [x] 6.1 Create `JPPhotoManagerWeb/frontend/src/app/shared/components/accent-color-picker/accent-color-picker.component.cy.ts`.
- [x] 6.2 Test `mount_rendersEightSwatches`: mount the component; assert there are exactly 8 `.swatch` buttons.
- [x] 6.3 Test `mount_activeSwatchHasCheckIcon`: stub `ThemeService.accentColor$` to emit `'#2e7d32'`; mount; assert the swatch with `background-color: rgb(46, 125, 50)` has class `active` and its `.check-icon` is visible.
- [x] 6.4 Test `swatchClick_callsSetAccentColor`: spy on `ThemeService.setAccentColor`; click the second swatch (`#1565c0`); assert `setAccentColor` was called with `'#1565c0'`.
- [x] 6.5 Test `swatchClick_updatesActiveClass`: mount with a real `ThemeService`; click the third swatch (`#6a1b9a`); assert the third swatch gains class `active` and no other swatch has it.

## 7. Build verification

- [x] 7.1 Run `cd JPPhotoManagerWeb/frontend && npm run build -- --configuration production` and confirm zero errors.
- [x] 7.2 Run `cd JPPhotoManagerWeb/frontend && npm test` and confirm all existing and new tests pass.
- [x] 7.3 Commit all changes once both steps pass.
