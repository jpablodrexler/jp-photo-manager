package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.application.usecase.home.GetHomeStatsUseCaseImpl;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.folder.PruneDeletedFoldersUseCase;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogRunHistoryPort;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that a catalog job's real completion (not just its async kickoff) evicts the
 * {@code home-stats} cache.
 *
 * <p>{@link com.jpablodrexler.photomanager.application.usecase.catalog.CatalogAssetsUseCaseImpl#execute}
 * is {@code @Async} and returns its {@code CompletableFuture} immediately after launching the
 * batch job, so its own {@code @CacheEvict} fires at kickoff, not when the job actually
 * finishes. A Home dashboard read that re-populates the cache while the job is still running
 * would otherwise keep serving that stale (pre-run) snapshot for the cache's full TTL even after
 * the job genuinely completes — confirmed via {@code 02-home-dashboard.cy.ts}'s
 * {@code homePage_afterCatalogRun_lastCatalogCompletedIsNotNever} failing exactly this way.
 */
class CatalogItemWriteListenerCachingTest {

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
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        PruneDeletedFoldersUseCase pruneDeletedFoldersUseCase() {
            return mock(PruneDeletedFoldersUseCase.class);
        }

        @Bean
        GetHomeStatsUseCaseImpl getHomeStatsUseCase(FolderRepository folderRepository,
                                                     AssetRepository assetRepository,
                                                     CatalogRunHistoryPort catalogRunHistoryPort) {
            return new GetHomeStatsUseCaseImpl(folderRepository, assetRepository, catalogRunHistoryPort);
        }

        @Bean
        CatalogItemWriteListener catalogItemWriteListener(KafkaTemplate<String, Object> kafkaTemplate,
                                                            PruneDeletedFoldersUseCase pruneDeletedFoldersUseCase) {
            return new CatalogItemWriteListener(kafkaTemplate, pruneDeletedFoldersUseCase);
        }
    }

    private AnnotationConfigApplicationContext context;
    private GetHomeStatsUseCase getHomeStatsUseCase;
    private JobExecutionListener catalogItemWriteListener;
    private CatalogRunHistoryPort catalogRunHistoryPort;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        getHomeStatsUseCase = context.getBean(GetHomeStatsUseCase.class);
        // Looked up by interface, not the concrete class: @CacheEvict on afterJob() makes Spring
        // wrap this bean in a JDK dynamic proxy (it implements ItemWriteListener and
        // JobExecutionListener), which does not extend CatalogItemWriteListener itself — exactly
        // how Spring Batch's own job-builder consumes it as a plain JobExecutionListener.
        catalogItemWriteListener = context.getBean(JobExecutionListener.class);
        catalogRunHistoryPort = context.getBean(CatalogRunHistoryPort.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void afterJob_calledAfterStaleEntryWasCachedDuringTheRun_evictsHomeStatsCache() {
        when(catalogRunHistoryPort.findLastCompletedCatalogRunTime()).thenReturn(Optional.empty());

        // Simulates a Home dashboard visit that re-populates the cache *while the catalog job is
        // still running* — i.e. after CatalogAssetsUseCaseImpl.execute()'s kickoff-time
        // @CacheEvict already fired, but before the job's real completion.
        getHomeStatsUseCase.execute();
        verify(catalogRunHistoryPort, times(1)).findLastCompletedCatalogRunTime();

        when(catalogRunHistoryPort.findLastCompletedCatalogRunTime())
                .thenReturn(Optional.of(Instant.parse("2026-07-25T00:00:00Z")));
        JobParameters params = new JobParametersBuilder().addLong("runId", 1L).toJobParameters();
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("catalogJob", 1L, 1L, params);

        catalogItemWriteListener.afterJob(jobExecution);
        HomeStats stats = getHomeStatsUseCase.execute();

        // If afterJob had not evicted home-stats, this second read would be served from the
        // stale entry primed above and lastCatalogCompletedAt would still be null ("Never").
        verify(catalogRunHistoryPort, times(2)).findLastCompletedCatalogRunTime();
        assertThat(stats.lastCatalogCompletedAt()).isNotNull();
    }
}
