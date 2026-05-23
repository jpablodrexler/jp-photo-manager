## Context

The `CatalogAssetsUseCaseImpl` scans directory trees and delegates to `StorageService` per file. Archive files (`.zip`, `.tar.gz`, `.tgz`) need special handling: entries are extracted to a temp directory, processed as regular images, and stored with the virtual path `<archivePath>!/<entryPath>`. The `folder_path` column stores the virtual directory; `file_name` stores the entry name. The `!` separator is not valid in Linux file paths, making it unambiguous as a virtual path indicator.

## Goals / Non-Goals

**Goals:**
- Catalog detects `.zip`, `.tar.gz`, `.tgz` files; delegates to `ArchiveService.extractImages(archivePath, consumer)` which iterates entries, extracts image entries to temp, and calls the consumer with the temp file path and virtual folder path
- Virtual `folder_path` = `<archivePath>!/<entryDirectory>` (e.g. `/photos/album.zip!/summer/`)
- `FolderNavComponent` detects `folderPath.includes('!')` and renders an archive icon on the tree node
- `GET /api/assets/download?assetIds=...&format=zip|tar.gz`: existing zip path unchanged; new tar.gz path uses `TarArchiveOutputStream` wrapped in `GzipCompressorOutputStream`
- `ArchiveService` is injectable and reused by `asset-backup` (#61)

**Non-Goals:**
- Nested archives (zip inside zip) — extract one level only
- Write/modify archive contents
- Streaming extraction for very large archives (whole-file temp extraction is acceptable)

## Decisions

### 1. Temp directory with cleanup

**Decision:** Extract archive entries to a `Files.createTempDirectory()` location. Process them as normal image files. Delete the temp directory in a `finally` block.

**Rationale:** Reuses the entire existing image cataloging path without modification. Temp cleanup is safe because all data is persisted to the DB.

### 2. `!` as virtual path separator

**Decision:** Use `!` (exclamation mark) as the separator between the archive path and the entry path (e.g. `/photos/album.zip!/summer/`).

**Rationale:** `!` is not a valid character in Linux file paths and is therefore unambiguous. The same convention is used by Java's `jar:` URL scheme for resources inside JARs.

### 3. `commons-compress` for tar.gz only

**Decision:** Use `java.util.zip` for zip reading (already in use for downloads) and `TarArchiveInputStream` + `GzipCompressorInputStream` from `commons-compress` for tar.gz.

**Rationale:** Avoids replacing the existing zip code path. `commons-compress` is the de-facto standard for tar.gz in Java.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Large archive exhausts temp disk space | Medium | Log a warning if archive extraction would exceed a configurable threshold (default 2GB) |
| Re-cataloging an archive re-catalogs already-stored assets | Low | The existing catalog idempotency check (`assets` table `folder_path + file_name` uniqueness) prevents duplicates |
