# Feature 48 — email-notifications

Spring Mail (`spring-boot-starter-mail`) to send a summary email when long-running operations complete: catalog (N new assets, M updated), sync result, convert result, and backup uploaded; add `email` VARCHAR and `email_notifications_enabled` boolean to `users` (Flyway migration); configurable SMTP host, port, and credentials in `application.yml`; frontend profile page gains an email field and notification toggle; pairs naturally with `notification-center` (#54) as both are triggered by the same operation-completion events
