# Feature 67 — event-auto-grouping

**As a** photo library owner, **I want** photos grouped automatically into events by time gaps, **so that** I browse my library by occasion without organising it by hand.

## Acceptance Criteria
- AC1: Given photos with a date taken, when events are computed, then consecutive shots within the gap threshold (default 2 hours) belong to one event.
- AC2: Given /events, when it opens, then events are listed as date-range cards with a cover photo and asset count.
- AC3: Given an event, when I rename it, set a custom cover, or merge or split events, then only my overrides are stored.
- AC4: Given ungrouped assets, when the list is queried, then their event is inferred from date taken at query time.
- AC5: Given the timeline view, when I open its toolbar, then events are available as an alternate grouping mode.

## Technical notes

Automatically cluster photos into events based on configurable time gaps between consecutive shots (default threshold: 2 hours); a new `/events` route lists events as date-range cards showing a cover photo and asset count; users can rename events, set a custom cover photo, and merge or split events manually; ungrouped assets infer their event from `dateTaken` at query time — the `user_events` table (`id`, `userId`, `name`, `startAt`, `endAt`, `coverAssetId`) stores only user overrides, keeping the migration small; `GET /api/events` returns computed events for the authenticated user with asset counts; integrates with `timeline-view` (#18) as an alternate grouping mode selectable from the view toolbar

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
