# email-notifications

Users can configure an email address and enable notifications. When a catalog, sync, convert, or backup operation completes, a summary email is sent to the configured address.

---

## ADDED Requirements

### Requirement: Users can configure an email address and notification preference

`PUT /api/profile/email` SHALL update the authenticated user's email address. `PUT /api/profile/notifications` SHALL toggle the `email_notifications_enabled` flag.

#### Scenario: User sets their email address

- **GIVEN** an authenticated user with no email configured
- **WHEN** `PUT /api/profile/email` is called with `{ "email": "user@example.com" }`
- **THEN** the response is `200 OK` and the email is stored; subsequent profile requests reflect the new email

#### Scenario: User enables email notifications

- **GIVEN** an authenticated user with `email_notifications_enabled = false`
- **WHEN** `PUT /api/profile/notifications` is called with `{ "emailNotificationsEnabled": true }`
- **THEN** `email_notifications_enabled` is set to `true`

### Requirement: Email is sent after catalog completes

When a catalog operation completes and the user has `email_notifications_enabled = true` and a configured email, the backend SHALL send a summary email asynchronously.

#### Scenario: Catalog completion triggers email

- **GIVEN** a user with `email = "user@example.com"` and `email_notifications_enabled = true`
- **WHEN** a catalog operation completes with 150 new assets cataloged
- **THEN** an email is sent to `user@example.com` with subject `"JPPhotoManager: Catalog completed"` and body including "150 new assets"

#### Scenario: No email sent when notifications disabled

- **GIVEN** a user with `email_notifications_enabled = false`
- **WHEN** a catalog operation completes
- **THEN** no email is sent

#### Scenario: No email sent when no address configured

- **GIVEN** a user with `email_notifications_enabled = true` and `email = null`
- **WHEN** a catalog operation completes
- **THEN** no email is sent

### Requirement: Email is sent after sync, convert, and backup complete

The email notification SHALL be triggered for sync, convert, and backup operations in addition to catalog, following the same enabled/configured check.

#### Scenario: Sync completion triggers email

- **GIVEN** a user with email notifications enabled
- **WHEN** a sync operation completes
- **THEN** an email is sent with subject `"JPPhotoManager: Sync completed"` and key sync statistics in the body
