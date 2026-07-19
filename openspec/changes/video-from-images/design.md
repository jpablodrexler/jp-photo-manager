## Context

FFmpeg is already installed in the backend Docker container via `video-file-support` (#21). The command pattern for a slideshow is: `ffmpeg -framerate 1/{duration} -i frame%04d.jpg [-i music.mp3] -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p output.mp4`. Images must be staged in a temp directory with sequentially numbered filenames before FFmpeg is invoked.

## Goals / Non-Goals

**Goals:**
- `POST /api/assets/create-video` accepts `{ assetIds: [long], slideDuration: int, musicAssetId?: long, outputFolder: string }` and returns an `SseEmitter` streaming progress events
- Use case:
  1. Resolve asset file paths from `assetIds` in order
  2. Copy images to a temp directory as `frame0001.jpg`, `frame0002.jpg`, ...
  3. If `musicAssetId` is provided, resolve its file path
  4. Build FFmpeg command: `ffmpeg -framerate 1/{slideDuration} -i <tmp>/frame%04d.jpg [-i <musicPath>] -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p <outputFolder>/<timestamp>.mp4`
  5. Stream FFmpeg's stderr progress output via SSE
  6. On completion, catalog the output MP4 as a new asset
- SSE events: `{ phase: "PREPARING" | "ENCODING" | "CATALOGING" | "DONE", progress: 0-100 }`
- Frontend wizard: step 1 (select images + ordering), step 2 (duration + music), step 3 (output folder + run with SSE progress)

**Non-Goals:**
- Transitions between slides (FFmpeg filters for transitions add significant complexity)
- Multiple audio tracks or volume control
- Custom output file name (uses `<ISO-8601-timestamp>.mp4`)

## Decisions

### 1. Staging images in a temp directory

**Decision:** Copy (not symlink) the selected images to a temp directory with sequential names before invoking FFmpeg.

**Rationale:** FFmpeg's `-i frame%04d.jpg` requires sequentially numbered files. Symlinking would work but copying is simpler and avoids path-escaping issues.

### 2. FFmpeg stderr for progress

**Decision:** Parse FFmpeg stderr for `frame=N` to compute progress percentage (`N / totalFrames * 100`).

**Rationale:** FFmpeg writes progress to stderr in a parseable format. The same pattern is used in `video-file-support`.

### 3. Catalog output as a new asset after completion

**Decision:** After FFmpeg exits successfully, invoke the catalog service on the output file path.

**Rationale:** The output becomes a first-class asset in the gallery, viewable with the existing video viewer and manageable with all standard asset operations.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Large number of images makes the temp directory large | Medium | Clean up temp directory in a `finally` block after FFmpeg exits |
| FFmpeg not installed (missing #21 dependency) | High | Document the dependency; the endpoint returns 500 with an actionable message if FFmpeg is not found |
