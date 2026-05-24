## 1. Backend — MediaController

- [x] 1.1 Create `infrastructure/web/controller/MediaController.java`
- [x] 1.2 Implement `GET /api/assets/{id}/stream`: resolve file path from asset; detect MIME type from extension; use `ResourceRegion` + `HttpRange.parseRanges(rangeHeader, fileLength)` to serve byte ranges; return `206 Partial Content` for range requests, `200 OK` for full requests; set `Accept-Ranges: bytes`
- [x] 1.3 Implement `GET /api/audio/playlist/{id}`: resolve asset file path; delegate to `PlaylistParserPort` based on extension (`.m3u`/`.m3u8` → `M3uPlaylistParserAdapter`, `.pls` → `PlsPlaylistParserAdapter`); return `List<AssetDto>`

## 2. PlaylistParserPort and adapters

- [x] 2.1 Create `domain/port/out/playlist/PlaylistParserPort.java` with `List<Asset> parse(Path playlistPath)`
- [x] 2.2 Create `infrastructure/adapter/out/playlist/M3uPlaylistParserAdapter.java`: read lines; skip `#EXTM3U` and `#EXTINF` lines; for each file path line, look up by `fileName` in `AssetRepositoryPort`
- [x] 2.3 Create `PlsPlaylistParserAdapter.java`: parse INI `[playlist]` section; extract `FileN=` entries; look up by `fileName`

## 3. Catalog — playlist and audio extension acceptance

- [x] 3.1 Add `.m3u`, `.m3u8`, `.pls` to accepted extensions in `CatalogAssetsUseCaseImpl`; store as `fileType = PLAYLIST`; no metadata extraction needed; use a static playlist-icon placeholder as thumbnail
- [x] 3.2 (Already done in audio-asset-support) Confirm `.mp3`, `.flac`, `.wav`, `.aac`, `.ogg` are accepted

## 4. Backend unit tests

- [x] 4.1 Test that `MediaController` returns `206 Partial Content` when `Range` header is present
- [x] 4.2 Test that `M3uPlaylistParserAdapter.parse()` returns the correct ordered asset list
- [x] 4.3 Test that `PlsPlaylistParserAdapter.parse()` correctly reads `FileN=` entries

## 5. Frontend — AudioPlayerService

- [x] 5.1 Create `core/services/audio-player.service.ts` annotated `providedIn: 'root'`
- [x] 5.2 `private audio = new Audio()` in constructor; wire `timeupdate` event to update `currentTime` signal; `ended` event to call `next()`
- [x] 5.3 Implement signals: `currentTrack`, `queue`, `currentIndex`, `isPlaying`, `currentTime`, `duration`
- [x] 5.4 Implement methods: `play(assets, startIndex?)`, `loadFolder(folderPath)`, `loadPlaylist(assetId)`, `togglePause()`, `stop()`, `prev()`, `next()`, `seek(seconds)`
- [x] 5.5 `prev()` logic: if `audio.currentTime > 3`, call `audio.currentTime = 0`; else decrement `currentIndex` and reload

## 6. Frontend — AudioPlayerComponent

- [x] 6.1 Create `shared/components/audio-player/audio-player.component.ts` as a standalone component
- [x] 6.2 Template: top row — thumbnail `<img>`, title, artist; second row — `<button>` skip_previous, stop, play/pause, skip_next; `<input type="range">` progress slider; `currentTime / duration` counter
- [x] 6.3 `(input)` on progress slider → `audioPlayerService.seek($event.target.value)`; `(timeupdate)` on audio element updates slider value
- [x] 6.4 Add to `AppComponent` template: `@if (audioPlayer.currentTrack()) { <app-audio-player/> }` pinned to bottom of the layout

## 7. Gallery integration

- [x] 7.1 Add "Play" button to audio asset cards in the gallery; clicking calls `audioPlayerService.play([asset])`
- [x] 7.2 Add "Play all audio" action in folder toolbar; calls `audioPlayerService.loadFolder(folderPath)`

## 8. Testing and Commit

- [x] 8.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 8.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 8.3 Commit all changes (only after both test suites pass)
