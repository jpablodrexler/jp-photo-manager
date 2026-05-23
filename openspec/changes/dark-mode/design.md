## Context

The frontend currently defines a single Angular Material dark theme in `styles.scss` and applies it unconditionally to `html` via `@include mat.all-component-themes($dark-green-theme)`. Hard-coded background/foreground values (`#121212`, `rgba(255,255,255,0.87)`) are spread across the global stylesheet and component styles.

The backend has a `users` table (V4 migration) and a JWT-based authentication flow. There is no concept of per-user UI preferences yet.

The desired outcome is:

1. Users can toggle the theme from the toolbar.
2. The preference survives a page reload via `localStorage`.
3. When the user is logged in the preference is also stored on the server so that it follows them across devices or browser sessions.
4. On first visit (no stored preference) the OS / browser `prefers-color-scheme` setting is honoured.

## Goals / Non-Goals

**Goals:**

- Define both a dark and a light Angular Material theme and switch between them at runtime using a CSS class on `<html>`.
- A `ThemeService` that owns theme state, reads from / writes to `localStorage`, and observes `prefers-color-scheme` as a fallback.
- A toolbar toggle button (icon-button, `dark_mode` / `light_mode` icon) visible when logged in.
- A `PreferenceService` that calls `GET /api/preferences` on login and `PUT /api/preferences` on toggle.
- A `user_preferences` table (Flyway V14) storing `theme_mode` per user.
- Two backend use cases (`GetUserPreferenceUseCase`, `SaveUserPreferenceUseCase`) following the hexagonal architecture conventions.
- A `GET /api/preferences` and `PUT /api/preferences` endpoint; both require authentication.

**Non-Goals:**

- Storing any preferences other than `theme_mode` in this change (a future `multi-language-i18n` change may share the same table).
- A user-facing settings page — the toggle is the only UI affordance.
- Admin ability to override another user's preference.
- Per-component theme overrides (the whole page switches).

## Decisions

### 1. CSS-class-based theme switching on `<html>`

**Decision:** Define two theme variables in `styles.scss`:

```scss
$dark-green-theme: mat.define-theme(…, theme-type: dark, …);
$light-green-theme: mat.define-theme(…, theme-type: light, …);

html.theme-dark  { @include mat.all-component-themes($dark-green-theme); … }
html.theme-light { @include mat.all-component-themes($light-green-theme); … }
```

`ThemeService` adds/removes `theme-dark` / `theme-light` on `document.documentElement`.

**Rationale:** Angular Material's `all-component-themes` mixin emits CSS custom properties scoped to the selector it is included under. Swapping a class on `<html>` is the idiomatic and officially documented way to achieve runtime theme switching without re-rendering the component tree.

**Alternative considered:** `OverlayContainer.getContainerElement().classList` — this only covers Material overlay elements (dialogs, menus) and not the rest of the page.

### 2. `localStorage` key `photomanager_theme` with `prefers-color-scheme` fallback

**Decision:** `ThemeService.init()` reads `localStorage.getItem('photomanager_theme')`. If the value is `'light'` or `'dark'` it is used directly. Otherwise the service checks `window.matchMedia('(prefers-color-scheme: dark)').matches`. The resolved value is written back to `localStorage` on every toggle so that the next page load is instant.

**Rationale:** `localStorage` survives page reload without a round-trip; the OS preference provides a sensible default for first-time visitors. The backend preference is authoritative only when the user is authenticated (loaded on login, written on toggle), so offline or unauthenticated sessions still work correctly.

### 3. Backend persistence in a dedicated `user_preferences` table

**Decision:** Create a new `user_preferences` table (Flyway V14):

```sql
CREATE TABLE user_preferences (
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    theme_mode VARCHAR(10)  NOT NULL DEFAULT 'dark',
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id)
);
```

**Rationale:** A separate table with `user_id` as the primary key is the simplest schema that satisfies the requirement. It is also forward-compatible with the future `multi-language-i18n` change: additional columns (e.g. `language_code`) can be added to this table later without altering the `users` table.

**Alternative considered:** Adding a `theme_mode` column directly to `users` — rejected because it pollutes the core identity table with application preferences, and adding new preferences in future would require multiple migrations modifying `users`.

### 4. `GET /api/preferences` returns only the authenticated user's preference

**Decision:** The endpoint reads the username from `SecurityContextHolder.getContext().getAuthentication().getName()`, looks up the `User` by username via the existing `UserRepository`, and returns `{ "themeMode": "dark" }`. If no row exists in `user_preferences` the response is `{ "themeMode": "dark" }` (the default).

**Rationale:** Preferences are private per user. Using the JWT-derived principal avoids exposing a user ID in the URL and prevents one user from reading another's preferences.

### 5. `PUT /api/preferences` upserts the row

**Decision:** The handler calls `SaveUserPreferenceUseCase.execute(userId, themeMode)` which performs an upsert (`INSERT … ON CONFLICT (user_id) DO UPDATE SET theme_mode = …`).

**Rationale:** The first time a user changes their theme no row exists; subsequent changes must update the existing row. A single upsert handles both cases without a read-before-write.

### 6. Frontend loads preference after login, writes on toggle

**Decision:** `AuthService.login()` chains a call to `PreferenceService.load()` which calls `GET /api/preferences` and passes the result to `ThemeService.applyTheme(themeMode)`. `ThemeService.toggle()` calls `PreferenceService.save(newMode)` after updating `localStorage` and the DOM class.

**Rationale:** Loading on login keeps the UX fast — the theme is applied before the first page render after the redirect. Writing on toggle is synchronous from the user's perspective (the DOM changes immediately; the HTTP call fires in the background).

**Error handling:** If `GET /api/preferences` fails (network error, 500), the locally resolved preference is used and no error is shown. If `PUT /api/preferences` fails, the local toggle is not reverted — the UI remains consistent and the server will be updated on the next successful call.

## Risks / Trade-offs

**Flash of wrong theme on hard reload**
→ The theme CSS class is applied by `ThemeService.init()` which is called from `AppComponent.ngOnInit()`. There can be a brief flash if the component bootstrap takes more than ~50 ms. Mitigation: the `localStorage` read is synchronous and fast; in practice the flash is imperceptible on modern hardware.

**Light theme missing from existing component styles**
→ Several hard-coded dark values (`#121212`, `rgba(255,255,255,0.87)`) are in `styles.scss`. These must be replaced with CSS custom properties or theme-aware variables so they adapt when the class changes. This is addressed in task 1.

**Server preference lags local preference on first toggle**
→ The `PUT /api/preferences` call is fire-and-forget. If the page is closed immediately after toggle the server may not have received the update. On next login the stale value is loaded from the server and overwrites `localStorage`. This is an acceptable trade-off given the low-stakes nature of a theme preference.

## Open Questions

_(none)_
