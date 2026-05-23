## Why

Long-running operations (catalog, sync, convert, backup) complete in the background while users may have navigated away or closed the browser. There is currently no way for users to know when these operations finish without staying on the page. Email notifications let users walk away and be informed when the operation completes.

## What Changes

- Add `spring-boot-starter-mail` to the backend
- Flyway migration adds `email VARCHAR(255)` and `email_notifications_enabled BOOLEAN` to `users`
- After each long-running operation completes, the service sends a summary email if notifications are enabled for the user
- Frontend profile page gains an email field and notification toggle

## Capabilities

### New Capabilities

- `email-notifications`: Users can configure an email address and enable notifications. When a catalog, sync, convert, or backup operation completes, a summary email is sent to the configured address.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `spring-boot-starter-mail`
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V19__add_email_notifications.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — SMTP configuration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/EmailNotificationService.java` — email sending service
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/UserProfileController.java` — email and notification toggle endpoints
- `JPPhotoManagerWeb/backend/src/test/` — tests for email notification triggering
- `JPPhotoManagerWeb/frontend/src/app/features/profile/profile.component.ts` — email field and toggle
