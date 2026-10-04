# Feature 51 — folder-bookmarks

A pin icon on each node in the folder navigation tree bookmarks it per authenticated user; bookmarks stored in a new `folder_bookmarks` table (`id`, `userId`, `folderPath`, `createdAt`); bookmarked folders appear as a pinned section above the full tree backed by `GET /api/folders/bookmarks`, `POST /api/folders/bookmarks`, and `DELETE /api/folders/bookmarks/{id}`; frontend inserts a `MatDivider` between the pinned section and the main tree; new Flyway migration
