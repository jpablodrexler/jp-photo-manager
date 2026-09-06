# Mutation Testing Report (backend) — 2026-08-15

**Commit:** cdb2dbf
**Generated:** 2026-08-15T03:28:22Z
**Scope:** com.jpablodrexler.photomanager.application.usecase.* (65 classes)
**Tool:** PIT (`mvn org.pitest:pitest-maven:mutationCoverage@mutation-report`)

**Line coverage:** 97% (1016/1049)
**Mutation coverage:** 84% (374/443)
**Test strength:** 87%
**Total mutants:** 443
**Killed:** 374  **Survived:** 54  **No coverage:** 15  **Timed out:** 0  **Non-viable:** 0  **Other:** 0

A survived mutant means the test suite ran and passed even though the mutated line changed the code's behavior — the tests exercise that code path but don't actually assert on the behavior a bug there would break. A "no coverage" mutant means no test reaches that line at all.

## Per-class results (worst mutation score first)

| Class | Score | Total | Killed | Survived | No cov | Timed out |
| --- | --- | --- | --- | --- | --- | --- |
| com.jpablodrexler.photomanager.application.usecase.search.CreateSearchPresetUseCaseImpl | 33.3% | 6 | 2 | 4 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.CreateAlbumUseCaseImpl | 42.9% | 7 | 3 | 4 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | 56.2% | 16 | 9 | 5 | 2 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | 60.0% | 25 | 15 | 10 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | 65.6% | 32 | 21 | 10 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | 66.7% | 24 | 16 | 6 | 2 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.DownloadAssetsUseCaseImpl | 71.4% | 7 | 5 | 2 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.GetAlbumAssetsUseCaseImpl | 75.0% | 4 | 3 | 0 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.GetSubFoldersUseCaseImpl | 75.0% | 4 | 3 | 1 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.user.CreateUserUseCaseImpl | 75.0% | 8 | 6 | 2 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.GetAlbumsUseCaseImpl | 80.0% | 5 | 4 | 0 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.GetActiveSessionsUseCaseImpl | 80.0% | 5 | 4 | 1 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.catalog.CatalogAssetsUseCaseImpl | 80.0% | 5 | 4 | 0 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.MoveAssetsUseCaseImpl | 84.0% | 25 | 21 | 2 | 2 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetsTimelineUseCaseImpl | 85.7% | 7 | 6 | 1 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser | 88.9% | 36 | 32 | 1 | 3 | 0 |
| com.jpablodrexler.photomanager.application.usecase.catalog.GetDuplicatedAssetsUseCaseImpl | 88.9% | 9 | 8 | 0 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.RevokeSessionUseCaseImpl | 90.0% | 10 | 9 | 0 | 1 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.RenameAssetsUseCaseImpl | 92.9% | 42 | 39 | 3 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetImageUseCaseImpl | 93.1% | 29 | 27 | 2 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.AddAssetsToAlbumUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.AlbumAssetFilterFactory | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.DeleteAlbumUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.GetAlbumSummaryUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.RemoveAssetsFromAlbumUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.album.UpdateAlbumUseCaseImpl | 100.0% | 6 | 6 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.analytics.GetAnalyticsUseCaseImpl | 100.0% | 9 | 9 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.DeleteAssetsUseCaseImpl | 100.0% | 5 | 5 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetExifUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetThumbnailUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetsUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.GetPlaylistUseCaseImpl | 100.0% | 5 | 5 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.RateAssetUseCaseImpl | 100.0% | 4 | 4 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.ReprocessAssetUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.asset.StreamAssetUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.audit.GetAuditLogUseCaseImpl | 100.0% | 4 | 4 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.LoginUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.LogoutUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.auth.RefreshTokenUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.convert.GetConvertConfigUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.convert.SaveConvertConfigUseCaseImpl | 100.0% | 5 | 5 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.GetDrivesUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.GetFolderIdByPathUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.GetInitialFolderUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.GetRecentTargetPathsUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.folder.PruneDeletedFoldersUseCaseImpl | 100.0% | 8 | 8 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.home.GetHomeStatsUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.preference.GetUserPreferenceUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.preference.SaveUserPreferenceUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.recycle.GetDeletedAssetsUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.recycle.PurgeAssetsUseCaseImpl | 100.0% | 4 | 4 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.recycle.RestoreAssetsUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.search.DeleteSearchPresetUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.search.GetSearchPresetsUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.sync.GetSyncConfigUseCaseImpl | 100.0% | 1 | 1 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.sync.SaveSyncConfigUseCaseImpl | 100.0% | 5 | 5 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.tag.AddTagToAssetUseCaseImpl | 100.0% | 8 | 8 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.tag.BulkAddTagUseCaseImpl | 100.0% | 5 | 5 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.tag.BulkRemoveTagUseCaseImpl | 100.0% | 6 | 6 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.tag.ListTagsUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.tag.RemoveTagFromAssetUseCaseImpl | 100.0% | 9 | 9 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.user.DeleteUserUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.user.GetCurrentUserUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.user.ListUsersUseCaseImpl | 100.0% | 2 | 2 | 0 | 0 | 0 |
| com.jpablodrexler.photomanager.application.usecase.user.UpdatePasswordUseCaseImpl | 100.0% | 3 | 3 | 0 | 0 | 0 |

## 69 surviving / uncovered mutant(s)

| Class | Method | Line | Mutator | Status |
| --- | --- | --- | --- | --- |
| com.jpablodrexler.photomanager.application.usecase.album.CreateAlbumUseCaseImpl | execute | 29 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.album.CreateAlbumUseCaseImpl | execute | 30 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.album.CreateAlbumUseCaseImpl | execute | 31 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.album.CreateAlbumUseCaseImpl | execute | 32 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.album.GetAlbumAssetsUseCaseImpl | lambda$execute$0 | 28 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.album.GetAlbumsUseCaseImpl | countAssets | 40 | PrimitiveReturnsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | buildOutputFileName | 103 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | execute | 65 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | execute | 67 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | lambda$execute$0 | 43 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | lambda$execute$1 | 86 | NullReturnValsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | toJpegBytes | 112 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | toJpegBytes | 117 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | validateCropBounds | 92 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | validateCropBounds | 92 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | validateCropBounds | 93 | MathMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl | validateCropBounds | 94 | MathMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.DownloadAssetsUseCaseImpl | execute | 59 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.DownloadAssetsUseCaseImpl | execute | 60 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetImageUseCaseImpl | detectMimeType | 49 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetImageUseCaseImpl | detectMimeType | 56 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.GetAssetsTimelineUseCaseImpl | execute | 53 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.MoveAssetsUseCaseImpl | execute | 50 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.MoveAssetsUseCaseImpl | lambda$execute$1 | 53 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.asset.MoveAssetsUseCaseImpl | revertFileOperation | 91 | VoidMethodCallMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.asset.MoveAssetsUseCaseImpl | saveRecentTargetPath | 114 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.RenameAssetsUseCaseImpl | applyRenames | 192 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.RenameAssetsUseCaseImpl | baseNameOf | 128 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.RenameAssetsUseCaseImpl | extensionOf | 133 | ConditionalsBoundaryMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 59 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 60 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 61 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 62 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 63 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 64 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 65 | NegateConditionalsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 65 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 67 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl | execute | 68 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser | detectBrowser | 35 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser | detectOs | 57 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser | detectOs | 60 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser | detectOs | 65 | EmptyObjectReturnValsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.auth.GetActiveSessionsUseCaseImpl | sortKey | 46 | NegateConditionalsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.auth.RevokeSessionUseCaseImpl | lambda$currentUser$0 | 60 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.catalog.CatalogAssetsUseCaseImpl | execute | 61 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.catalog.GetDuplicatedAssetsUseCaseImpl | lambda$execute$0 | 33 | BooleanTrueReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | convertDirectory | 57 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | convertDirectory | 58 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | convertDirectory | 62 | NegateConditionalsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | convertDirectory | 63 | VoidMethodCallMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | convertDirectory | 84 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | execute | 46 | NullReturnValsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.convert.ConvertAssetsUseCaseImpl | execute | 49 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.folder.GetSubFoldersUseCaseImpl | execute | 24 | EmptyObjectReturnValsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.search.CreateSearchPresetUseCaseImpl | execute | 37 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.search.CreateSearchPresetUseCaseImpl | execute | 38 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.search.CreateSearchPresetUseCaseImpl | execute | 39 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.search.CreateSearchPresetUseCaseImpl | execute | 40 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | execute | 48 | NullReturnValsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | execute | 51 | NullReturnValsMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 60 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 64 | NegateConditionalsMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 65 | VoidMethodCallMutator | NO_COVERAGE |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 71 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 74 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl | syncDirectories | 75 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.user.CreateUserUseCaseImpl | execute | 34 | VoidMethodCallMutator | SURVIVED |
| com.jpablodrexler.photomanager.application.usecase.user.CreateUserUseCaseImpl | execute | 36 | VoidMethodCallMutator | SURVIVED |

