## Context

`StorageServiceAdapter.listFiles()` currently filters directory entries with a private `isImageFile(String fileName)` helper that checks for `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.tiff`, and `.webp`. Any file whose extension is not in that list is silently excluded from the catalog pass. No video-related logic exists anywhere in the backend or frontend.

`generateThumbnail()` in `StorageServiceAdapter` calls `loadImage()`, which uses `ImageIO` to decode image pixels into a `BufferedImage`. `ImageIO` cannot decode video containers; calling it on an `.mp4` file throws `IOException`. For the same reason, `getImageRotation()` and `getExifMetadata()` would also fail if invoked on a video file.

`CatalogFolderServiceImpl.createAsset()` calls `storagePort.generateThumbnail()` unconditionally for every file returned by `listFiles()`. Once video files are included in `listFiles()`, thumbnail generation must be branched on file type.

FFmpeg is not present in the current Docker runtime image (`eclipse-temurin:21-jre-alpine`). A single `apk add --no-cache ffmpeg` line in the Dockerfile runtime stage installs FFmpeg and its dependencies (~80 MB). The build stage already uses `maven:3.9-eclipse-temurin-21` which is a Debian-based image; the runtime stage uses Alpine. The correct package manager for the runtime stage is `apk`.

The `Asset` domain model and `AssetEntity` JPA entity have no `isVideo` field. The frontend `Asset` TypeScript interface mirrors the backend DTO but also lacks this field. The gallery viewer template hard-codes `<img [src]="currentViewerAsset.imageUrl">` with no type branching.

## Goals / Non-Goals

**Goals:**

- Include `.mp4`, `.mov`, and `.mkv` files in `StorageServiceAdapter.listFiles()` so they are picked up by the catalog pass.
- Generate a JPEG still-frame thumbnail for video files using FFmpeg via `ProcessBuilder`, writing a temp file and reading its bytes.
- Short-circuit `getImageRotation()` and `getExifMetadata()` for video files to avoid applying EXIF logic to non-image containers.
- Persist a boolean `is_video` column in the `assets` table (Flyway migration) and propagate it through the domain model, DTO, and Angular model.
- Show a play-overlay icon on video thumbnail rows in the gallery list.
- Render `<video controls>` instead of `<img>` in the gallery viewer for video assets.
- Install FFmpeg in the backend Docker runtime image.

**Non-Goals:**

- Supporting additional video formats beyond `.mp4`, `.mov`, `.mkv` (e.g., `.avi`, `.webm`).
- Streaming video content — `GET /api/assets/{id}/image` currently returns full file bytes; video streaming with `Accept-Ranges` is deferred to `video-player` (#66).
- Extracting video metadata (duration, codec, frame rate) — deferred to `video-player` (#66).
- Audio extraction or waveform thumbnails.
- Transcoding video to a web-compatible format.
- Supporting video in the sync or convert workflows.

## Decisions

### 1. FFmpeg via `ProcessBuilder` with a temp file — not a Java FFmpeg binding

**Decision:** `StorageServiceAdapter.generateVideoThumbnail()` builds an `ffmpeg` command via `ProcessBuilder`, writes the output frame to a temp file (via `Files.createTempFile`), reads the bytes, and deletes the temp file in a `finally` block. The FFmpeg command is: `ffmpeg -i <input> -ss 00:00:01 -vframes 1 -vf scale=<maxWidth>:<maxHeight>:force_original_aspect_ratio=decrease -y <tempFile>.jpg`.

**Rationale:** The project already uses `ProcessBuilder` for no other purpose, but the pattern is established in the JVM ecosystem and requires zero new Maven dependencies. Embedding FFmpeg as a Java library (e.g., `org.bytedeco:ffmpeg`) adds ~200 MB of native binaries to the artifact. The system FFmpeg installed via `apk` is already required by `video-from-images` (#58); reusing it avoids duplication. Writing to a temp file is necessary because FFmpeg cannot stream a single frame to stdout reliably across all container formats; it requires a seekable output.

**Alternative considered:** Use `org.bytedeco:ffmpeg` (JavaCV) to decode video frames in-process. Rejected due to artifact size, non-trivial API surface, and the fact that system FFmpeg will be present in the container anyway.

**Alternative considered:** Seek to frame 0 (`-ss 00:00:00`) instead of 1 second. Rejected because frame 0 is often a black or near-black frame (pre-roll, fade-in) for many consumer video files. One second into the video is a reliable heuristic for a representative frame.

### 2. `isVideoFile()` on `StoragePort` — not inferred from `Asset.isVideo` at runtime

**Decision:** `boolean isVideoFile(String fileName)` is added to the `StoragePort` interface. `StorageServiceAdapter` implements it by checking the lowercased file extension against `{".mp4", ".mov", ".mkv"}`. `CatalogFolderServiceImpl` calls `storagePort.isVideoFile(fileName)` to set `asset.setIsVideo()`.

**Rationale:** Centralizing the extension set in `StoragePort` ensures `listFiles()`, `generateThumbnail()`, and the catalog all use exactly the same set of supported extensions. If the set is scattered across classes, a future addition (e.g., `.webm`) requires changes in multiple places. The interface method makes the contract explicit and mockable in tests.

**Alternative considered:** Infer `isVideo` from `asset.fileName` wherever it is needed using a utility method. Rejected because it duplicates the extension set and scatters the definition.

### 3. `is_video` column in the `assets` table — not derived at query time from file extension

**Decision:** A non-null `BOOLEAN NOT NULL DEFAULT FALSE` column `is_video` is added to the `assets` table via `V14__add_asset_is_video.sql`. `CatalogFolderServiceImpl.createAsset()` sets it at catalog time. The HTTP response DTO and the Angular model carry `isVideo`.

**Rationale:** Deriving `isVideo` from the file name at query time is possible but couples every consumer (frontend, future search/filter, `video-player`) to extension-matching logic that may diverge. Persisting it at catalog time is consistent with how `imageRotation` and `hash` are stored: the catalog is the single authoritative pass that computes and persists asset properties.

**Alternative considered:** Compute `isVideo` dynamically in the frontend from `asset.fileName`. Rejected because it duplicates the server-side extension set in the client and fails if a file is renamed after cataloging.

### 4. `<video controls>` in the viewer — not a custom player component

**Decision:** The gallery viewer template uses `@if (currentViewerAsset.isVideo)` to render `<video [src]="currentViewerAsset.imageUrl" controls class="viewer-video">` and `@else` for the existing `<img>`. No new component is introduced.

**Rationale:** The browser's native `<video controls>` element provides play/pause, seek, volume, and fullscreen for free with no dependencies. A custom `VideoPlayerComponent` with a unified queue and right-side drawer is the subject of `video-player` (#66). This change intentionally keeps the viewer minimal so it does not overlap with that future work.

**Alternative considered:** Build a `VideoPlayerComponent` now. Rejected because `video-player` (#66) specifies a significantly richer player (queue, `MediaPlayerService`, fullscreen overlay) that would conflict with a simpler intermediate implementation.

### 5. Play-overlay icon on thumbnail rows — CSS absolute positioning over `<img>`

**Decision:** In the thumbnail list row template, a `<mat-icon class="video-overlay-icon">play_circle</mat-icon>` is conditionally rendered with `@if (asset.isVideo)` inside a `position: relative` wrapper. The icon is positioned absolutely at the center of the thumbnail image via CSS.

**Rationale:** The thumbnail image is already an `<img>` tag inside a fixed-height row. An absolutely positioned icon overlaid on the image is the standard pattern for indicating video content (used by Google Photos, YouTube thumbnails, etc.). It requires one `mat-icon` import already present in `GalleryComponent`.

**Alternative considered:** Add a `isVideo` indicator below the filename in the metadata area. Rejected because it does not visually communicate video type at thumbnail scale the way an overlay icon does.

### 6. FFmpeg installed in Alpine runtime stage with `apk` — not in the build stage

**Decision:** `RUN apk add --no-cache ffmpeg` is added to the runtime stage (`eclipse-temurin:21-jre-alpine`) of the backend `Dockerfile`. It is not added to the build stage.

**Rationale:** The build stage (`maven:3.9-eclipse-temurin-21`, Debian) compiles sources but does not run the application. FFmpeg is a runtime dependency, not a build dependency. Adding it to the build stage would increase the build layer size unnecessarily and require switching the package manager command (`apt-get` in Debian vs `apk` in Alpine). Installing it only in the runtime stage keeps the two stages independent.

**Alternative considered:** Add FFmpeg to the build stage only. Rejected — the application runs in the runtime stage.

## Risks / Trade-offs

- **FFmpeg startup latency** — each thumbnail generation spawns a new `ffmpeg` process via `ProcessBuilder`. For a folder containing hundreds of video files, catalog time increases proportionally. Each FFmpeg invocation for a one-second frame seek on a typical `.mp4` file takes 0.5–2 seconds. A folder with 100 video files adds 50–200 seconds to the catalog run. This is acceptable given catalog runs asynchronously in the background, but it differs significantly from the sub-millisecond image thumbnail path.
- **Temp file cleanup on crash** — the `finally` block deletes the temp file, but a JVM kill signal would leave orphaned temp files under the system temp directory. This is a minor housekeeping concern; OS-level temp cleanup will reclaim the files on restart.
- **FFmpeg not found** — if FFmpeg is not installed (e.g., local development outside Docker), `ProcessBuilder` throws `IOException`. The existing error handling in `CatalogFolderServiceImpl.createAsset()` logs the exception and skips the file, so catalog continues for image files. A `log.warn` in `generateVideoThumbnail()` makes the root cause visible.
- **Alpine package availability** — `ffmpeg` is available in Alpine's standard `community` repository. The `eclipse-temurin:21-jre-alpine` base image includes the Alpine package index, so `apk add --no-cache ffmpeg` resolves without any repository configuration change.
- **`is_video` backfill for existing assets** — assets cataloged before `V14` is applied will have `is_video = FALSE` (the column default). Re-running the catalog populates the column correctly for video files. Image files that already exist in the catalog retain `FALSE`, which is correct.
