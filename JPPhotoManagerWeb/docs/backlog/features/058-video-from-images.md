# Feature 58 — video-from-images

**As a** photo library owner, **I want** to create a video from selected images with optional background music, **so that** I can turn an album into a slideshow video.

## Acceptance Criteria
- AC1: Given the wizard, when I choose an ordered list of images and a per-slide duration, then a video is generated.
- AC2: Given a music file, when I add it, then it plays as the background track; without one, the video has no sound.
- AC3: Given generation runs, when it progresses, then progress is streamed to the wizard.
- AC4: Given the video is saved to the chosen folder, when generation ends, then it is cataloged and appears in the gallery.
- AC5: Given a missing source image, when generation runs, then the operation fails with an error.

## Technical notes

New wizard component lets the user select an ordered list of images, set a per-slide duration, choose a background music file (uploaded on the spot or selected from a cataloged audio asset once `audio-asset-support` #59 is in place), and trigger video generation; the backend invokes FFmpeg via `ProcessBuilder` (`ffmpeg -framerate 1/{duration} -i frame%04d.jpg -i music.mp3 -c:v libx264 -c:a aac -shortest -pix_fmt yuv420p output.mp4`), streams progress via SSE (same pattern as catalog/sync/convert), and saves the output to a user-chosen folder where it is auto-cataloged as a video asset; hard dependency on `video-file-support` (#21) for FFmpeg presence in the container

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
