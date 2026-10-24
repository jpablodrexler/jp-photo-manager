# Feature 52 — multi-language-i18n

**As a** signed-in user, **I want** the interface in English or Spanish, **so that** I use the app in the language I am most comfortable with.

## Acceptance Criteria
- AC1: Given a locale, when it is Spanish or English, then the interface strings are shown in that language.
- AC2: Given the toolbar toggle, when I switch language, then the app switches and my choice is stored on my account.
- AC3: Given a backend error, when the locale is Spanish, then the message is returned in Spanish.
- AC4: Given an unsupported locale, when a string is requested, then it falls back to English.
- AC5: Given the build, when it runs with localization, then one bundle per locale is produced.

## Technical notes

Angular `@angular/localize` for the frontend starting with English and Spanish (mirrors open desktop issue #140); Spring Boot `MessageSource` for backend validation and error messages; locale preference stored per user as a `locale` column on `users` (or inside a `user_preferences` JSON column shared with dark-mode preference from #15); language toggle in the top navigation bar; Angular build produces one bundle per locale via `ng build --localize`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
