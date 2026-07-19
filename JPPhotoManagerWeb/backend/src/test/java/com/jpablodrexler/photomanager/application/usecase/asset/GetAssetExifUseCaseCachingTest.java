package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that permanently purging an asset evicts its cached EXIF entry.
 *
 * <p>{@code @Cacheable} skips the target method body entirely on a cache hit, including the
 * {@code assetRepository.findById(assetId)} existence check inside
 * {@link GetAssetExifUseCaseImpl}. Without evicting the {@code asset-exif} cache on purge, a
 * caller could keep retrieving an asset's (potentially sensitive, e.g. GPS-derived) EXIF metadata
 * for up to the cache's 30-minute TTL after the asset — and its EXIF document — were deleted.
 */
class GetAssetExifUseCaseCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("asset-exif", "home-stats");
        }

        @Bean
        AssetRepository assetRepository() {
            return mock(AssetRepository.class);
        }

        @Bean
        AssetExifRepository assetExifRepository() {
            return mock(AssetExifRepository.class);
        }

        @Bean
        StoragePort storagePort() {
            return mock(StoragePort.class);
        }

        @Bean
        ThumbnailPort thumbnailPort() {
            return mock(ThumbnailPort.class);
        }

        @Bean
        GetAssetExifUseCaseImpl getAssetExifUseCase(AssetRepository assetRepository,
                                                     AssetExifRepository assetExifRepository) {
            return new GetAssetExifUseCaseImpl(assetRepository, assetExifRepository);
        }

        @Bean
        DeleteAssetsUseCaseImpl deleteAssetsUseCase(AssetRepository assetRepository,
                                                     StoragePort storagePort,
                                                     ThumbnailPort thumbnailPort) {
            return new DeleteAssetsUseCaseImpl(assetRepository, storagePort, thumbnailPort);
        }
    }

    private AnnotationConfigApplicationContext context;
    private GetAssetExifUseCase getAssetExifUseCase;
    private DeleteAssetsUseCase deleteAssetsUseCase;
    private AssetRepository assetRepository;
    private AssetExifRepository assetExifRepository;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        getAssetExifUseCase = context.getBean(GetAssetExifUseCase.class);
        deleteAssetsUseCase = context.getBean(DeleteAssetsUseCase.class);
        assetRepository = context.getBean(AssetRepository.class);
        assetExifRepository = context.getBean(AssetExifRepository.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void execute_afterPurge_cacheEvictedAndExifNotServedFromStaleCache() {
        Long assetId = 42L;
        Folder folder = Folder.builder().path("/photos").build();
        Asset asset = Asset.builder().assetId(assetId).fileName("photo.jpg").folder(folder).build();
        AssetExif exif = AssetExif.builder().assetId(assetId).cameraMake("TestCamera").build();
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetExifRepository.findByAssetId(assetId)).thenReturn(Optional.of(exif));

        // Prime the cache.
        getAssetExifUseCase.execute(assetId);
        verify(assetExifRepository, times(1)).findByAssetId(assetId);

        // Purge the asset — this must evict the cached EXIF entry, not just "home-stats".
        when(assetRepository.findAllById(List.of(assetId))).thenReturn(List.of(asset));
        deleteAssetsUseCase.execute(new Long[]{assetId}, true);

        getAssetExifUseCase.execute(assetId);

        // If the "asset-exif" cache had not been evicted by the purge, this second call would be
        // served from the stale cache entry and assetExifRepository would still show only 1 call.
        verify(assetExifRepository, times(2)).findByAssetId(assetId);
    }
}
