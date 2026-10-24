# Feature 5 — refresh-token

**As a** signed-in user, **I want** my session to be renewed silently with a refresh token, **so that** I stay signed in without re-entering my password.

## Acceptance Criteria
- AC1: Given a successful login, when it completes, then both the access and the refresh cookies are issued; a failed login issues no refresh token.
- AC2: Given a valid refresh token, when I call the refresh endpoint, then both cookies are re-issued and the old token is rotated.
- AC3: Given an expired, revoked or missing refresh token, when I call the refresh endpoint, then it is rejected.
- AC4: Given the refresh endpoint, when called without a valid access token, then it is still reachable.
- AC5: Given an expired access token, when a request returns 401, then the app refreshes and retries transparently; if the refresh fails, then I am redirected to login; the refresh call itself is never retried.
- AC6: Given the app is open, when expiry approaches, then it refreshes proactively, and the timer stops on logout.
- AC7: Given I log out or my account is deleted, when it applies, then the refresh tokens are invalidated or removed.

## Technical notes

Long-lived HttpOnly refresh-token cookie with rotate-on-use; Angular service proactively refreshes before expiry; interceptor retries 401s transparently

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
