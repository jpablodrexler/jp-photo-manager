# Feature 47 — two-factor-authentication

**As a** signed-in user, **I want** two-factor authentication with an authenticator app, **so that** my account stays safe even if my password is stolen.

## Acceptance Criteria
- AC1: Given setup, when I start it, then a QR code is returned; when I verify a valid code, then 2FA is activated and ten single-use backup codes are shown; an invalid code is rejected.
- AC2: Given 2FA is enabled, when I log in with a correct password, then a TOTP challenge is returned instead of a session.
- AC3: Given the challenge, when I send a valid TOTP code or an unused backup code, then the session cookie is issued.
- AC4: Given the stored secret, when it is saved, then it is encrypted at rest and backup codes are hashed.
- AC5: Given repeated wrong codes, when attempts exceed the limit, then further attempts are rejected.

## Technical notes

TOTP-based 2FA via any RFC 6238-compliant authenticator app (Google Authenticator, Authy, 1Password); backend dependencies: `dev.samstevens.totp:totp` (secret generation, code verification, `otpauth://` URI building) and `com.google.zxing:core` + `com.google.zxing:javase` (QR code PNG encoding returned as base64); new `totp_secret` (AES-encrypted at rest) and `totp_enabled` boolean columns on `users` (Flyway migration); setup flow: `POST /api/auth/2fa/setup` returns a base64 QR code PNG, `POST /api/auth/2fa/verify` validates the first code and commits the secret; login flow: password check passes → if `totp_enabled` return a `202 TOTP_REQUIRED` challenge → `POST /api/auth/2fa/challenge` validates code and sets JWT cookie; generate 10 single-use backup codes (BCrypt-hashed, stored in `totp_backup_codes` table) in case the authenticator device is lost; TOTP verification endpoint must be covered by `api-rate-limiting` (#39)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
