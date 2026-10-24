# Feature 65 — audio-player

**As a** photo library owner, **I want** a persistent audio player with folder queues and playlists, **so that** music keeps playing while I browse the library.

## Acceptance Criteria
- AC1: Given an audio asset, when I play it, then a player toolbar appears at the bottom and persists when I navigate to another route.
- AC2: Given the queue modes single, folder and playlist, when I choose one, then tracks play in that order, including playing a folder queue.
- AC3: Given the player, when I press previous after more than 3 seconds, then the track restarts; before 3 seconds, it goes to the previous track.
- AC4: Given a seek, when I drag the position, then the request uses a Range header and the server answers with partial content.
- AC5: Given an M3U, M3U8 or PLS file, when it is cataloged, then it is parsed into a list of assets.
- AC6: Given the catalog, when it runs, then audio and playlist extensions are accepted.

## Technical notes

Persistent audio player toolbar fixed at the bottom of `AppComponent` (below all routes); three queue modes: single, folder, playlist; `AudioPlayerService` (`providedIn: 'root'`) wraps a private `HTMLAudioElement` and exposes Angular signals (`currentTrack`, `queue`, `currentIndex`, `isPlaying`, `currentTime`, `duration`); `AudioPlayerComponent` in `shared/components/audio-player/` shows only when a track is loaded; audio streaming via `GET /api/assets/{id}/audio` with `Accept-Ranges: bytes` for seeking; `AudioController` in `infrastructure/web/controller/` handles `/audio/{id}` and `/audio/playlist/{id}`; two playlist parser adapters (`M3uPlaylistParserAdapter`, `PlsPlaylistParserAdapter`) implement `PlaylistParserPort`; catalog service extended to accept audio extensions (`.mp3`, `.flac`, `.wav`, `.aac`, `.ogg`) and playlist extensions (`.m3u`, `.m3u8`, `.pls`)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
