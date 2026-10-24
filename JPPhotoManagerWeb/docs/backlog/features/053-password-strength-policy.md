# Feature 53 — password-strength-policy

**As a** administrator, **I want** a password policy with a live strength meter, **so that** accounts use strong passwords and users see exactly what is missing.

## Acceptance Criteria
- AC1: Given a new user or a password change, when the password is weak, then it is rejected with a 400 listing each violated rule.
- AC2: Given the rules (minimum 12 characters, an uppercase letter, a digit and a special character), when a strong password is sent, then it is accepted.
- AC3: Given the user form and the profile page, when I type, then a strength meter shows each rule failing or passing live; an empty password shows all failing and a compliant one all green.

## Technical notes

Enforce minimum password complexity on user creation and password change using the `Passay` library (configurable rules: minimum length 12, at least one uppercase, one digit, one special character); the Angular user-admin form and profile page show a live strength meter powered by the same rule set mirrored client-side; returns a structured `400` with per-rule violation details so the frontend can highlight exactly which rules failed; no schema change

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
