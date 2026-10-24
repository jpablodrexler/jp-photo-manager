# Feature 4 — virtual-albums

**As a** photo library owner, **I want** named albums that group my photos across folders, **so that** I can organise photos by theme without moving files.

## Acceptance Criteria
- AC1: Given the Albums page, when I list, create, rename or delete an album, then my own albums change accordingly.
- AC2: Given another user's album, when I try to access it, then it is refused.
- AC3: Given assets, when I add them to an album, then they belong to it; adding a duplicate is idempotent; I can remove assets; adding to an unknown album is refused.
- AC4: Given an album, when I browse it, then its assets are paginated.
- AC5: Given an asset is deleted, when the deletion applies, then it is removed from every album.
- AC6: Given the gallery, when I choose an asset, then I can add it to an existing album or create a new one from the dialog.
- AC7: Given an unauthenticated request, when it lists albums, then it is rejected.

## Technical notes

User-scoped named collections; assets can belong to multiple albums; albums have their own paginated asset view and CRUD API

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
