# Feature 49 — auto-tagging

**As a** photo library owner, **I want** photos tagged automatically with their year and camera make, **so that** I can browse by year and camera without tagging by hand.

## Acceptance Criteria
- AC1: Given a photo with a date taken, when it is cataloged, then a year tag is applied; with no date, none.
- AC2: Given a camera make, when it is cataloged, then a lowercase tag from its first word is applied (for example canon, sony or apple for an Apple iPhone).
- AC3: Given an auto-applied tag, when I look at it, then it is indistinguishable from a manual one and I can remove it.

## Technical notes

During cataloging, automatically apply tags derived from EXIF data: the year from `dateTaken` and camera make normalised to lowercase (e.g. `canon`, `sony`, `apple`); tags are written through the existing `asset_tags` table and tag infrastructure; auto-applied tags are indistinguishable from manual ones and can be removed by the user; no new schema required beyond what the tag feature already provides

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
