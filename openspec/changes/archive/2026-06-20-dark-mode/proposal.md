## Why

The application currently hard-codes a dark theme (`theme-type: dark`) in `styles.scss` and fixes `background-color` and `color` to dark values throughout. Users who prefer a light interface have no way to switch, and the system's `prefers-color-scheme` media query is never respected. Providing a light/dark toggle gives users control over readability and makes the app accessible in bright environments where a permanent dark background is uncomfortable.

## What Changes

- **Frontend — dual Angular Material themes:** define both a dark theme (`$dark-green-theme`) and a light theme (`$light-green-theme`) in `styles.scss`; apply the active theme via a CSS class on `<html>` (`theme-dark` / `theme-light`).
- **Frontend — `ThemeService`:** a new `core/services/theme.service.ts` that reads the initial preference from `localStorage` (key `photomanager_theme`), falls back to `prefers-color-scheme`, applies the correct CSS class to `<html>`, and exposes an `isDark$` observable and a `toggle()` method.
- **Frontend — toolbar toggle button:** a `mat-icon-button` in `AppComponent` (toolbar, right of nav links, left of Logout) toggles between `dark_mode` / `light_mode` icons and calls `ThemeService.toggle()`.
- **Frontend — preference sync on login:** after successful login the frontend calls `GET /api/preferences` to load the server-side preference and applies it; on toggle it calls `PUT /api/preferences` to persist the new value.
- **Backend — `UserPreference` domain model:** a lightweight value object `{ userId, themeMode }` (`themeMode` = `"dark"` | `"light"`).
- **Backend — Flyway migration V14:** adds a `user_preferences` table with `user_id` (FK to `users`), `theme_mode` `VARCHAR(10)`, and `updated_at` `TIMESTAMPTZ`.
- **Backend — preferences use cases:** `GetUserPreferenceUseCase` and `SaveUserPreferenceUseCase` in `domain/port/in/preference/`; implementations in `application/usecase/preference/`.
- **Backend — `GET /api/preferences` and `PUT /api/preferences`:** thin controller in `infrastructure/web/controller/` that delegates to the use cases; the authenticated user's ID is read from the JWT cookie via `SecurityContextHolder`.

## Capabilities

### New Capabilities

- `dark-mode-toggle`: Users can switch between dark and light themes via a toolbar button; the preference is remembered across sessions via `localStorage` and persisted per user on the backend.

### Modified Capabilities

- `user-authentication`: Login flow now also loads the user's saved theme preference from the backend.

## Impact

- `JPPhotoManagerWeb/frontend/src/styles.scss` — add light theme definition; apply theme via CSS class rather than unconditionally.
- `JPPhotoManagerWeb/frontend/src/app/core/services/theme.service.ts` — new file.
- `JPPhotoManagerWeb/frontend/src/app/core/services/preference.service.ts` — new file (HTTP calls for `GET/PUT /api/preferences`).
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts/html` — add toggle button.
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V14__add_user_preferences.sql` — new Flyway migration.
- `JPPhotoManagerWeb/backend/src/main/java/…/domain/model/UserPreference.java` — new domain model.
- `JPPhotoManagerWeb/backend/src/main/java/…/domain/port/in/preference/` — two new use-case interfaces.
- `JPPhotoManagerWeb/backend/src/main/java/…/domain/port/out/UserPreferenceRepository.java` — new port interface.
- `JPPhotoManagerWeb/backend/src/main/java/…/application/usecase/preference/` — two new use-case implementations.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/entity/UserPreferenceEntity.java` — new JPA entity.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/jpa/JpaUserPreferenceRepository.java` — new Spring Data JPA interface.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/adapter/UserPreferenceRepositoryImpl.java` — new persistence adapter.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/mapper/UserPreferenceMapper.java` — new MapStruct mapper.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/web/controller/UserPreferenceController.java` — new REST controller.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/web/dto/UserPreferenceDto.java` — new HTTP DTO.
