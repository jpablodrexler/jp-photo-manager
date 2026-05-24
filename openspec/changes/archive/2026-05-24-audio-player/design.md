## Context

Angular signals (`signal()`, `computed()`) are the reactive primitive for playback state. `AudioPlayerService` wraps a `new Audio()` element created once in the constructor. The `MediaController` uses Spring MVC `ResourceRegion` streaming for `Accept-Ranges` support — the same pattern as `ResourceHttpMessageConverter`. Playlist files (`.m3u`, `.m3u8`, `.pls`) are regular cataloged assets whose content is parsed by `PlaylistParserPort` adapters.

## Goals / Non-Goals

**Goals:**
- `AudioPlayerService` (singleton): private `HTMLAudioElement`; signals: `currentTrack: signal<Asset | null>`, `queue: signal<Asset[]>`, `currentIndex: signal<number>`, `isPlaying: signal<boolean>`, `currentTime: signal<number>`, `duration: signal<number>`; methods: `play(assets, startIndex?)`, `loadFolder(folderPath)`, `loadPlaylist(assetId)`, `togglePause()`, `stop()`, `prev()`, `next()`, `seek(seconds)`; `prev()` restarts if `currentTime > 3`
- `AudioPlayerComponent`: visible via `@if (audioPlayer.currentTrack())` in `AppComponent`; top row: thumbnail (`thumbnailUrl`), title (`asset_audio.title ?? fileName`), artist; second row: skip_previous, stop, play_arrow/pause, skip_next control buttons; progress `<input type="range">` updated on `timeupdate`; `currentTime / duration` counter
- `MediaController`:
  - `GET /api/assets/{id}/stream` — serve file via `ResourceRegion` with `Accept-Ranges: bytes`; detect MIME from extension
  - `GET /api/audio/playlist/{id}` — resolve asset path; delegate to `PlaylistParserPort` adapter; return `List<AssetDto>`
- `PlaylistParserPort` implementations:
  - `M3uPlaylistParserAdapter`: skip `#EXTM3U` and `#EXTINF` lines; resolve each file path against `assets` table by `folder_path + '/' + file_name`
  - `PlsPlaylistParserAdapter`: read INI `[playlist]` section; extract `FileN=` entries; resolve against `assets` table
- Catalog service extended to accept `.m3u`, `.m3u8`, `.pls` extensions (cataloged as `PLAYLIST` type assets)
- `loadFolder(folderPath)` calls `GET /api/assets?folderPath=&type=AUDIO` sorted by file name

**Non-Goals:**
- Equalizer or audio effects
- Shuffle mode (queue reordering)
- Crossfade between tracks

## Decisions

### 1. Signals for playback state

**Decision:** Use Angular 17+ `signal()` for all playback state instead of `BehaviorSubject`.

**Rationale:** Signals integrate natively with `@if`/`@for` in templates and avoid explicit subscription management in `ngOnDestroy`.

### 2. `ResourceRegion` streaming for byte-range support

**Decision:** Use `ResourceRegion` + `HttpRange.parseRanges()` in `MediaController` to honour `Range: bytes=X-Y` headers.

**Rationale:** Without byte-range support, the HTML5 `<audio>` element cannot seek in the track without downloading the entire file from the beginning.

### 3. Single `HTMLAudioElement` in the service

**Decision:** Create `private audio = new Audio()` once in the `AudioPlayerService` constructor. Swap the `src` attribute to change tracks.

**Rationale:** Creating a new `Audio()` per track causes a re-initialisation delay. Swapping `src` preserves the existing buffered state in many browsers.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Browser autoplay policy blocks audio without user gesture | Medium | Only call `audio.play()` after a user interaction (clicking a play button in the gallery or player bar) |
| Playlist files reference local absolute paths not in the DB | Low | `PlaylistParserPort` resolves by file name only (strips directory) as a fallback |
