## Context

`ConvertAssetsServiceImpl` currently invokes Java `ImageIO` for PNG→JPEG conversion. `cwebp` and `avifenc` are separate command-line tools; both accept an input file path and output file path. The `convert_assets_directories_definitions` table stores source/destination folder pairs; adding `target_format` makes the format configurable per pair rather than globally.

## Goals / Non-Goals

**Goals:**
- Flyway V23: `ALTER TABLE convert_assets_directories_definitions ADD COLUMN target_format VARCHAR(10) NOT NULL DEFAULT 'JPEG'`
- `ImageFormat` enum: `JPEG`, `WEBP`, `AVIF`
- `ConvertAssetsServiceImpl` dispatches on `targetFormat`:
  - `JPEG`: existing Java `ImageIO` path (PNG→JPEG)
  - `WEBP`: `ProcessBuilder(["cwebp", "-q", "80", inputPath, "-o", outputPath])`
  - `AVIF`: `ProcessBuilder(["avifenc", "--speed", "6", inputPath, outputPath])`
- Both `cwebp` and `avifenc` must be in `PATH`; add `apt-get install -y webp libavif-apps` to the backend `Dockerfile`
- Frontend `ConvertComponent` directory pair form adds a `MatSelect` for `targetFormat` (JPEG / WebP / AVIF)

**Non-Goals:**
- Quality/encoding-speed configuration per conversion (hardcoded quality 80 for WebP, speed 6 for AVIF)
- Thumbnail regeneration in the new format (thumbnails remain JPEG)
- Conversion between WebP and AVIF directly

## Decisions

### 1. `ProcessBuilder` for external encoder tools

**Decision:** Invoke `cwebp` and `avifenc` via `ProcessBuilder`, the same pattern used for FFmpeg in video support.

**Rationale:** Both tools are mature, widely packaged, and produce optimally compressed output compared to Java-native alternatives. Shelling out is standard for media encoding operations.

### 2. `target_format DEFAULT 'JPEG'`

**Decision:** Existing directory pairs default to `JPEG` in the migration, preserving backward compatibility.

**Rationale:** Existing users keep the current behavior; new pairs must explicitly choose a format.

### 3. Per-asset error handling

**Decision:** If the encoder process exits with a non-zero code, log the error and skip the asset (same behavior as the existing JPEG conversion path).

**Rationale:** One corrupt or incompatible source file should not abort the entire conversion run.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| `cwebp` / `avifenc` not installed in the Docker image | Medium | Add to `Dockerfile`; document installation requirement for bare-metal deployments |
| AVIF encoding is CPU-intensive | Medium | The conversion runs `@Async` and streams progress via SSE; high CPU use is expected for batch operations |
