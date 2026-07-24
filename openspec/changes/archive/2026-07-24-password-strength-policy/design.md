## Context

Passay is a Java password validation library with built-in rules for length, character class requirements, and common-word rejection. The `PasswordValidator` class accepts a list of `Rule` objects and returns a `RuleResult` with details of each failing rule. The frontend strength meter is implemented as a standalone Angular component using the same rule thresholds.

## Goals / Non-Goals

**Goals:**
- `PasswordValidationService` configured with four Passay rules: `LengthRule(12, 128)`, `CharacterRule(EnglishCharacterData.UpperCase, 1)`, `CharacterRule(EnglishCharacterData.Digit, 1)`, `CharacterRule(EnglishCharacterData.Special, 1)`
- On validation failure, throw a `PasswordPolicyException` with a `List<String> violations` (one description per failing rule)
- `GlobalExceptionHandler` maps `PasswordPolicyException` to a `400` response with `{ status: 400, message: "Password does not meet requirements", violations: ["Must be at least 12 characters", ...] }`
- Call `passwordValidationService.validate(newPassword)` in user creation and password change use cases
- `PasswordStrengthComponent`: standalone Angular component accepting `@Input() password: string`; shows a colour bar (red/yellow/green) and a checklist of rules with pass/fail indicators; usable in both the admin user-creation form and the profile password-change form

**Non-Goals:**
- Dictionary-based common-password rejection (Passay supports this but the dictionary file adds significant bundle size)
- Enforcing the policy on existing stored passwords (only at create/change time)
- Password history (preventing reuse of previous passwords)

## Decisions

### 1. `PasswordPolicyException` extends `ValidationException`

**Decision:** Create `PasswordPolicyException(List<String> violations)` extending the existing `ValidationException`. `GlobalExceptionHandler` catches `ValidationException` and adds a `violations` field to the response when the exception is a `PasswordPolicyException`.

**Rationale:** Reuses the existing exception hierarchy; only the error body needs a minor extension for the `violations` array.

### 2. Client-side strength meter without API call

**Decision:** The `PasswordStrengthComponent` evaluates the rules entirely in the browser using TypeScript. No API call is made on each keystroke.

**Rationale:** Real-time feedback must be immediate. Calling the backend on each keystroke would add latency and server load unnecessarily.

### 3. Four fixed rules, no runtime configuration

**Decision:** The four Passay rules are hardcoded in `PasswordValidationService`. They are not user-configurable.

**Rationale:** Password policy is a security decision, not an end-user preference. Hardcoding the rules avoids an admin UI for policy configuration.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Passay rejects legitimate passwords with special chars from non-English keyboards | Low | The `Special` character class includes all non-alphanumeric ASCII characters; international keyboards typically produce these |
| Minimum 12-character policy frustrates users with short existing passwords | Low | Policy only applies to new accounts and explicit password changes; existing stored passwords are unaffected |
