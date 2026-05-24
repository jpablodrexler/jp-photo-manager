# audio-player

A persistent bottom-bar audio player allows continuous playback while browsing. Supports single track, folder queue, and playlist files. Playback state (current track, progress) persists across route navigation.

---

## Requirements

### Requirement: Audio assets can be streamed with byte-range support

`GET /api/assets/{id}/stream` SHALL serve the audio file with `Accept-Ranges: bytes` support so the browser can seek within the track without downloading the entire file.

#### Scenario: Seek request uses a Range header

- **GIVEN** an audio asset is playing in the browser
- **WHEN** the user seeks to the middle of the track
- **THEN** the browser sends a `Range: bytes=X-Y` request and the server responds with `206 Partial Content` with the requested byte range

### Requirement: Playlist files can be parsed and queued

`GET /api/audio/playlist/{id}` SHALL parse an M3U, M3U8, or PLS playlist file and return an ordered list of audio assets.

#### Scenario: M3U playlist is parsed into a list of assets

- **GIVEN** a cataloged `.m3u` playlist file referencing 5 audio files
- **WHEN** `GET /api/audio/playlist/{id}` is called
- **THEN** the response is a JSON array of 5 `AssetDto` objects in playlist order

### Requirement: AudioPlayerComponent is persistently shown when a track is loaded

The `AudioPlayerComponent` at the bottom of `AppComponent` SHALL be visible whenever `audioPlayerService.currentTrack()` is non-null, and SHALL persist across route navigation.

#### Scenario: Player persists when navigating to another route

- **GIVEN** a track is playing and the user is on `/gallery`
- **WHEN** the user navigates to `/sync`
- **THEN** the audio continues playing and the bottom player bar remains visible

### Requirement: Playback controls work correctly

The player SHALL support play/pause, stop, previous, next, and seek via the progress slider.

#### Scenario: prev() restarts the track when played for more than 3 seconds

- **GIVEN** the current track has been playing for 10 seconds
- **WHEN** the user clicks "Previous"
- **THEN** the track restarts from the beginning (does not jump to the previous track)

#### Scenario: prev() goes to previous track when played for less than 3 seconds

- **GIVEN** the current track has been playing for 2 seconds
- **WHEN** the user clicks "Previous"
- **THEN** the queue steps back to the previous track

### Requirement: Folder queue loads all audio assets in the folder

`audioPlayerService.loadFolder(folderPath)` SHALL load all audio assets in the specified folder (ordered by file name) as the queue and start playback from the first track.

#### Scenario: Playing a folder queue

- **GIVEN** a folder `/music/album` contains 10 audio assets
- **WHEN** `loadFolder('/music/album')` is called
- **THEN** the queue contains all 10 audio assets in file-name order and playback begins from the first track
