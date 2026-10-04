# Feature 38 — folder-stats-in-tree

Show asset count and total size as a secondary line per folder node in the folder navigation tree; backed by `GET /api/folders/stats?path=...` running a lightweight `SELECT COUNT(*), SUM(file_size)` query per folder; distinct from the full analytics dashboard (#20) — this is inline contextual data in the tree, not a separate page
