## Why

The application currently accepts any password at registration and password-change time. Weak passwords like "123456" or "password" undermine account security. Enforcing a minimum complexity policy (length, character classes) and showing a live strength meter on the frontend prevents weak passwords from being stored.

## What Changes

- Add `org.passay:passay` to the backend for configurable password validation rules
- On user creation and password change, validate against: minimum 12 characters, at least one uppercase letter, one digit, one special character
- A structured `400` response includes per-rule violation details so the frontend can highlight exactly which rules failed
- The Angular user-admin and profile forms show a live strength meter (colour bar + rule checklist) driven by the same rule set mirrored client-side
- No schema change

## Capabilities

### New Capabilities

- `password-strength-policy`: Password creation and change endpoints enforce a minimum complexity policy. The frontend shows a live strength meter and per-rule feedback using the same rules mirrored client-side.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `org.passay:passay`
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/service/PasswordValidationService.java` — Passay rule evaluation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/UserController.java` — validate on user creation
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/UserProfileController.java` — validate on password change
- `JPPhotoManagerWeb/backend/src/test/` — tests for rule violations
- `JPPhotoManagerWeb/frontend/src/app/shared/components/password-strength/password-strength.component.ts` — new shared component
