# Complexity & File-Size Hotspots Report (backend) — 2026-08-15

**Commit:** a62ecce
**Generated:** 2026-08-15T20:28:53Z
**Scope:** backend/src/main/java

**Files scanned:** 405
**Methods analyzed:** 872 (average complexity: 1.63; gate threshold: 15, see `mvn pmd:check`)
**Average file size:** 35 lines

## Top 20 methods by cyclomatic complexity

| Complexity | Method | Line |
| --- | --- | --- |
| 32 | com.jpablodrexler.photomanager.infrastructure.service.StorageServiceAdapter#getExifMetadata | 305 |
| 15 | com.jpablodrexler.photomanager.application.usecase.asset.GetAssetImageUseCaseImpl#detectMimeType | 42 |
| 12 | com.jpablodrexler.photomanager.application.usecase.asset.RenameAssetsUseCaseImpl#checkFolderCollisions | 145 |
| 11 | com.jpablodrexler.photomanager.application.usecase.asset.UploadAssetUseCaseImpl#sanitizeAndValidate | 88 |
| 10 | com.jpablodrexler.photomanager.infrastructure.service.StorageServiceAdapter#loadImage | 138 |
| 9 | com.jpablodrexler.photomanager.infrastructure.service.StorageServiceAdapter#getImageRotation | 256 |
| 9 | com.jpablodrexler.photomanager.infrastructure.persistence.adapter.AssetRepositoryImpl#findFiltered | 80 |
| 9 | com.jpablodrexler.photomanager.application.usecase.sync.SyncAssetsUseCaseImpl#syncFolder | 81 |
| 9 | com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser#detectOs | 49 |
| 8 | com.jpablodrexler.photomanager.infrastructure.service.PlsPlaylistParserServiceAdapter#parse | 30 |
| 8 | com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService#extractAlbumArt | 53 |
| 8 | com.jpablodrexler.photomanager.infrastructure.persistence.adapter.AuditLogRepositoryImpl#buildFilterQuery | 51 |
| 8 | com.jpablodrexler.photomanager.infrastructure.batch.CatalogItemWriteListener#afterJob | 32 |
| 8 | com.jpablodrexler.photomanager.application.usecase.asset.CropAssetUseCaseImpl#validateCropBounds | 90 |
| 7 | com.jpablodrexler.photomanager.infrastructure.persistence.adapter.AssetRepositoryImpl#findAllFilteredSortedByDateDesc | 102 |
| 7 | com.jpablodrexler.photomanager.application.usecase.auth.DeviceHintParser#detectBrowser | 30 |
| 6 | com.jpablodrexler.photomanager.infrastructure.service.StorageServiceAdapter#applyRotation | 460 |
| 6 | com.jpablodrexler.photomanager.infrastructure.service.M3uPlaylistParserServiceAdapter#parse | 30 |
| 6 | com.jpablodrexler.photomanager.infrastructure.service.CatalogFolderServiceAdapter#createAsset | 65 |
| 6 | com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService#extract | 30 |

## Top 20 files by line count

| Lines | File |
| --- | --- |
| 508 | src/main/java/com/jpablodrexler/photomanager/infrastructure/service/StorageServiceAdapter.java |
| 486 | src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AssetController.java |
| 318 | src/main/java/com/jpablodrexler/photomanager/infrastructure/persistence/adapter/AssetRepositoryImpl.java |
| 257 | src/main/java/com/jpablodrexler/photomanager/config/AppConfig.java |
| 227 | src/main/java/com/jpablodrexler/photomanager/infrastructure/service/CatalogFolderServiceAdapter.java |
| 227 | src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/RenameAssetsUseCaseImpl.java |
| 194 | src/main/java/com/jpablodrexler/photomanager/infrastructure/web/exception/GlobalExceptionHandler.java |
| 181 | src/main/java/com/jpablodrexler/photomanager/infrastructure/persistence/jpa/JpaAssetRepository.java |
| 179 | src/main/java/com/jpablodrexler/photomanager/infrastructure/kafka/AuditLogKafkaListener.java |
| 176 | src/main/java/com/jpablodrexler/photomanager/infrastructure/batch/CatalogAssetItemProcessor.java |
| 173 | src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AuthController.java |
| 168 | src/main/java/com/jpablodrexler/photomanager/infrastructure/batch/CatalogAssetItemWriter.java |
| 164 | src/main/java/com/jpablodrexler/photomanager/infrastructure/kafka/KafkaProgressListener.java |
| 158 | src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller/AlbumController.java |
| 143 | src/main/java/com/jpablodrexler/photomanager/infrastructure/service/ThumbnailStorageServiceAdapter.java |
| 135 | src/main/java/com/jpablodrexler/photomanager/infrastructure/web/filter/RateLimitFilter.java |
| 130 | src/main/java/com/jpablodrexler/photomanager/infrastructure/batch/CatalogJobConfig.java |
| 123 | src/main/java/com/jpablodrexler/photomanager/application/usecase/sync/SyncAssetsUseCaseImpl.java |
| 121 | src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/CropAssetUseCaseImpl.java |
| 119 | src/main/java/com/jpablodrexler/photomanager/application/usecase/asset/MoveAssetsUseCaseImpl.java |

