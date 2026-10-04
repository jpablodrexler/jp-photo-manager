# Feature 37 — asset-description

Add a `description` VARCHAR column to `assets` (new Flyway migration); expose `PATCH /api/assets/{id}/description`; display an editable text field in the EXIF panel in the viewer; the description field feeds directly into the `full-text-search` (#31) index once both are in place
