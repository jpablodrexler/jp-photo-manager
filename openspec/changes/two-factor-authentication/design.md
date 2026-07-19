## Context

The `users` table currently has `id`, `username`, `password` (BCrypt), and `role`. JWT cookies are set by the login endpoint after password verification. The `dev.samstevens.totp` library handles TOTP secret generation, code validation, and `otpauth://` URI construction. ZXing encodes the URI as a QR code PNG.

## Goals / Non-Goals

**Goals:**
- Flyway V18: `ALTER TABLE users ADD COLUMN totp_secret VARCHAR(512) NULL, ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE`; create `totp_backup_codes (id BIGINT PK, user_id BIGINT FK, code_hash VARCHAR(60), used BOOLEAN DEFAULT FALSE)`
- `POST /api/auth/2fa/setup`: generate a TOTP secret, store it (AES-encrypted) with `totp_enabled = false`, return a base64-encoded QR code PNG for the `otpauth://totp/...` URI
- `POST /api/auth/2fa/verify`: validate the submitted 6-digit code against the stored secret; on success set `totp_enabled = true` and return 10 plain-text backup codes (immediately hashed and stored)
- Login flow change: after password verification, if `totp_enabled = true`, return `202` with `{ "status": "TOTP_REQUIRED", "challengeToken": "<short-lived JWT>" }`; `POST /api/auth/2fa/challenge` validates the TOTP code (or a backup code) against the `challengeToken`, then issues the full JWT cookie
- TOTP challenge endpoint rate-limited (5 attempts/15 minutes per IP) via `api-rate-limiting`

**Non-Goals:**
- Hardware security keys (FIDO2/WebAuthn) — out of scope
- Recovery flow beyond backup codes (e.g., admin reset via email)
- Per-device "remember this device for 30 days" option

## Decisions

### 1. Short-lived `challengeToken` instead of session state

**Decision:** After password verification with `totp_enabled`, issue a short-lived (5-minute) JWT signed with the same secret but with `scope: "totp-challenge"` in the claims. `POST /api/auth/2fa/challenge` validates this token before accepting the TOTP code.

**Rationale:** Stateless; no server-side session required. The challenge token cannot be used for any other API call (wrong scope).

### 2. AES encryption for TOTP secret at rest

**Decision:** Encrypt the TOTP secret with AES-256 before storing in `totp_secret`. The AES key is read from `${photomanager.security.totp-encryption-key}`.

**Rationale:** If the database is compromised, the attacker needs both the DB dump and the application config to reconstruct TOTP secrets.

### 3. Backup codes: 10 codes, BCrypt-hashed, single-use

**Decision:** Generate 10 random 8-character alphanumeric codes. Return them in plain text once. Hash each with BCrypt (cost 10) and store in `totp_backup_codes`. Mark `used = true` on first use.

**Rationale:** Consistent with industry practice (GitHub, AWS). BCrypt prevents bulk code exposure if `totp_backup_codes` is leaked.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| User loses authenticator and backup codes | Medium | Admin can manually reset `totp_enabled = false` via DB; a future admin API endpoint can automate this |
| Clock skew causes valid codes to fail | Low | `dev.samstevens.totp` allows ±1 time step (±30s) tolerance by default |
| Brute-force on 6-digit code | Medium | TOTP challenge endpoint rate-limited to 5 attempts/15 minutes |
