# two-factor-authentication

Users can enable TOTP-based 2FA by scanning a QR code in any RFC 6238-compliant authenticator app. Login requires a TOTP code when 2FA is enabled. Ten single-use backup codes are generated for device recovery.

---

## ADDED Requirements

### Requirement: Users can set up TOTP-based 2FA

`POST /api/auth/2fa/setup` SHALL generate a TOTP secret and return a base64-encoded QR code PNG that encodes an `otpauth://totp/...` URI.

#### Scenario: Setup returns a QR code

- **GIVEN** an authenticated user with `totp_enabled = false`
- **WHEN** `POST /api/auth/2fa/setup` is called
- **THEN** the response is `200 OK` with `{ "qrCodeBase64": "<base64 PNG>", "secret": "<TOTP secret>" }` and the secret is stored (encrypted) in the database with `totp_enabled = false`

### Requirement: 2FA is activated after verifying the first TOTP code

`POST /api/auth/2fa/verify` SHALL validate the submitted code against the stored secret, enable 2FA, and return 10 single-use backup codes (shown once, never retrievable again).

#### Scenario: Valid code activates 2FA and returns backup codes

- **GIVEN** a user who has completed setup and is submitting a valid TOTP code
- **WHEN** `POST /api/auth/2fa/verify` is called with `{ "code": "123456" }`
- **THEN** `totp_enabled` is set to `true`, the response includes `{ "backupCodes": ["ABCD1234", ...] }` (10 codes), and the codes are stored as BCrypt hashes in `totp_backup_codes`

#### Scenario: Invalid code during verify is rejected

- **GIVEN** a user submitting an incorrect code during setup verification
- **WHEN** `POST /api/auth/2fa/verify` is called with an invalid code
- **THEN** the response is `400 Bad Request` and `totp_enabled` remains `false`

### Requirement: Login requires a TOTP code when 2FA is enabled

When a user with `totp_enabled = true` successfully enters their password, the login response SHALL be `202 TOTP_REQUIRED` with a short-lived challenge token instead of a JWT cookie.

#### Scenario: Login with 2FA enabled returns TOTP challenge

- **GIVEN** a user with `totp_enabled = true`
- **WHEN** `POST /api/auth/login` is called with correct credentials
- **THEN** the response is `202` with `{ "status": "TOTP_REQUIRED", "challengeToken": "<JWT>" }` and no JWT cookie is set

#### Scenario: TOTP challenge with valid code issues JWT cookie

- **GIVEN** a valid `challengeToken` and a correct TOTP code
- **WHEN** `POST /api/auth/2fa/challenge` is called with `{ "challengeToken": "...", "code": "123456" }`
- **THEN** the response is `200 OK`, a JWT HttpOnly cookie is set, and the user is fully authenticated

#### Scenario: TOTP challenge with backup code succeeds

- **GIVEN** a valid `challengeToken` and a valid unused backup code
- **WHEN** `POST /api/auth/2fa/challenge` is called with the backup code
- **THEN** authentication succeeds and the backup code is marked as used (cannot be reused)

### Requirement: TOTP challenge endpoint is rate-limited

`POST /api/auth/2fa/challenge` SHALL reject requests from a client IP that has exceeded 5 attempts in 15 minutes.

#### Scenario: Excessive challenge attempts are rejected

- **GIVEN** a client IP that has made 5 failed challenge attempts in 15 minutes
- **WHEN** a 6th attempt is made
- **THEN** the response is `429 Too Many Requests`
