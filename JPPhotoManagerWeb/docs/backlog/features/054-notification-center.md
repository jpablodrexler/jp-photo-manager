# Feature 54 — notification-center

In-app notification bell in the top navigation bar showing a history of completed background operations (catalog finished, sync complete, convert complete, backup uploaded); new `notifications` table (`id`, `userId`, `type`, `message`, `read_at`, `created_at`); backend writes a notification row at the end of each SSE stream; `GET /api/notifications` returns unread count and paginated history; `PATCH /api/notifications/read` marks all read; badge count clears when the panel is opened; new Flyway migration
