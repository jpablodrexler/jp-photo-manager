# Feature 51 — folder-bookmarks

**As a** photo library owner, **I want** to pin folders as bookmarks above the folder tree, **so that** I reach my most used folders without expanding the tree.

## Acceptance Criteria
- AC1: Given a folder node, when I press its pin icon, then the folder is bookmarked; bookmarking an already-bookmarked folder is idempotent.
- AC2: Given 10 bookmarks, when I try to add another, then it is rejected.
- AC3: Given the folder tree, when it renders, then a pinned section lists my bookmarks above the tree, separated by a divider.
- AC4: Given the bookmarks API, when I list, add or remove, then only my own bookmarks are affected; removing another user's bookmark is refused.

## Technical notes

A pin icon on each node in the folder navigation tree bookmarks it per authenticated user; bookmarks stored in a new `folder_bookmarks` table (`id`, `userId`, `folderPath`, `createdAt`); bookmarked folders appear as a pinned section above the full tree backed by `GET /api/folders/bookmarks`, `POST /api/folders/bookmarks`, and `DELETE /api/folders/bookmarks/{id}`; frontend inserts a `MatDivider` between the pinned section and the main tree; new Flyway migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
