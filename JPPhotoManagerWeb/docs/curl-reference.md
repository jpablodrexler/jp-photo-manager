[← Back to README](../README.md)

# curl Command Reference

All commands below assume the backend is reachable at `http://localhost:8080`.  
Authentication uses **HttpOnly cookies** — `curl` handles them automatically via the `-c`/`-b` flags.

```bash
# Save cookies to a file after login (run this first)
curl -c cookies.txt -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# → 200

# All subsequent requests use -b cookies.txt to send the jwt cookie
```

---

## Authentication

```bash
# Log in — sets jwt and refreshToken cookies
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Refresh the JWT using the refresh-token cookie (rotates both cookies)
curl -c cookies.txt -b cookies.txt \
  -X POST http://localhost:8080/api/auth/refresh

# Log out — clears both cookies server-side
curl -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

---

## Folders

```bash
# List all catalogued folders
curl -b cookies.txt http://localhost:8080/api/folders

# List folders under a specific parent
curl -b cookies.txt "http://localhost:8080/api/folders?parentPath=/home/user/Pictures"

# List available filesystem roots (drives)
curl -b cookies.txt http://localhost:8080/api/folders/drives

# Get the configured initial folder
curl -b cookies.txt http://localhost:8080/api/folders/initial

# Get recently used destination paths (used by the move dialog)
curl -b cookies.txt http://localhost:8080/api/folders/recent-paths
```

---

## Assets

```bash
# List assets in a folder (page 0, default sort)
curl -b cookies.txt \
  "http://localhost:8080/api/assets?folderPath=/home/user/Pictures&page=0"

# List assets with all filter options
curl -b cookies.txt \
  "http://localhost:8080/api/assets?folderPath=/home/user/Pictures&page=0&sort=FILE_CREATION_DATE_TIME&search=sunset&dateFrom=2024-01-01&dateTo=2024-12-31&minRating=3&tags=vacation"

# Available sort values:
#   FILE_NAME | FILE_SIZE | FILE_CREATION_DATE_TIME
#   FILE_MODIFICATION_DATE_TIME | THUMBNAIL_CREATION_DATE_TIME | RATING

# List assets grouped by date (timeline view)
curl -b cookies.txt \
  "http://localhost:8080/api/assets/timeline?folderPath=/home/user/Pictures&page=0"

# Download a thumbnail (200×150 JPEG) — save to file
curl -b cookies.txt \
  "http://localhost:8080/api/assets/1/thumbnail" -o thumbnail.jpg

# Download the full-size original image
curl -b cookies.txt \
  "http://localhost:8080/api/assets/1/image" -o original.jpg

# Get EXIF metadata for an asset
curl -b cookies.txt http://localhost:8080/api/assets/1/exif

# Rate an asset (0–5 stars; 0 clears the rating)
curl -b cookies.txt -X PATCH http://localhost:8080/api/assets/1/rating \
  -H "Content-Type: application/json" \
  -d '{"rating":4}'

# Move assets to another folder
curl -b cookies.txt -X POST http://localhost:8080/api/assets/move \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2,3],"destinationFolderPath":"/home/user/Pictures/Archive","preserveOriginal":false}'

# Copy assets (preserveOriginal: true)
curl -b cookies.txt -X POST http://localhost:8080/api/assets/move \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2],"destinationFolderPath":"/home/user/Backup","preserveOriginal":true}'

# Download assets as a ZIP archive — save to file
curl -b cookies.txt -X POST http://localhost:8080/api/assets/download \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2,3]}' -o assets.zip

# Remove assets from the catalog only (files kept on disk)
curl -b cookies.txt -X DELETE \
  "http://localhost:8080/api/assets?assetIds=1&assetIds=2"

# Delete assets from the catalog AND delete the files on disk
curl -b cookies.txt -X DELETE \
  "http://localhost:8080/api/assets?assetIds=1&assetIds=2&deleteFiles=true"

# Get grouped duplicate assets
curl -b cookies.txt http://localhost:8080/api/assets/duplicates

# Upload a file into a folder
curl -b cookies.txt -X POST http://localhost:8080/api/assets/upload \
  -F "file=@/home/user/photo.jpg" \
  -F "folderPath=/home/user/Pictures/Imported"
```

---

## Catalog

The catalog endpoint streams Server-Sent Events. Use `curl -N` (no buffering) to see events as they arrive.

```bash
# Start cataloguing all configured root folders and stream progress
curl -b cookies.txt -N http://localhost:8080/api/assets/catalog
# Events arrive as:  data: {"reason":"ASSET_CREATED","asset":{...}}
# The stream closes automatically when cataloguing is complete.
```

---

## Tags

```bash
# Search tag suggestions (returns tags matching a prefix)
curl -b cookies.txt "http://localhost:8080/api/tags?q=vac"

# Add a tag to a single asset
curl -b cookies.txt -X POST http://localhost:8080/api/assets/1/tags \
  -H "Content-Type: application/json" \
  -d '{"name":"vacation"}'

# Remove a tag from a single asset
curl -b cookies.txt -X DELETE \
  "http://localhost:8080/api/assets/1/tags?name=vacation"

# Add a tag to multiple assets at once
curl -b cookies.txt -X POST http://localhost:8080/api/assets/tags/bulk \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2,3],"name":"vacation"}'

# Remove a tag from multiple assets at once
curl -b cookies.txt -X DELETE http://localhost:8080/api/assets/tags/bulk \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2,3],"name":"vacation"}'
```

---

## Albums

```bash
# List all albums
curl -b cookies.txt http://localhost:8080/api/albums

# Create an album
curl -b cookies.txt -X POST http://localhost:8080/api/albums \
  -H "Content-Type: application/json" \
  -d '{"name":"Summer 2024","description":"Beach photos"}'

# Get an album's assets (paginated)
curl -b cookies.txt "http://localhost:8080/api/albums/1?page=0"

# Rename / update an album
curl -b cookies.txt -X PUT http://localhost:8080/api/albums/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Summer 2024 — Best Of","description":"Curated selection"}'

# Add assets to an album
curl -b cookies.txt -X POST http://localhost:8080/api/albums/1/assets \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2,3]}'

# Remove assets from an album
curl -b cookies.txt -X DELETE http://localhost:8080/api/albums/1/assets \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[2]}'

# Delete an album
curl -b cookies.txt -X DELETE http://localhost:8080/api/albums/1
```

---

## Search Presets

```bash
# List all saved search presets
curl -b cookies.txt http://localhost:8080/api/search-presets

# Save the current filters as a preset
curl -b cookies.txt -X POST http://localhost:8080/api/search-presets \
  -H "Content-Type: application/json" \
  -d '{"name":"Vacation 3-star","search":"vacation","dateFrom":"2024-06-01","dateTo":"2024-08-31","minRating":3}'

# Delete a preset
curl -b cookies.txt -X DELETE http://localhost:8080/api/search-presets/1
```

---

## Recycle Bin

```bash
# List soft-deleted assets (page 0)
curl -b cookies.txt "http://localhost:8080/api/recycle-bin?page=0"

# Restore specific assets from the recycle bin
curl -b cookies.txt -X POST http://localhost:8080/api/recycle-bin/restore \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[1,2]}'

# Purge specific assets permanently
curl -b cookies.txt -X DELETE http://localhost:8080/api/recycle-bin \
  -H "Content-Type: application/json" \
  -d '{"assetIds":[3,4]}'

# Purge ALL deleted assets permanently (empty body)
curl -b cookies.txt -X DELETE http://localhost:8080/api/recycle-bin
```

---

## Sync

```bash
# Get current sync configuration
curl -b cookies.txt http://localhost:8080/api/sync/configuration

# Save sync configuration (list of directory pairs)
curl -b cookies.txt -X PUT http://localhost:8080/api/sync/configuration \
  -H "Content-Type: application/json" \
  -d '[{"sourceDirectory":"/home/user/Pictures","destinationDirectory":"/backup/Pictures","includeSubFolders":true,"deleteAssetsNotInSource":false,"order":1}]'

# Run sync and stream progress events
curl -b cookies.txt -N http://localhost:8080/api/sync/run
```

---

## Convert

```bash
# Get current convert configuration
curl -b cookies.txt http://localhost:8080/api/convert/configuration

# Save convert configuration (PNG → JPEG directory pairs)
curl -b cookies.txt -X PUT http://localhost:8080/api/convert/configuration \
  -H "Content-Type: application/json" \
  -d '[{"sourceDirectory":"/home/user/Pictures/Raw","destinationDirectory":"/home/user/Pictures/JPEG","includeSubFolders":false,"deleteAssetsNotInSource":false,"order":1}]'

# Run convert and stream progress events
curl -b cookies.txt -N http://localhost:8080/api/convert/run
```

---

## Media streaming

```bash
# Stream an audio asset (returns the audio file bytes)
curl -b cookies.txt "http://localhost:8080/api/assets/1/stream" -o track.mp3

# Get the asset list for a playlist asset
curl -b cookies.txt http://localhost:8080/api/audio/playlist/5
```

---

## Home / Dashboard

```bash
# Get dashboard statistics (total assets, folders, duplicates, etc.)
curl -b cookies.txt http://localhost:8080/api/home/stats
```

---

## User Administration

These endpoints require an authenticated administrator account.

```bash
# List all users
curl -b cookies.txt http://localhost:8080/api/admin/users

# Create a new user
curl -b cookies.txt -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"s3cr3t!"}'

# Change a user's password (replace UUID with the actual user id)
curl -b cookies.txt -X PATCH \
  http://localhost:8080/api/admin/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/password \
  -H "Content-Type: application/json" \
  -d '{"password":"newpassword"}'

# Delete a user
curl -b cookies.txt -X DELETE \
  http://localhost:8080/api/admin/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

[← Back to README](../README.md)
