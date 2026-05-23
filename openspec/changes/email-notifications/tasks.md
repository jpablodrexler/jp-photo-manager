## 1. Dependency and configuration

- [ ] 1.1 Add `spring-boot-starter-mail` to `pom.xml`
- [ ] 1.2 Add to `application.yml`: `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`, `spring.mail.properties.mail.smtp.auth: true`, `spring.mail.properties.mail.smtp.starttls.enable: true`

## 2. Database migration

- [ ] 2.1 Create `V19__add_email_notifications.sql`: `ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL, ADD COLUMN email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE`

## 3. EmailNotificationService

- [ ] 3.1 Create `infrastructure/service/EmailNotificationService.java` annotated with `@Service`
- [ ] 3.2 Inject `JavaMailSender` and `@Value("${spring.mail.username}")` as the sender address
- [ ] 3.3 Implement `@Async void sendOperationSummary(String to, String operationType, Map<String, Object> stats)`: build a plain-text body from `stats` entries; send via `SimpleMailMessage`; log failures at WARN level without propagating

## 4. Operation completion hooks

- [ ] 4.1 In `CatalogAssetsUseCaseImpl` (and sync/convert/backup equivalents): after the operation loop ends, load the user's `email` and `email_notifications_enabled`; if both are set, call `emailNotificationService.sendOperationSummary(...)`

## 5. HTTP adapter — UserProfileController

- [ ] 5.1 Create `infrastructure/web/controller/UserProfileController.java`
- [ ] 5.2 Add `PUT /api/profile/email` accepting `{ "email": "..." }` with `@Email` validation; persist via user repository
- [ ] 5.3 Add `PUT /api/profile/notifications` accepting `{ "emailNotificationsEnabled": true/false }`; persist via user repository
- [ ] 5.4 Add `GET /api/profile` returning `{ username, email, emailNotificationsEnabled, role }`

## 6. Backend unit tests

- [ ] 6.1 Test that `EmailNotificationService.sendOperationSummary()` calls `JavaMailSender.send()` with the correct subject and recipient
- [ ] 6.2 Test that no email is sent when `email_notifications_enabled = false`
- [ ] 6.3 Test that no email is sent when `email = null`
- [ ] 6.4 Test `PUT /api/profile/email` with an invalid email format returns 400

## 7. Frontend — ProfileComponent

- [ ] 7.1 Add email `<input matInput>` and `<mat-slide-toggle>` for notifications to `ProfileComponent` (or create it if it doesn't exist)
- [ ] 7.2 Add `getProfile()`, `updateEmail(email: string)`, and `updateNotifications(enabled: boolean)` to `UserService`
- [ ] 7.3 Register lazy route `/profile` in `app.routes.ts` if not already present

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
