# notification-center

A notification bell in the navigation bar shows the count of unread operation-completion notifications. Clicking the bell opens a panel with a paginated history. The badge clears when the panel is opened.

---

## ADDED Requirements

### Requirement: Notifications are created when background operations complete

The backend SHALL create a notification row in the `notifications` table when a catalog, sync, convert, or backup operation completes successfully.

#### Scenario: Catalog completion creates a notification

- **GIVEN** a catalog operation completes with 150 new assets
- **WHEN** the operation finishes
- **THEN** a notification row is created with `type = "CATALOG"` and `message = "Cataloged 150 new assets"` for the authenticated user

### Requirement: Unread notification count is available via API

`GET /api/notifications` SHALL return the unread count and paginated notification history for the authenticated user.

#### Scenario: Unread count reflects unread notifications

- **GIVEN** a user has 3 unread notifications
- **WHEN** `GET /api/notifications` is called
- **THEN** the response is `200 OK` with `{ "unreadCount": 3, "items": [...] }`

#### Scenario: Read notifications are included in history but excluded from unread count

- **GIVEN** a user has 1 unread and 2 read notifications
- **WHEN** `GET /api/notifications` is called
- **THEN** `unreadCount` is `1` and `items` contains all 3 notifications (read and unread)

### Requirement: All notifications can be marked as read

`PATCH /api/notifications/read` SHALL mark all unread notifications as read for the authenticated user.

#### Scenario: Marking all read clears the unread count

- **GIVEN** a user has 3 unread notifications
- **WHEN** `PATCH /api/notifications/read` is called
- **THEN** the response is `204 No Content` and a subsequent `GET /api/notifications` returns `"unreadCount": 0`

### Requirement: Notification bell displays unread count and opens history panel

The navigation bar SHALL show a `MatBadge` on the bell icon with the current unread count. Clicking the bell SHALL mark all notifications as read and open a `MatMenu` with recent notifications.

#### Scenario: Badge shows unread count

- **GIVEN** the user has 5 unread notifications
- **WHEN** the navigation bar renders
- **THEN** the bell icon has a badge displaying `5`

#### Scenario: Opening the panel clears the badge

- **GIVEN** the user has 3 unread notifications
- **WHEN** the user clicks the bell icon
- **THEN** `PATCH /api/notifications/read` is called, the badge count becomes `0`, and the panel shows the 3 (now-read) notifications
