# Feature 19 — shareable-album-links

**As a** photo library owner, **I want** to share an album through a link anyone can open, **so that** friends and family can see an album without an account.

## Acceptance Criteria
- AC1: Given my own album, when I generate a share link with or without an expiry date, then a signed token is created.
- AC2: Given another user's album or an unauthenticated request, when I try to share, then it is refused.
- AC3: Given a valid token, when a visitor opens /s/:token, then the album's assets are shown with pagination, without signing in.
- AC4: Given an unknown token, an expired token, or a token for a deleted album, when a visitor opens the link, then 404, 410 or gone is returned and a friendly message is shown.
- AC5: Given an album is deleted, when the deletion applies, then its share tokens are removed.
- AC6: Given I share an album, when the link is created, then I can copy it; if the Clipboard API is unavailable, then a fallback is offered.

## Technical notes

`POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
