# Feature 10 — mobile-responsive-layout

**As a** photo library owner using a phone, **I want** the gallery to adapt to small screens, **so that** I can browse my library comfortably on my phone.

## Acceptance Criteria
- AC1: Given a viewport below 768 px, when the app renders, then navigation collapses into a hamburger menu containing all routes; above it, inline links show.
- AC2: Given a mobile viewport, when the gallery renders, then the folder tree is an overlay drawer, closed by default, opened by a toggle and closed when I select a folder; on desktop the sidebar is persistent.
- AC3: Given a 320 px screen, when the thumbnail grid renders, then it shows two columns; on desktop the grid keeps its dimensions.
- AC4: Given the toolbar, when I use it on touch, then icon buttons are touch-friendly and long folder paths are truncated.
- AC5: Given I resize across the breakpoint, when it crosses, then the layout switches without a reload.
- AC6: Given the mobile layout, when it loads data, then the API calls are unchanged.

## Technical notes

Navigation collapses to a hamburger menu below 768 px; folder tree becomes a `MatSidenav` overlay drawer on mobile; thumbnail grid column minimum reduces to 140 px

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
