## Why

The application gives users no visibility into how their photo library is structured or how storage is distributed across folders, file types, and time. The home dashboard exposes top-level counters (total assets, total file size, duplicate count) but no breakdown that would help a user answer practical questions like "which folder consumes the most disk space?", "do I have too many raw files I forgot to convert?", or "have my uploads been consistent over the years?". Adding a dedicated analytics view surfaces this information through interactive charts without requiring the user to open a file manager or run SQL queries.

## What Changes

- **Backend — `AnalyticsData` domain model:** a new read-only value object containing four aggregate results: `folderStorage` (bytes per catalogued folder), `formatDistribution` (asset count by file extension), `photosPerMonth` (asset count by `YYYY-MM`), and `ratingDistribution` (asset count per integer star rating 0–5).
- **Backend — `GetAnalyticsUseCase`:** a single-method port in `domain/port/in/analytics/` returning `AnalyticsData`. No domain service is needed — the use case delegates directly to `AssetRepository` aggregate queries.
- **Backend — aggregate JPQL queries on `AssetRepository`:** four new methods on the `AssetRepository` port and the `JpaAssetRepository` Spring Data interface using `@Query` JPQL aggregations; no Flyway migration is required because all data lives in the existing `assets` and `folders` tables.
- **Backend — `GetAnalyticsUseCaseImpl`:** annotated `@Service @Transactional(readOnly = true)`; calls the four repository methods and assembles the response.
- **Backend — `AnalyticsController`:** thin `@RestController` at `/api/analytics` that delegates to `GetAnalyticsUseCase` and returns `AnalyticsResponseDto`.
- **Frontend — `@swimlane/ngx-charts` dependency:** added via `npm install @swimlane/ngx-charts`; provides the treemap, pie chart, bar chart, and grouped bar chart components.
- **Frontend — `AnalyticsService`:** a new service in `core/services/` calling `GET /api/analytics` and mapping the response to `AnalyticsData`.
- **Frontend — `AnalyticsComponent`:** a new standalone component under `features/analytics/` rendering four `ngx-charts` charts in a Material card grid.
- **Frontend — `/analytics` route:** a new lazy-loaded route protected by `authGuard`, registered in `app.routes.ts`.
- **Frontend — navigation link:** an "Analytics" link added to the `AppComponent` toolbar alongside the existing Gallery, Sync, Convert, Albums, and Recycle Bin links.

## Capabilities

### New Capabilities

- `storage-analytics`: Users can navigate to `/analytics` and view four interactive charts — a storage-per-folder treemap, a file-format pie chart, a photos-per-month histogram, and a rating distribution bar chart — all built from live aggregate queries over the catalogued asset data.

### Modified Capabilities

- `app-navigation`: The top navigation bar gains an "Analytics" link visible to authenticated users.

## Impact

- `JPPhotoManagerWeb/backend/src/main/java/…/domain/model/AnalyticsData.java` — new domain value object.
- `JPPhotoManagerWeb/backend/src/main/java/…/domain/port/in/analytics/GetAnalyticsUseCase.java` — new use-case interface.
- `JPPhotoManagerWeb/backend/src/main/java/…/domain/port/out/AssetRepository.java` — four new aggregate query methods added to the port interface.
- `JPPhotoManagerWeb/backend/src/main/java/…/application/usecase/analytics/GetAnalyticsUseCaseImpl.java` — new use-case implementation.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/jpa/JpaAssetRepository.java` — four new `@Query` methods.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/persistence/adapter/AssetRepositoryImpl.java` — delegate the four new port methods to the JPA repository.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/web/dto/AnalyticsResponseDto.java` — new HTTP response record.
- `JPPhotoManagerWeb/backend/src/main/java/…/infrastructure/web/controller/AnalyticsController.java` — new REST controller.
- `JPPhotoManagerWeb/frontend/package.json` — add `@swimlane/ngx-charts` dependency.
- `JPPhotoManagerWeb/frontend/src/app/core/models/analytics.model.ts` — new TypeScript interfaces.
- `JPPhotoManagerWeb/frontend/src/app/core/services/analytics.service.ts` — new Angular service.
- `JPPhotoManagerWeb/frontend/src/app/features/analytics/analytics.component.ts/html/scss` — new standalone component.
- `JPPhotoManagerWeb/frontend/src/app/app.routes.ts` — new `/analytics` lazy route.
- `JPPhotoManagerWeb/frontend/src/app/app.component.html` — add "Analytics" nav link.
