## 1. Angular i18n setup

- [ ] 1.1 Run `ng add @angular/localize` to add the i18n package
- [ ] 1.2 Add `i18n` attributes to all user-visible strings in templates (labels, buttons, headings, snackbar messages)
- [ ] 1.3 Run `ng extract-i18n --output-path src/locale` to generate `messages.xlf` (English source)
- [ ] 1.4 Copy to `src/locale/messages.en.xlf` and `src/locale/messages.es.xlf`; translate all units in the Spanish file

## 2. Angular build configuration

- [ ] 2.1 Add to `angular.json` under `projects.frontend.i18n`: `sourceLocale: "en"`, `locales: { "es": "src/locale/messages.es.xlf" }`
- [ ] 2.2 Update `build.configurations.production` to include `"localize": true`
- [ ] 2.3 Update the dev proxy and `ng serve` to serve the default English locale in development

## 3. Language toggle in AppComponent

- [ ] 3.1 Add a `MatButtonToggleGroup` with `EN` / `ES` options to the navigation bar
- [ ] 3.2 On change, call `PUT /api/profile/locale` then `window.location.href = '/<locale>/gallery'`
- [ ] 3.3 Set the toggle initial value from the current URL prefix

## 4. Database migration

- [ ] 4.1 Create `V24__add_locale_to_users.sql`: `ALTER TABLE users ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en'`

## 5. Backend — locale persistence

- [ ] 5.1 Add `PUT /api/profile/locale` to `UserProfileController` accepting `{ "locale": "en" | "es" }`; persist to `users.locale`
- [ ] 5.2 Include `locale` in the `GET /api/profile` response
- [ ] 5.3 Include `locale` in the JWT payload so the frontend can read it on login without a separate profile call

## 6. Backend — MessageSource

- [ ] 6.1 Create `src/main/resources/messages_en.properties` with all error message keys (e.g., `error.resource.not.found=Resource not found`, `error.access.denied=Access denied`, `error.unexpected=An unexpected error occurred`)
- [ ] 6.2 Create `src/main/resources/messages_es.properties` with Spanish translations for all keys
- [ ] 6.3 Configure `MessageSource` bean in `AppConfig` with `defaultEncoding: UTF-8` and `fallbackToSystemLocale: false`
- [ ] 6.4 Update `GlobalExceptionHandler` to use `messageSource.getMessage(code, null, locale)` where locale is parsed from the `Accept-Language` header

## 7. HTTP interceptor

- [ ] 7.1 Add `Accept-Language: <locale>` to all outgoing HTTP requests in the Angular interceptor (read locale from the URL prefix)

## 8. Backend unit tests

- [ ] 8.1 Test that `GlobalExceptionHandler` returns the Spanish message when `Accept-Language: es`
- [ ] 8.2 Test that `GlobalExceptionHandler` falls back to English for `Accept-Language: fr`

## 9. Testing and Commit

- [ ] 9.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 9.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 9.3 Commit all changes (only after both test suites pass)
