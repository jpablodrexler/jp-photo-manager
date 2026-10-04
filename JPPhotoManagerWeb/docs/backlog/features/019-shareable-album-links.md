# Feature 19 — shareable-album-links

`POST /api/albums/{id}/share` generates a signed UUID token stored in a `shared_albums` table with optional `expires_at`; public `/s/:token` route renders the album without authentication
