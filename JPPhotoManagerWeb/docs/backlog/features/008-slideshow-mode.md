# Feature 8 — slideshow-mode

**As a** photo library owner, **I want** a slideshow mode that advances through photos automatically, **so that** I can present or review a folder hands-free.

## Acceptance Criteria
- AC1: Given the viewer or thumbnail toolbar, when I choose slideshow, then it starts; the button is hidden when no assets are loaded.
- AC2: Given the slideshow plays, when the interval elapses, then it advances to the next image; at the last asset it stops and loads no additional pages.
- AC3: Given the slideshow, when I pause, resume or change the interval, then playback follows; the progress bar fills during playback, pauses with it and resets on each advance.
- AC4: Given I step manually, when I use next or previous, then auto-advance pauses.
- AC5: Given keyboard control, when I press Escape, then the slideshow exits; when I press Space, then play/pause toggles.
- AC6: Given I navigate away, when the view is destroyed, then the timer is cancelled.

## Technical notes

Third `ViewMode` in the gallery that auto-advances through assets on a configurable interval; animated progress bar; keyboard shortcuts; pause on manual navigation

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
