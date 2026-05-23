## Context

Angular's built-in i18n system uses `i18n` attributes in templates, `ng extract-i18n` to generate XLIFF files, and `ng build --localize` to produce per-locale bundles served at `/en/` and `/es/`. The backend uses Spring `MessageSource` (auto-configured from `messages*.properties` on the classpath) to resolve message keys by `Accept-Language` header.

## Goals / Non-Goals

**Goals:**
- Mark all user-visible strings in Angular templates with `i18n` attributes; generate `messages.en.xlf` and `messages.es.xlf`
- `angular.json` `i18n` section: `sourceLocale: "en"`, `locales: { "es": "src/locale/messages.es.xlf" }`
- Language toggle in `AppComponent` navbar: a `MatButtonToggle` with `EN` / `ES`; on change, navigate to `/<locale>/...` (Angular i18n route prefix)
- Flyway V24: `ALTER TABLE users ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en'`
- `PUT /api/profile/locale` persists the user's locale preference
- Backend `GlobalExceptionHandler` uses `messageSource.getMessage(code, args, locale)` to localise error messages; locale is read from `Accept-Language` header
- `messages_en.properties` and `messages_es.properties` with keys for all validation and error messages

**Non-Goals:**
- More than two locales in the initial implementation
- Right-to-left (RTL) layout support
- Machine translation (translations are written manually)
- Locale-specific date/number formatting (Angular i18n handles this by default per locale)

## Decisions

### 1. Build-time locale bundles (not runtime translation service)

**Decision:** Use Angular's native `ng build --localize` to produce separate bundles, not a runtime translation library (ngx-translate).

**Rationale:** Build-time i18n is the Angular team's recommended approach for static translations. It eliminates a runtime dependency and produces smaller, faster bundles. The tradeoff is that switching language requires a page reload to the new locale prefix.

### 2. Locale preference stored in `users.locale`

**Decision:** Persist the locale in the `users` table. On login, the JWT payload includes the preferred locale so the frontend can redirect to the correct bundle prefix.

**Rationale:** Locale is a user preference that should persist across sessions and devices. Storing it in `localStorage` only would reset on browser clear.

### 3. Backend messages localised via `Accept-Language`

**Decision:** The Angular HTTP interceptor sets `Accept-Language: <locale>` on every request (matching the current bundle's locale). `GlobalExceptionHandler` reads this header to select the message locale.

**Rationale:** The backend does not store per-request state, so the header is the natural mechanism for locale signalling.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| New UI strings added without i18n attributes | Medium | Add a lint rule or CI check for missing `i18n` attributes (Angular ESLint `@angular-eslint/template/i18n`) |
| Spanish translations incomplete at launch | Low | Fall back to English for untranslated strings; mark them for review |
