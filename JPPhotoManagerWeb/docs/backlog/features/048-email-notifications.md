# Feature 48 — email-notifications

**As a** signed-in user, **I want** an email when a long-running operation finishes, **so that** I do not have to watch the screen while a catalog, sync, conversion or backup runs.

## Acceptance Criteria
- AC1: Given my profile page, when I set my email address and enable notifications, then they are saved.
- AC2: Given notifications enabled and an address configured, when a catalog, sync, convert or backup completes, then I receive a summary email.
- AC3: Given notifications disabled or no address configured, when an operation completes, then no email is sent.
- AC4: Given the configuration, when the application starts, then SMTP host, port and credentials are read from application.yml.

## Technical notes

Spring Mail (`spring-boot-starter-mail`) to send a summary email when long-running operations complete: catalog (N new assets, M updated), sync result, convert result, and backup uploaded; add `email` VARCHAR and `email_notifications_enabled` boolean to `users` (Flyway migration); configurable SMTP host, port, and credentials in `application.yml`; frontend profile page gains an email field and notification toggle; pairs naturally with `notification-center` (#54) as both are triggered by the same operation-completion events

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
