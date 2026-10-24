# Feature 38 — folder-stats-in-tree

**As a** photo library owner, **I want** the asset count and total size of each folder shown in the folder tree, **so that** I see which folders are large without opening the analytics page.

## Acceptance Criteria
- AC1: Given a folder with assets, when I expand its node, then a secondary line shows its asset count and total size.
- AC2: Given an empty folder, when I expand it, then it shows zero.
- AC3: Given soft-deleted assets, when the stats are computed, then they are excluded.
- AC4: Given the stats endpoint, when called with a path, then it runs a lightweight count and sum query for that folder.

## Technical notes

Show asset count and total size as a secondary line per folder node in the folder navigation tree; backed by `GET /api/folders/stats?path=...` running a lightweight `SELECT COUNT(*), SUM(file_size)` query per folder; distinct from the full analytics dashboard (#20) — this is inline contextual data in the tree, not a separate page

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
