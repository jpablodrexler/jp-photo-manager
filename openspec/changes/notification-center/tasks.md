## 1. Database migration

- [ ] 1.1 Create `V21__create_notifications.sql`: `CREATE TABLE notifications (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), type VARCHAR(50) NOT NULL, message TEXT NOT NULL, read_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW())`
- [ ] 1.2 Add index: `CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_at)`

## 2. NotificationService

- [ ] 2.1 Create `infrastructure/service/NotificationService.java` annotated with `@Service`
- [ ] 2.2 Inject `NotificationRepositoryPort`; implement `void createNotification(long userId, String type, String message)` inserting a row with `read_at = null`

## 3. Integration with operation completions

- [ ] 3.1 Call `notificationService.createNotification(userId, "CATALOG", "Cataloged N new assets")` after catalog SSE stream completes
- [ ] 3.2 Same for sync, convert, and backup operations

## 4. HTTP adapter

- [ ] 4.1 Create `infrastructure/web/controller/NotificationController.java`
- [ ] 4.2 Add `GET /api/notifications?page=0&size=20` returning `NotificationPageResponse { long unreadCount, List<NotificationDto> items }`; `NotificationDto` has `id`, `type`, `message`, `readAt`, `createdAt`
- [ ] 4.3 Add `PATCH /api/notifications/read` setting `read_at = NOW()` for all unread notifications of the current user; return `204 No Content`

## 5. Cleanup scheduled job

- [ ] 5.1 Add `@Scheduled(cron = "0 0 3 * * ?")` method in `NotificationService` that deletes notifications older than `${photomanager.notifications.retention-days:90}` days

## 6. Backend unit tests

- [ ] 6.1 Test that `createNotification()` inserts a row with `read_at = null`
- [ ] 6.2 Test that `GET /api/notifications` returns correct `unreadCount`
- [ ] 6.3 Test that `PATCH /api/notifications/read` sets `read_at` for all unread notifications
- [ ] 6.4 Test that the cleanup job deletes notifications older than the retention period

## 7. Frontend — AppComponent notification bell

- [ ] 7.1 Add `MatBadge` bell icon button to the navigation bar in `AppComponent`
- [ ] 7.2 Create `core/services/notification.service.ts` with `getNotifications(page?: number)` and `markAllRead()`
- [ ] 7.3 In `AppComponent.ngOnInit()`, call `getNotifications()` and start an `interval(60000)` polling for `unreadCount`
- [ ] 7.4 On bell click: call `markAllRead()`, set local `unreadCount = 0`, open `MatMenu` with the last 20 notifications
- [ ] 7.5 In the `MatMenu` panel, show a scrollable list of `type` + `message` + `createdAt` (formatted); add a "Load more" button that fetches page 1

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
