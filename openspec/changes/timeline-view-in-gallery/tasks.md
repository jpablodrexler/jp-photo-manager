## 1. Backend: Timeline DTO and Endpoint

- [ ] 1.1 Create `TimelineGroupDto` record in `api/dto/` with fields `localDate`, `label`, and `List<AssetDto> assets`
- [ ] 1.2 Add `getAssetsTimeline(folderPath, page, search, dateFrom, dateTo, minRating)` method signature to `PhotoManagerFacade` interface
- [ ] 1.3 Implement the method in `PhotoManagerFacadeImpl`: query assets filtered and sorted by `fileCreationDateTime` DESC, group by `localDate`, page by day count (30 days/page), return `PaginatedData<TimelineGroup>`
- [ ] 1.4 Add `GET /api/assets/timeline` endpoint to `AssetController` mapping to the facade method and converting to `TimelineGroupDto`
- [ ] 1.5 Write unit test for the timeline facade method (filter, grouping, pagination logic) in `PhotoManagerFacadeImplTest`
- [ ] 1.6 Write `@WebMvcTest` for the new controller endpoint covering the success case and the empty-folder case

## 2. Frontend: Timeline Data Model and Service

- [ ] 2.1 Add `TimelineGroup` and `TimelineGroupDto` interfaces to `core/models/` with `localDate: string`, `label: string`, and `assets: Asset[]`
- [ ] 2.2 Add `getTimeline(folderPath, page, filters)` method to `AssetService` calling `GET /api/assets/timeline` and returning `Observable<PaginatedData<TimelineGroup>>`
- [ ] 2.3 Write a Cypress component test for the `AssetService.getTimeline()` call verifying request parameters are forwarded correctly

## 3. Frontend: Timeline View Component

- [ ] 3.1 Create `features/gallery/timeline-view/timeline-view.component.ts` as a standalone component accepting `@Input() groups: TimelineGroup[]`
- [ ] 3.2 Implement the template: month-header + day-header + thumbnail row per group, using `@for` control flow and the existing `ThumbnailComponent`
- [ ] 3.3 Add SCSS: sticky month headers (`position: sticky`), day label styling, and responsive thumbnail row wrapping
- [ ] 3.4 Write a Cypress component test for `TimelineViewComponent` covering: groups rendered with correct headers, empty state, thumbnail click emits event

## 4. Frontend: Gallery Integration

- [ ] 4.1 Add `viewType: 'grid' | 'timeline'` field to `GalleryComponent` (default `'grid'`)
- [ ] 4.2 Add Grid / Timeline icon-button toggle to the gallery toolbar template; bind active state styling
- [ ] 4.3 Add `loadTimelinePage()` method and `timelineGroups: TimelineGroup[]` state to `GalleryComponent`; wire `IntersectionObserver` sentinel to call it in timeline mode
- [ ] 4.4 Update the gallery template to show `<app-timeline-view>` when `viewType === 'timeline'` and the existing grid when `'grid'`
- [ ] 4.5 Ensure filter changes (search, date, rating, preset) reset and reload both grid and timeline state
- [ ] 4.6 Update `GalleryComponent` Cypress tests to cover the view-mode toggle and timeline rendering path

## 5. Integration Verification

- [ ] 5.1 Run `mvn test` in the backend — all tests pass
- [ ] 5.2 Run `npm test` in the frontend — all Cypress component tests pass
- [ ] 5.3 Run `npm run lint` — no lint errors
- [ ] 5.4 Start the app locally and manually verify: toggle between grid/timeline, filters apply, infinite scroll loads more groups, month headers stick while scrolling
