# dark-mode

Specifies how the application supports a user-controlled dark/light theme toggle stored in `localStorage` with a `prefers-color-scheme` fallback and persisted per user on the backend.

---

### Requirement: The toolbar exposes a dark/light mode toggle button

The application toolbar SHALL display an icon button that toggles between dark and light themes. The button SHALL use the `dark_mode` icon when the active theme is light (i.e. clicking it will switch to dark), and the `light_mode` icon when the active theme is dark. The button SHALL be visible only when the user is logged in. The button SHALL be positioned in the toolbar to the left of the Logout button.

#### Scenario: Toggle button shows dark_mode icon in light theme

- **GIVEN** the active theme is light
- **WHEN** the user views the toolbar
- **THEN** an icon button with the `dark_mode` icon is visible in the toolbar

#### Scenario: Toggle button shows light_mode icon in dark theme

- **GIVEN** the active theme is dark
- **WHEN** the user views the toolbar
- **THEN** an icon button with the `light_mode` icon is visible in the toolbar

#### Scenario: Toggle button not shown when logged out

- **GIVEN** the user is not authenticated
- **WHEN** any page is loaded
- **THEN** the theme toggle button is not present in the toolbar

---

### Requirement: Clicking the toggle button switches the active theme

Clicking the toolbar toggle button SHALL immediately switch the active theme between dark and light. The CSS class on `<html>` SHALL change from `theme-dark` to `theme-light` or vice versa, causing Angular Material components and global styles to update their appearance. The new preference SHALL be written to `localStorage` under the key `photomanager_theme`.

#### Scenario: Switching from dark to light

- **GIVEN** the active theme is dark (`html` has class `theme-dark`)
- **WHEN** the user clicks the toggle button
- **THEN** `html` has class `theme-light`, `localStorage['photomanager_theme']` equals `'light'`, and the `light_mode` icon is replaced by `dark_mode`

#### Scenario: Switching from light to dark

- **GIVEN** the active theme is light (`html` has class `theme-light`)
- **WHEN** the user clicks the toggle button
- **THEN** `html` has class `theme-dark`, `localStorage['photomanager_theme']` equals `'dark'`, and the `dark_mode` icon is replaced by `light_mode`

---

### Requirement: The theme preference is restored from `localStorage` on page load

On every page load, before any route renders, the application SHALL read `photomanager_theme` from `localStorage`. If the value is `'dark'` or `'light'` it SHALL be applied immediately. If no stored value exists the system SHALL check `window.matchMedia('(prefers-color-scheme: dark)')` and apply `dark` if it matches, otherwise `light`.

#### Scenario: Stored dark preference is restored

- **GIVEN** `localStorage['photomanager_theme']` is `'dark'`
- **WHEN** the page loads
- **THEN** `html` has class `theme-dark` before any route component renders

#### Scenario: Stored light preference is restored

- **GIVEN** `localStorage['photomanager_theme']` is `'light'`
- **WHEN** the page loads
- **THEN** `html` has class `theme-light` before any route component renders

#### Scenario: No stored preference — system dark mode active

- **GIVEN** no value is stored in `localStorage['photomanager_theme']`
- **AND** `window.matchMedia('(prefers-color-scheme: dark)').matches` is `true`
- **WHEN** the page loads
- **THEN** `html` has class `theme-dark`

#### Scenario: No stored preference — system light mode active

- **GIVEN** no value is stored in `localStorage['photomanager_theme']`
- **AND** `window.matchMedia('(prefers-color-scheme: dark)').matches` is `false`
- **WHEN** the page loads
- **THEN** `html` has class `theme-light`

---

### Requirement: The theme preference is loaded from the backend after login

After a successful login the frontend SHALL call `GET /api/preferences`. If the response contains `themeMode`, the value SHALL be applied via `ThemeService` and written to `localStorage`, overriding any previously stored local value. If the request fails the locally resolved preference SHALL be used unchanged and no error is displayed to the user.

#### Scenario: Server preference overrides local preference on login

- **GIVEN** `localStorage['photomanager_theme']` is `'dark'`
- **AND** the server returns `{ "themeMode": "light" }` from `GET /api/preferences`
- **WHEN** the user logs in
- **THEN** `html` has class `theme-light` and `localStorage['photomanager_theme']` equals `'light'`

#### Scenario: Local preference retained when server request fails

- **GIVEN** `localStorage['photomanager_theme']` is `'dark'`
- **AND** `GET /api/preferences` returns an HTTP error
- **WHEN** the user logs in
- **THEN** `html` retains class `theme-dark` and no error message is displayed

---

### Requirement: The theme preference is persisted to the backend on toggle

When the user clicks the toggle button and is authenticated, the frontend SHALL call `PUT /api/preferences` with body `{ "themeMode": "<newMode>" }`. The call SHALL be fire-and-forget: the DOM update and `localStorage` write happen synchronously before the HTTP response arrives. A failed `PUT` request SHALL not revert the local theme change.

#### Scenario: Preference is saved to the backend on toggle

- **GIVEN** the user is logged in and the active theme is dark
- **WHEN** the user clicks the toggle button
- **THEN** `PUT /api/preferences` is called with body `{ "themeMode": "light" }`; the theme switches immediately regardless of the HTTP response

#### Scenario: Failed save does not revert local theme

- **GIVEN** the user is logged in and the active theme is dark
- **AND** `PUT /api/preferences` returns an HTTP error
- **WHEN** the user clicks the toggle button
- **THEN** the theme switches to light locally and `localStorage['photomanager_theme']` equals `'light'`; no error is shown

#### Scenario: Preference not sent when not authenticated

- **GIVEN** the user is not authenticated
- **WHEN** the theme is toggled (e.g. via a future public page)
- **THEN** no HTTP request is issued; only `localStorage` is updated

---

### Requirement: The backend stores and retrieves theme preference per authenticated user

`GET /api/preferences` SHALL return the authenticated user's theme preference. If no preference has been saved for the user, the response SHALL default to `{ "themeMode": "dark" }`. `PUT /api/preferences` SHALL upsert the preference for the authenticated user. Both endpoints SHALL require authentication (returning 401 for unauthenticated requests).

#### Scenario: Retrieve saved preference

- **GIVEN** the authenticated user has a saved preference of `light` in `user_preferences`
- **WHEN** `GET /api/preferences` is called
- **THEN** the response is `200 OK` with body `{ "themeMode": "light" }`

#### Scenario: Retrieve default preference when none is saved

- **GIVEN** the authenticated user has no row in `user_preferences`
- **WHEN** `GET /api/preferences` is called
- **THEN** the response is `200 OK` with body `{ "themeMode": "dark" }`

#### Scenario: Save preference creates a new row

- **GIVEN** the authenticated user has no row in `user_preferences`
- **WHEN** `PUT /api/preferences` is called with body `{ "themeMode": "light" }`
- **THEN** the response is `200 OK`; a row for the user exists in `user_preferences` with `theme_mode = 'light'`

#### Scenario: Save preference updates existing row

- **GIVEN** the authenticated user has `theme_mode = 'dark'` in `user_preferences`
- **WHEN** `PUT /api/preferences` is called with body `{ "themeMode": "light" }`
- **THEN** the response is `200 OK`; the row is updated to `theme_mode = 'light'`

#### Scenario: Unauthenticated request is rejected

- **GIVEN** the request has no valid JWT cookie
- **WHEN** `GET /api/preferences` or `PUT /api/preferences` is called
- **THEN** the response is `401 Unauthorized`
