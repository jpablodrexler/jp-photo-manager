# Feature 35 — thumbnail-regeneration

Add `POST /api/assets/regenerate-thumbnails` (optionally scoped by `folderPath` query param) that deletes existing `.bin` thumbnail files via `ThumbnailPort` and re-generates them through `StoragePort`; covers corrupted thumbnails, thumbnail size changes, and retroactive EXIF-rotation correction; reuses existing infrastructure adapters with no schema change
