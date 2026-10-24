# Feature 25 — on-push-change-detection

**As a** maintainer of the project, **I want** all components to use OnPush change detection, **so that** the gallery renders fewer checks and stays fast.

## Acceptance Criteria
- AC1: Given the thumbnail, gallery and every feature component, when they are declared, then they use OnPush.
- AC2: Given state changes, when lists or selections are updated, then they are replaced immutably so the view updates.
- AC3: Given catalog and sync progress events, when they arrive outside the Angular zone, then the template still updates.

## Technical notes

Apply `ChangeDetectionStrategy.OnPush` to all 18 components; replace mutable state mutations with immutable assignments so Angular's OnPush check can detect changes; prioritise `ThumbnailComponent` (one instance per visible image) and `GalleryComponent` (the most complex); inject `ChangeDetectorRef` where manual `markForCheck()` calls are needed (e.g. after SSE events or async callbacks outside the Angular zone)

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
