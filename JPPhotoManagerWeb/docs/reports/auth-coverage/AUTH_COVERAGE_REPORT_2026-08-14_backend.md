# Auth Coverage Report (backend) — 2026-08-14

**Commit:** 64e7da3
**Generated:** 2026-08-14T23:27:49Z
**Scope:** 63 endpoints across infrastructure/web/controller/*.java, resolved against SecurityConfig.java's ordered requestMatchers rules

Factual snapshot of which SecurityConfig rule governs each endpoint — not a pass/fail gate. An endpoint resolving to the `anyRequest` fallback isn't automatically wrong (SecurityConfig's own fallback is `permitAll()`, and some endpoints are intentionally public), but it means no explicit `requestMatchers` rule covers it — worth a second look if that endpoint touches user data. `@PreAuthorize` (if present) is a real, separate, tighter restriction this table doesn't attempt to merge into the "Governing rule" column — read both.

| Method | Path | Controller | Governing rule | @PreAuthorize |
| --- | --- | --- | --- | --- |
| GET | /api/admin/users | UserAdminController | hasRole("ADMIN") | - |
| POST | /api/admin/users | UserAdminController | hasRole("ADMIN") | - |
| DELETE | /api/admin/users/{id} | UserAdminController | hasRole("ADMIN") | - |
| PATCH | /api/admin/users/{id}/password | UserAdminController | hasRole("ADMIN") | - |
| GET | /api/albums | AlbumController | authenticated() | - |
| POST | /api/albums | AlbumController | authenticated() | - |
| DELETE | /api/albums/{id} | AlbumController | authenticated() | - |
| GET | /api/albums/{id} | AlbumController | authenticated() | - |
| PUT | /api/albums/{id} | AlbumController | authenticated() | - |
| DELETE | /api/albums/{id}/assets | AlbumController | authenticated() | - |
| POST | /api/albums/{id}/assets | AlbumController | authenticated() | - |
| GET | /api/analytics | AnalyticsController | authenticated() | - |
| DELETE | /api/assets | AssetController | authenticated() | hasRole('ADMIN') |
| GET | /api/assets | AssetController | authenticated() | - |
| GET | /api/assets/catalog | AssetController | authenticated() | hasRole('ADMIN') |
| GET | /api/assets/catalog/observe | AssetController | authenticated() | - |
| POST | /api/assets/download | AssetController | authenticated() | - |
| GET | /api/assets/duplicates | AssetController | authenticated() | - |
| POST | /api/assets/move | AssetController | authenticated() | - |
| POST | /api/assets/rename | AssetController | authenticated() | - |
| DELETE | /api/assets/tags/bulk | AssetController | authenticated() | - |
| POST | /api/assets/tags/bulk | AssetController | authenticated() | - |
| GET | /api/assets/timeline | AssetController | authenticated() | - |
| POST | /api/assets/upload | AssetController | authenticated() | - |
| GET | /api/assets/upload/{assetId}/observe | AssetController | authenticated() | - |
| GET | /api/assets/{assetId}/exif | AssetController | authenticated() | - |
| GET | /api/assets/{assetId}/image | AssetController | authenticated() | - |
| GET | /api/assets/{assetId}/thumbnail | AssetController | authenticated() | - |
| POST | /api/assets/{id}/crop | AssetController | authenticated() | - |
| PATCH | /api/assets/{id}/rating | AssetController | authenticated() | - |
| POST | /api/assets/{id}/reprocess | AssetController | authenticated() | hasRole('ADMIN') |
| GET | /api/assets/{id}/stream | MediaController | authenticated() | - |
| DELETE | /api/assets/{id}/tags | AssetController | authenticated() | - |
| POST | /api/assets/{id}/tags | AssetController | authenticated() | - |
| GET | /api/audio/playlist/{id} | MediaController | authenticated() | - |
| GET | /api/audit-log | AuditLogController | authenticated() | - |
| POST | /api/auth/login | AuthController | permitAll() | - |
| POST | /api/auth/logout | AuthController | permitAll() | - |
| GET | /api/auth/me | AuthController | authenticated() | - |
| POST | /api/auth/refresh | AuthController | permitAll() | - |
| DELETE | /api/auth/sessions | AuthController | authenticated() | - |
| GET | /api/auth/sessions | AuthController | authenticated() | - |
| DELETE | /api/auth/sessions/{id} | AuthController | authenticated() | - |
| GET | /api/convert/configuration | ConvertController | authenticated() | - |
| PUT | /api/convert/configuration | ConvertController | authenticated() | - |
| GET | /api/convert/run | ConvertController | authenticated() | - |
| GET | /api/folders | FolderController | authenticated() | - |
| GET | /api/folders/drives | FolderController | authenticated() | - |
| GET | /api/folders/initial | FolderController | authenticated() | - |
| GET | /api/folders/recent-paths | FolderController | authenticated() | - |
| GET | /api/home/stats | HomeController | authenticated() | - |
| GET | /api/preferences | UserPreferenceController | authenticated() | - |
| PUT | /api/preferences | UserPreferenceController | authenticated() | - |
| DELETE | /api/recycle-bin | RecycleBinController | authenticated() | - |
| GET | /api/recycle-bin | RecycleBinController | authenticated() | - |
| POST | /api/recycle-bin/restore | RecycleBinController | authenticated() | - |
| GET | /api/search-presets | SearchPresetController | authenticated() | - |
| POST | /api/search-presets | SearchPresetController | authenticated() | - |
| DELETE | /api/search-presets/{id} | SearchPresetController | authenticated() | - |
| GET | /api/sync/configuration | SyncController | authenticated() | - |
| PUT | /api/sync/configuration | SyncController | authenticated() | - |
| GET | /api/sync/run | SyncController | authenticated() | - |
| GET | /api/tags | TagController | authenticated() | - |

