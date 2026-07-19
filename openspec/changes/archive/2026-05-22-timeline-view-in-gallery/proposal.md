## Why

The gallery currently displays photos in a flat grid regardless of sort order. When users sort by date, there is no visual grouping — photos from different days or months appear in an undifferentiated stream. A timeline view groups photos by date, making chronological browsing natural and intuitive, consistent with how most photo management tools (Google Photos, iPhoto) present a date-sorted library.

## What Changes

- Add a `GET /api/assets/timeline` backend endpoint that returns assets grouped by date, with pagination per group.
- Introduce a **Timeline** view mode toggle in the gallery toolbar (alongside the existing grid view).
- Render timeline groups with sticky date headers (month + day label) and a thumbnail row per day.
- Preserve all existing gallery filters (search, date range, min rating, search presets) in timeline mode.
- Infinite scroll continues to work: new date groups load as the user scrolls.

## Capabilities

### New Capabilities
- `gallery-timeline-view`: A chronological, date-grouped view mode in the gallery that groups assets by day and displays collapsible month/day headers alongside the existing flat-grid view.

### Modified Capabilities

## Impact

- **Backend:** New endpoint `GET /api/assets/timeline` in `AssetController`; new application-layer method in `PhotoManagerFacade`; new response DTO `TimelineGroupDto`.
- **Frontend:** New `timeline-view` sub-component in `features/gallery/`; `GalleryComponent` gains a `viewType: 'grid' | 'timeline'` toggle; `AssetService` gains a `getTimeline()` method.
- **No breaking changes** to existing `/api/assets` endpoint or any other route.
