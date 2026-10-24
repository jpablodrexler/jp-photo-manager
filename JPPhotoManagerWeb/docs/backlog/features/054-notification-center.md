# Feature 54 — notification-center

**As a** signed-in user, **I want** a notification bell with the history of finished operations, **so that** I can see what completed while I was away.

## Acceptance Criteria
- AC1: Given a catalog, sync, convert or backup finishes, when the operation ends, then a notification is created for me.
- AC2: Given the bell, when it renders, then it shows the unread count.
- AC3: Given the history, when I open it, then read and unread notifications are listed, paginated, and only unread ones count in the badge.
- AC4: Given I open the panel or mark all read, when the update applies, then the badge clears.

## Technical notes

In-app notification bell in the top navigation bar showing a history of completed background operations (catalog finished, sync complete, convert complete, backup uploaded); new `notifications` table (`id`, `userId`, `type`, `message`, `read_at`, `created_at`); backend writes a notification row at the end of each SSE stream; `GET /api/notifications` returns unread count and paginated history; `PATCH /api/notifications/read` marks all read; badge count clears when the panel is opened; new Flyway migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
