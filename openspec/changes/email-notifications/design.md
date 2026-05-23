## Context

Spring Mail (`JavaMailSender`) is the standard Spring Boot email abstraction. SMTP credentials are configured via `spring.mail.*` properties. Each long-running use case (catalog, sync, convert, backup) already calls a `Consumer<Notification>` to stream progress; a completion callback can trigger the email. The `users` table stores per-user preferences.

## Goals / Non-Goals

**Goals:**
- Flyway V19: `ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL, ADD COLUMN email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE`
- `EmailNotificationService` with `sendOperationSummary(String to, String operationType, Map<String, Object> stats)` using `JavaMailSender`
- After each operation completes (catalog, sync, convert, backup), check if the user has `email_notifications_enabled = true` and a non-null `email`; if so, call `EmailNotificationService`
- Email subject: `"JPPhotoManager: <operationType> completed"`; body: plain text with key stats (e.g., "Cataloged 150 new assets, updated 12")
- `PUT /api/profile/email` updates the user's email address
- `PUT /api/profile/notifications` toggles `email_notifications_enabled`
- Frontend `ProfileComponent` shows email field + toggle

**Non-Goals:**
- HTML email templates (plain text is sufficient for V1)
- Unsubscribe links (users can disable via the profile toggle)
- Sending notifications for per-asset failures (only operation-level summary)

## Decisions

### 1. Call `EmailNotificationService` in the use case completion handler

**Decision:** After the operation's `SseEmitter` sends the `done` event, call `emailNotificationService.sendOperationSummary(...)` in a `@Async` method to avoid blocking the SSE response.

**Rationale:** Sending email synchronously would delay the `done` SSE event by the SMTP round-trip. An async call detaches email sending from the SSE stream.

### 2. Plain text email body

**Decision:** Build the email body as a formatted plain-text string. No Thymeleaf or Freemarker templates.

**Rationale:** Avoids adding a template engine dependency. Plain text is universally readable and sufficient for a summary notification.

### 3. SMTP credentials in `application.yml`

**Decision:** Configure `spring.mail.host`, `port`, `username`, `password`, and `properties.mail.smtp.auth=true` in `application.yml`. Credentials are injected via environment variables in production.

**Rationale:** Standard Spring Boot Mail configuration. No custom abstraction needed.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| SMTP server unavailable — operation result lost | Low | Log the email sending failure; do not fail the operation result |
| User configures incorrect email address | Low | No email validation beyond format; user is responsible for configuring a valid address |
| Email contains sensitive catalog statistics | Low | Stats are counts and file names owned by the user; no third-party data |
