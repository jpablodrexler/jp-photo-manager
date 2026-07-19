## Why

Passwords alone are insufficient protection for a self-hosted photo manager exposed to the internet. A compromised password gives an attacker full access to the catalog. TOTP-based two-factor authentication (RFC 6238) adds a second factor that requires physical possession of the user's authenticator device, significantly raising the bar for unauthorized access.

## What Changes

- Backend dependencies: `dev.samstevens.totp:totp` (secret generation, code verification, QR URI) and `com.google.zxing` (QR code PNG encoding)
- Flyway migration adds `totp_secret` (AES-encrypted), `totp_enabled` boolean to `users`; creates `totp_backup_codes` table
- Setup flow: `POST /api/auth/2fa/setup` → QR code PNG base64; `POST /api/auth/2fa/verify` confirms first code and enables 2FA
- Login flow: password passes → if `totp_enabled`, return `202 TOTP_REQUIRED` challenge; `POST /api/auth/2fa/challenge` validates code and sets JWT cookie
- 10 single-use BCrypt-hashed backup codes stored in `totp_backup_codes` for device recovery
- TOTP challenge endpoint covered by `api-rate-limiting`

## Capabilities

### New Capabilities

- `two-factor-authentication`: Users can enable TOTP-based 2FA by scanning a QR code in any RFC 6238-compliant authenticator app. Login requires a TOTP code when 2FA is enabled. Ten single-use backup codes are generated for device recovery.

### Modified Capabilities

- **Login flow**: when `totp_enabled` is true, the login response returns `202 TOTP_REQUIRED` instead of a JWT cookie; the JWT is issued only after a successful `POST /api/auth/2fa/challenge`.

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `dev.samstevens.totp:totp`, `com.google.zxing:core`, `com.google.zxing:javase`
- `JPPhotoManagerWeb/backend/src/main/resources/db/migration/V18__add_2fa_fields.sql` — new Flyway migration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/TwoFactorAuthController.java` — new endpoints
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/application/usecase/auth/` — new use cases for setup, verify, and challenge
- `JPPhotoManagerWeb/backend/src/test/` — tests for 2FA flow
- `JPPhotoManagerWeb/frontend/src/app/features/profile/two-factor/two-factor.component.ts` — setup UI
- `JPPhotoManagerWeb/frontend/src/app/features/auth/login/login.component.ts` — handle `202 TOTP_REQUIRED` and show TOTP input
