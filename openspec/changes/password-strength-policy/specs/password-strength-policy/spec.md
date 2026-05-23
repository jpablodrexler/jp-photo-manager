# password-strength-policy

Password creation and change endpoints enforce a minimum complexity policy. The frontend shows a live strength meter and per-rule feedback using the same rules mirrored client-side.

---

## ADDED Requirements

### Requirement: Password creation enforces a minimum complexity policy

User creation and password change endpoints SHALL reject passwords that do not meet all four rules: minimum 12 characters, at least one uppercase letter, at least one digit, at least one special character.

#### Scenario: Weak password is rejected with per-rule violations

- **GIVEN** a user creation request with password `"short1"`
- **WHEN** `POST /api/admin/users` is called
- **THEN** the response is `400 Bad Request` with body `{ "status": 400, "message": "Password does not meet requirements", "violations": ["Must be at least 12 characters", "Must contain at least one uppercase letter", "Must contain at least one special character"] }`

#### Scenario: Strong password is accepted

- **GIVEN** a user creation request with password `"StrongP@ssw0rd!"`
- **WHEN** `POST /api/admin/users` is called
- **THEN** the password is accepted and the user is created

#### Scenario: Password change with weak password is rejected

- **GIVEN** an authenticated user attempts to change their password to `"weak"`
- **WHEN** `PUT /api/profile/password` is called
- **THEN** the response is `400 Bad Request` with the violations list

### Requirement: Frontend shows a live password strength meter

The `PasswordStrengthComponent` SHALL evaluate the password against the same four rules client-side and display a colour bar and rule checklist without making an API call.

#### Scenario: Strength meter shows all rules as failing for an empty password

- **GIVEN** the password field is empty
- **WHEN** the `PasswordStrengthComponent` renders
- **THEN** the colour bar is red and all four rules are marked as failing

#### Scenario: Strength meter updates as the user types

- **GIVEN** the user has typed `"StrongP@ss"`
- **WHEN** the password has 10 characters, one uppercase, one special char, but no digit
- **THEN** 3 rules are marked as passing, the "at least one digit" rule is failing, and the colour bar is yellow

#### Scenario: Strength meter shows all green when all rules pass

- **GIVEN** the user has typed `"StrongP@ssw0rd!"`
- **WHEN** all four rules are satisfied
- **THEN** the colour bar is green and all four rules are marked as passing
