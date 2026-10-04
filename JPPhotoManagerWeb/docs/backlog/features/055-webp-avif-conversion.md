# Feature 55 — webp-avif-conversion

Extend `ConvertAssetsUseCase` — currently PNG→JPEG only — to support JPEG/PNG→WebP and JPEG/PNG→AVIF; WebP encoding via `cwebp` invoked through `ProcessBuilder` (same pattern as #21 FFmpeg); AVIF encoding via `avifenc` through `ProcessBuilder`; the `convert_assets_directories_definitions` table gains a `target_format` VARCHAR column (Flyway migration); frontend `ConvertComponent` adds a "Target format" dropdown (JPEG / WebP / AVIF) to the directory pair configuration form
