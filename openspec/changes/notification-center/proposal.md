## Why

Background operations (catalog, sync, convert, backup) complete without any persistent record visible to the user. Once the SSE stream closes, the result is gone unless the user was watching the progress dialog. An in-app notification center gives users a browsable history of completed operations and clears the badge when they open the panel.

## What Changes

- A new `notifications` table stores per-user, per-operation completion records
- The backend writes a notification row at the end of each SSE stream
- `GET /api/notifications` returns unread count and paginated history
- `PATCH /api/notifications/read` marks all notifications as read
- A notification bell icon in the top navigation bar shows the unread badge count; clicking opens a dropdown panel listing recent notifications

## Capabilities

### New Capabilities

- `notification-center`: A notification bell in the navigation bar shows the count of unread operation-completion notifications. Clicking the bell opens a panel with a paginated history. The badge clears when the panel is opened.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V21__create_notifications.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/service/NotificationService.java` — writes notification rows
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/NotificationController.java` — REST endpoints
- `JPPhotoManagerWeb/backend/src/test/` — tests for notification creation and listing
- `JPPhotoManagerWeb/frontend/src/app/app.component.ts` — notification bell with badge and dropdown panel
- `JPPhotoManagerWeb/frontend/src/app/core/services/notification.service.ts` — Angular service for notification API calls
