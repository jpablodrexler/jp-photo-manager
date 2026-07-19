package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.application.usecase.catalog.CatalogAssetsUseCaseImpl;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogRunHistoryPort;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.infrastructure.service.KafkaProgressRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the Spring Cache behavior (@Cacheable / @CacheEvict) declared on
 * {@link GetHomeStatsUseCaseImpl} and {@link CatalogAssetsUseCaseImpl}. This requires a real
 * Spring AOP proxy backed by an actual CacheManager, so a minimal annotation-config context is
 * used instead of a plain Mockito @InjectMocks unit test.
 */
class GetHomeStatsUseCaseCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("home-stats", "sub-folders", "asset-exif");
        }

        @Bean
        FolderRepository folderRepository() {
            return mock(FolderRepository.class);
        }

        @Bean
        AssetRepository assetRepository() {
            return mock(AssetRepository.class);
        }

        @Bean
        CatalogRunHistoryPort catalogRunHistoryPort() {
            return mock(CatalogRunHistoryPort.class);
        }

        @Bean
        GetHomeStatsUseCaseImpl getHomeStatsUseCase(FolderRepository folderRepository,
                                                     AssetRepository assetRepository,
                                                     CatalogRunHistoryPort catalogRunHistoryPort) {
            return new GetHomeStatsUseCaseImpl(folderRepository, assetRepository, catalogRunHistoryPort);
        }

        @Bean
        JobLauncher asyncCatalogJobLauncher() {
            return mock(JobLauncher.class);
        }

        @Bean
        Job catalogJob() {
            return mock(Job.class);
        }

        @Bean
        KafkaProgressRegistry kafkaProgressRegistry() {
            return mock(KafkaProgressRegistry.class);
        }

        @Bean
        CatalogAssetsUseCaseImpl catalogAssetsUseCase(JobLauncher asyncCatalogJobLauncher,
                                                       Job catalogJob,
                                                       KafkaProgressRegistry kafkaProgressRegistry) {
            return new CatalogAssetsUseCaseImpl(asyncCatalogJobLauncher, catalogJob, kafkaProgressRegistry);
        }
    }

    private AnnotationConfigApplicationContext context;
    private GetHomeStatsUseCase getHomeStatsUseCase;
    private CatalogAssetsUseCase catalogAssetsUseCase;
    private AssetRepository assetRepository;
    private KafkaProgressRegistry kafkaProgressRegistry;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        getHomeStatsUseCase = context.getBean(GetHomeStatsUseCase.class);
        catalogAssetsUseCase = context.getBean(CatalogAssetsUseCase.class);

        FolderRepository folderRepository = context.getBean(FolderRepository.class);
        assetRepository = context.getBean(AssetRepository.class);
        CatalogRunHistoryPort catalogRunHistoryPort = context.getBean(CatalogRunHistoryPort.class);
        JobLauncher asyncCatalogJobLauncher = context.getBean(JobLauncher.class);
        kafkaProgressRegistry = context.getBean(KafkaProgressRegistry.class);

        when(folderRepository.count()).thenReturn(1L);
        when(assetRepository.count()).thenReturn(1L);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());
        when(catalogRunHistoryPort.findLastCompletedCatalogRunTime()).thenReturn(Optional.empty());
        when(asyncCatalogJobLauncher.run(any(), any(JobParameters.class))).thenReturn(mock(JobExecution.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void execute_calledTwice_secondCallServedFromCache() {
        getHomeStatsUseCase.execute();
        getHomeStatsUseCase.execute();

        verify(assetRepository, times(1)).count();
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_afterCatalogCompletes_cacheEvictedAndRepositoryCalledAgain() {
        getHomeStatsUseCase.execute();

        // CatalogAssetsUseCaseImpl.execute() registers its internal completion future with
        // KafkaProgressRegistry.registerCompletion(runId, completion) and returns a derived stage
        // chained onto it via Spring's async-aware @CacheEvict support (CacheAspectSupport attaches
        // a whenComplete callback to the *original* future to defer eviction until the catalog job
        // actually finishes). Production code completes that original future from
        // KafkaProgressListener when a done=true message arrives; simulate that here by capturing
        // and completing the exact future handed to the registry.
        ArgumentCaptor<CompletableFuture<Void>> completionCaptor = ArgumentCaptor.forClass(CompletableFuture.class);
        doNothing().when(kafkaProgressRegistry).registerCompletion(anyLong(), completionCaptor.capture());

        catalogAssetsUseCase.execute(1L, null);
        completionCaptor.getValue().complete(null);

        getHomeStatsUseCase.execute();

        verify(assetRepository, times(2)).count();
    }
}
