# Feature 53 — password-strength-policy

Enforce minimum password complexity on user creation and password change using the `Passay` library (configurable rules: minimum length 12, at least one uppercase, one digit, one special character); the Angular user-admin form and profile page show a live strength meter powered by the same rule set mirrored client-side; returns a structured `400` with per-rule violation details so the frontend can highlight exactly which rules failed; no schema change
