## 1. Dependencies

- [ ] 1.1 Add to `pom.xml`: `dev.samstevens.totp:totp`, `com.google.zxing:core`, `com.google.zxing:javase`

## 2. Database migration

- [ ] 2.1 Create `V18__add_2fa_fields.sql`:
  - `ALTER TABLE users ADD COLUMN totp_secret VARCHAR(512) NULL, ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE`
  - `CREATE TABLE totp_backup_codes (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), code_hash VARCHAR(60) NOT NULL, used BOOLEAN NOT NULL DEFAULT FALSE)`

## 3. Domain — Use case interfaces

- [ ] 3.1 Create `domain/port/in/auth/Setup2faUseCase.java` returning `Setup2faResult { String qrCodeBase64, String secret }`
- [ ] 3.2 Create `domain/port/in/auth/Verify2faSetupUseCase.java` returning `List<String>` (plain backup codes)
- [ ] 3.3 Create `domain/port/in/auth/Totp2faChallengeUseCase.java` with `void challenge(String challengeToken, String code)`

## 4. Application — Use case implementations

- [ ] 4.1 `Setup2faUseCaseImpl`: generate TOTP secret via `dev.samstevens.totp`; AES-encrypt; build `otpauth://` URI; encode as QR code PNG via ZXing; return base64 PNG + plain secret
- [ ] 4.2 `Verify2faSetupUseCaseImpl`: validate code; on success set `totp_enabled = true`; generate 10 random 8-char codes; BCrypt-hash and store in `totp_backup_codes`; return plain codes
- [ ] 4.3 Update login use case: after password check, if `totp_enabled`, issue short-lived `challengeToken` JWT (5-minute expiry, claim `scope: "totp-challenge"`) and return instead of full JWT
- [ ] 4.4 `Totp2faChallengeUseCaseImpl`: validate `challengeToken` JWT scope; try TOTP code via `dev.samstevens.totp`; if fails try backup codes (BCrypt match); on success issue full JWT cookie; mark backup code used if applicable

## 5. HTTP adapter

- [ ] 5.1 Create `TwoFactorAuthController` with:
  - `POST /api/auth/2fa/setup` → `Setup2faUseCaseImpl`
  - `POST /api/auth/2fa/verify` → `Verify2faSetupUseCaseImpl`
  - `POST /api/auth/2fa/challenge` → `Totp2faChallengeUseCaseImpl`
- [ ] 5.2 Add `api-rate-limiting` bucket for `/api/auth/2fa/challenge`: 5 attempts per 15 minutes per IP in `RateLimitFilter`

## 6. Backend unit tests

- [ ] 6.1 Test that `Setup2faUseCaseImpl` returns a non-null base64 QR code and stores encrypted secret
- [ ] 6.2 Test that `Verify2faSetupUseCaseImpl` returns exactly 10 backup codes and sets `totp_enabled = true`
- [ ] 6.3 Test that an invalid TOTP code during verify returns an error
- [ ] 6.4 Test that login returns `202` for a user with `totp_enabled = true`
- [ ] 6.5 Test that `Totp2faChallengeUseCaseImpl` issues a JWT cookie on a valid TOTP code
- [ ] 6.6 Test that a used backup code cannot be reused

## 7. Frontend — Login and Profile pages

- [ ] 7.1 Update `LoginComponent`: detect `202` status; show a TOTP code input step; submit `POST /api/auth/2fa/challenge`
- [ ] 7.2 Create `features/profile/two-factor/two-factor.component.ts`: show setup button; display QR code image from base64; display backup codes after verify
- [ ] 7.3 Register lazy route `/profile/two-factor` in `app.routes.ts`

## 8. Testing and Commit

- [ ] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 8.3 Commit all changes (only after both test suites pass)
