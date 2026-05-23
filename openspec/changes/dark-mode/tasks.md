## 1. Backend — Database Migration

- [ ] 1.1 Create `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V14__add_user_preferences.sql` with the following DDL:
  ```sql
  CREATE TABLE user_preferences (
      user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      theme_mode VARCHAR(10)  NOT NULL DEFAULT 'dark',
      updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
      PRIMARY KEY (user_id)
  );
  ```

## 2. Backend — Domain Layer

- [ ] 2.1 Create `domain/model/UserPreference.java` as a Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` class with fields `UUID userId` and `String themeMode`
- [ ] 2.2 Create `domain/port/in/preference/GetUserPreferenceUseCase.java` as a single-method interface: `UserPreference execute(UUID userId)`
- [ ] 2.3 Create `domain/port/in/preference/SaveUserPreferenceUseCase.java` as a single-method interface: `void execute(UUID userId, String themeMode)`
- [ ] 2.4 Create `domain/port/out/UserPreferenceRepository.java` with methods `Optional<UserPreference> findByUserId(UUID userId)` and `void save(UserPreference preference)`

## 3. Backend — Application Layer

- [ ] 3.1 Create `application/usecase/preference/GetUserPreferenceUseCaseImpl.java` annotated `@Service @Transactional(readOnly = true)`; inject `UserPreferenceRepository`; return the stored preference or a default `UserPreference` with `themeMode = "dark"` if none exists
- [ ] 3.2 Create `application/usecase/preference/SaveUserPreferenceUseCaseImpl.java` annotated `@Service @Transactional`; inject `UserPreferenceRepository`; call `repository.save(new UserPreference(userId, themeMode))`

## 4. Backend — Infrastructure — Persistence

- [ ] 4.1 Create `infrastructure/persistence/entity/UserPreferenceEntity.java` as a JPA `@Entity` with `@Table(name = "user_preferences")`; fields: `@Id UUID userId`, `String themeMode`, `Instant updatedAt`; annotate `userId` with `@Column(name = "user_id")`
- [ ] 4.2 Create `infrastructure/persistence/jpa/JpaUserPreferenceRepository.java` extending `JpaRepository<UserPreferenceEntity, UUID>`
- [ ] 4.3 Create `infrastructure/persistence/mapper/UserPreferenceMapper.java` as a MapStruct `@Mapper(componentModel = "spring")` interface with `UserPreference toDomain(UserPreferenceEntity entity)` and `UserPreferenceEntity toEntity(UserPreference domain)` methods
- [ ] 4.4 Create `infrastructure/persistence/adapter/UserPreferenceRepositoryImpl.java` implementing `UserPreferenceRepository`; inject `JpaUserPreferenceRepository` and `UserPreferenceMapper`; implement `findByUserId` and `save` (use `JpaRepository.save()` which handles upsert via `@Id` match)

## 5. Backend — Infrastructure — Web

- [ ] 5.1 Create `infrastructure/web/dto/UserPreferenceDto.java` as a Java record: `record UserPreferenceDto(String themeMode) {}`
- [ ] 5.2 Create `infrastructure/web/controller/UserPreferenceController.java` annotated `@RestController @RequestMapping("/api/preferences")`; inject `GetUserPreferenceUseCase`, `SaveUserPreferenceUseCase`, and the existing `UserRepository` (to resolve username → UUID)
- [ ] 5.3 Implement `GET /api/preferences`: read the authenticated username from `SecurityContextHolder`; look up the user by username; call `GetUserPreferenceUseCase.execute(userId)`; return `ResponseEntity<UserPreferenceDto>`
- [ ] 5.4 Implement `PUT /api/preferences`: read the authenticated username; look up the user; call `SaveUserPreferenceUseCase.execute(userId, dto.themeMode())`; return `ResponseEntity<Void>` with status `200`
- [ ] 5.5 Ensure `SecurityConfig` does not add a new permit-all rule — both endpoints inherit the existing `authenticated()` default

## 6. Backend — Unit Tests

- [ ] 6.1 Write `GetUserPreferenceUseCaseImplTest` covering: preference found returns stored value; no preference returns default dark
- [ ] 6.2 Write `SaveUserPreferenceUseCaseImplTest` covering: delegates to repository with correct userId and themeMode
- [ ] 6.3 Write `UserPreferenceControllerTest` (using `@WebMvcTest`) covering: GET returns 200 with themeMode; PUT returns 200; unauthenticated GET returns 401; unauthenticated PUT returns 401

## 7. Frontend — Theme Infrastructure

- [ ] 7.1 In `styles.scss`, define a `$light-green-theme` using `mat.define-theme` with `theme-type: light` and `primary: mat.$green-palette`
- [ ] 7.2 Replace the unconditional `html { @include mat.all-component-themes($dark-green-theme); }` with two class-scoped rules:
  ```scss
  html.theme-dark  { @include mat.all-component-themes($dark-green-theme); background-color: #121212; color: rgba(255,255,255,0.87); }
  html.theme-light { @include mat.all-component-themes($light-green-theme); background-color: #fafafa; color: rgba(0,0,0,0.87); }
  ```
- [ ] 7.3 Replace the hard-coded `background-color` and `color` on `html, body` in `styles.scss` with theme-aware values defined inside the two class selectors above
- [ ] 7.4 Add a default class to `<html>` in `index.html`: `<html lang="en" class="theme-dark">` so there is no flash before Angular bootstraps

## 8. Frontend — `ThemeService`

- [ ] 8.1 Create `core/services/theme.service.ts` as an `@Injectable({ providedIn: 'root' })` service
- [ ] 8.2 Add private `_isDark$` `BehaviorSubject<boolean>` initialised by reading `localStorage.getItem('photomanager_theme')` (falling back to `window.matchMedia('(prefers-color-scheme: dark)').matches`)
- [ ] 8.3 Expose `readonly isDark$: Observable<boolean>` as `this._isDark$.asObservable()`
- [ ] 8.4 Implement `applyTheme(mode: 'dark' | 'light'): void` that adds/removes `theme-dark` / `theme-light` on `document.documentElement`, writes `localStorage.setItem('photomanager_theme', mode)`, and calls `_isDark$.next(mode === 'dark')`
- [ ] 8.5 Implement `toggle(): void` that reads the current value, flips it, calls `applyTheme`, and returns the new mode string for callers that need it
- [ ] 8.6 Implement `init(): void` that calls `applyTheme` with the resolved initial mode (same logic as the `BehaviorSubject` initialiser — extract into a private helper `resolveInitialMode(): 'dark' | 'light'`)

## 9. Frontend — `PreferenceService`

- [ ] 9.1 Create `core/services/preference.service.ts` as an `@Injectable({ providedIn: 'root' })` service; inject `HttpClient` and `ThemeService`
- [ ] 9.2 Define `interface PreferenceResponse { themeMode: 'dark' | 'light'; }`
- [ ] 9.3 Implement `load(): Observable<void>` that calls `GET /api/preferences`, applies the result via `ThemeService.applyTheme()`, and swallows errors with `catchError(() => of(undefined))`
- [ ] 9.4 Implement `save(themeMode: 'dark' | 'light'): void` that calls `PUT /api/preferences` with body `{ themeMode }` and subscribes; errors are silently ignored

## 10. Frontend — App Component Integration

- [ ] 10.1 Inject `ThemeService` and `PreferenceService` in `AppComponent`
- [ ] 10.2 Call `ThemeService.init()` in `AppComponent.ngOnInit()` before the breakpoint subscription
- [ ] 10.3 Add `isDark$` alias in `AppComponent`: `readonly isDark$ = this.themeService.isDark$`
- [ ] 10.4 Implement `toggleTheme(): void` that calls `const newMode = this.themeService.toggle()` and then `this.preferenceService.save(newMode)` if `this.authService.isLoggedIn()`
- [ ] 10.5 In `app.component.html`, inside the `@if (isLoggedIn)` block and before the Logout button (both mobile menu and desktop toolbar), add:
  ```html
  <button mat-icon-button (click)="toggleTheme()" [attr.aria-label]="(isDark$ | async) ? 'Switch to light mode' : 'Switch to dark mode'">
    <mat-icon>{{ (isDark$ | async) ? 'light_mode' : 'dark_mode' }}</mat-icon>
  </button>
  ```
- [ ] 10.6 Add `MatIconModule`, `MatButtonModule`, and `AsyncPipe` to `AppComponent` imports (they are likely already present; verify and add only what is missing)

## 11. Frontend — Login Integration

- [ ] 11.1 In `AuthService.login()`, after `tap(() => this.scheduleProactiveRefresh())`, add a call to `PreferenceService.load()` subscribed in a `tap(() => this.preferenceService.load().subscribe())`; inject `PreferenceService` into `AuthService` to avoid a circular dependency (verify no cycle: `AuthService` → `PreferenceService` → `ThemeService`; `ThemeService` has no dependency on `AuthService`)

## 12. Frontend — Tests

- [ ] 12.1 Create `core/services/theme.service.cy.ts` as a Cypress component test (using `cy.mount` with a minimal fixture component); cover: init applies stored dark preference; init applies stored light preference; init falls back to dark when `prefers-color-scheme: dark`; toggle switches from dark to light and writes localStorage; toggle switches from light to dark
- [ ] 12.2 Create `core/services/preference.service.cy.ts` covering: `load()` calls `GET /api/preferences` and applies themeMode; `load()` swallows errors silently; `save()` calls `PUT /api/preferences` with correct body
- [ ] 12.3 Update or create `app.component.cy.ts` to cover: toggle button shows `dark_mode` icon when theme is light; toggle button shows `light_mode` icon when theme is dark; toggle button absent when logged out; clicking toggle button calls `ThemeService.toggle()`

## 13. Testing and Commit

- [ ] 13.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 13.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 13.3 Commit all changes (only after both test suites pass)
