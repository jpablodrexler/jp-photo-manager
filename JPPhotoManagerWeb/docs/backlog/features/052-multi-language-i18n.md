# Feature 52 — multi-language-i18n

Angular `@angular/localize` for the frontend starting with English and Spanish (mirrors open desktop issue #140); Spring Boot `MessageSource` for backend validation and error messages; locale preference stored per user as a `locale` column on `users` (or inside a `user_preferences` JSON column shared with dark-mode preference from #15); language toggle in the top navigation bar; Angular build produces one bundle per locale via `ng build --localize`
