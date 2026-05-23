## Context

Each long-running use case already fires a `done` SSE event on completion. The `NotificationService` is called in the same `@Async` callback after `sseEmitter.complete()`. The Angular `AppComponent` polls or uses a lightweight SSE connection for the badge count; the panel is a `MatMenu` overlay.

## Goals / Non-Goals

**Goals:**
- Flyway V21: `CREATE TABLE notifications (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), type VARCHAR(50) NOT NULL, message TEXT NOT NULL, read_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW())`
- `NotificationService.createNotification(long userId, String type, String message)` — called at end of catalog, sync, convert, and backup
- `GET /api/notifications?page=0&size=20` returns `{ unreadCount, items: [ { id, type, message, readAt, createdAt } ] }`
- `PATCH /api/notifications/read` sets `read_at = NOW()` for all unread notifications of the authenticated user
- Angular: badge count fetched on login and refreshed every 60s via `interval(60000)`; clicking bell calls `markAllRead()` then loads history in a `MatMenu`; panel is scrollable with a "Load more" button for pagination

**Non-Goals:**
- Real-time push of new notifications without polling (WebSockets/SSE for this would be a future enhancement)
- Per-notification read status (mark-all-read is sufficient for V1)
- Notification preferences (which types to receive — a future enhancement)

## Decisions

### 1. `NotificationService` called after each SSE stream completes

**Decision:** At the end of each long-running use case, call `notificationService.createNotification(userId, type, message)` synchronously (no separate async; it's a single DB insert).

**Rationale:** A DB insert is fast enough to do inline after `sseEmitter.complete()`. An `@Async` wrapper would be overkill for a single row insert.

### 2. Polling every 60 seconds for unread count

**Decision:** `AppComponent` fetches the unread count on login and then via `interval(60000)` (RxJS).

**Rationale:** WebSocket or SSE for the bell badge would complicate authentication handling. A 60-second poll is simple and sufficient for non-real-time notifications.

### 3. `MatMenu` dropdown, not a full page

**Decision:** The notification bell opens a `MatMenu` overlay with a fixed-height scrollable list of the 20 most recent notifications.

**Rationale:** A dropdown is less disruptive than navigating to a separate page. "Load more" in the dropdown covers the rare case of more than 20 notifications.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Notifications table grows unbounded | Low | Add a cleanup job that deletes notifications older than 90 days (configurable) |
| 60-second poll does not feel real-time | Low | Notifications are for completed background operations; 60s delay is acceptable |
