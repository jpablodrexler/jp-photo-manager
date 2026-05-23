## Why

The application is currently English-only. Users in Spanish-speaking countries (the primary target audience) cannot use it in their native language. Angular `@angular/localize` and Spring Boot `MessageSource` provide a standard internationalisation stack that enables the UI and backend messages to be delivered in the user's preferred locale.

## What Changes

- Angular `@angular/localize` with English (`en`) and Spanish (`es`) translations; `ng build --localize` produces one bundle per locale
- A `locale` column is added to `users` (or `user_preferences` JSON); a language toggle in the top navigation bar persists the preference
- Spring Boot `MessageSource` serves localised backend validation and error messages keyed by `Accept-Language` header
- A Flyway migration adds the `locale` column to `users`

## Capabilities

### New Capabilities

- `multi-language-i18n`: The frontend is available in English and Spanish. Users can switch language via a toggle in the navigation bar; the preference is persisted per user. Backend validation and error messages are localised to match the selected language.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/frontend/src/` — `i18n` attributes on all UI strings; `messages.en.xlf` and `messages.es.xlf` translation files; `angular.json` locale configuration
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — language toggle in navigation bar
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V24__add_locale_to_users.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/resources/messages_en.properties` and `messages_es.properties` — localised error messages
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/AppConfig.java` — configure `MessageSource`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/exception/GlobalExceptionHandler.java` — use `MessageSource` for localised messages
