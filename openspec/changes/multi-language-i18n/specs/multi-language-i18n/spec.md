# multi-language-i18n

The frontend is available in English and Spanish. Users can switch language via a toggle in the navigation bar; the preference is persisted per user. Backend validation and error messages are localised to match the selected language.

---

## ADDED Requirements

### Requirement: Frontend is available in English and Spanish

The Angular build SHALL produce locale-specific bundles for `en` and `es`. All user-visible strings SHALL have `i18n` attributes and corresponding translations in both XLIFF files.

#### Scenario: Spanish locale displays translated strings

- **GIVEN** the user navigates to the `/es/` URL prefix
- **WHEN** the application loads
- **THEN** all UI labels, button text, and messages are displayed in Spanish

#### Scenario: English locale displays English strings

- **GIVEN** the user navigates to the `/en/` URL prefix
- **WHEN** the application loads
- **THEN** all UI labels are displayed in English

### Requirement: Language toggle in the navigation bar persists the locale preference

The navigation bar SHALL include a language toggle (`EN` / `ES`). Selecting a language SHALL persist the preference via `PUT /api/profile/locale` and redirect to the corresponding locale URL prefix.

#### Scenario: User switches from English to Spanish

- **GIVEN** the user is on the English (`/en/`) locale
- **WHEN** the user clicks `ES` in the language toggle
- **THEN** `PUT /api/profile/locale` is called with `{ "locale": "es" }`, the browser navigates to `/es/gallery`, and the UI displays Spanish text

### Requirement: Backend error messages are localised

The `GlobalExceptionHandler` SHALL resolve error messages from `MessageSource` using the `Accept-Language` header from the request.

#### Scenario: 404 error message returned in Spanish

- **GIVEN** a request with `Accept-Language: es` for a non-existent asset
- **WHEN** the backend returns a 404 response
- **THEN** the `message` field in the `ErrorResponse` is in Spanish (e.g., "Recurso no encontrado")

#### Scenario: Fallback to English for unsupported locale

- **GIVEN** a request with `Accept-Language: fr` (French, not supported)
- **WHEN** the backend returns an error
- **THEN** the `message` field falls back to English
