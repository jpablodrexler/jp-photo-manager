package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ProgressPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final JobLauncher asyncCatalogJobLauncher;
    private final Job catalogJob;
    private final ProgressPort progressPort;
    private final Counter catalogAssetsCounter;

    public CatalogAssetsUseCaseImpl(
            @Qualifier("asyncCatalogJobLauncher") JobLauncher asyncCatalogJobLauncher,
            Job catalogJob,
            ProgressPort progressPort,
            MeterRegistry meterRegistry) {
        this.asyncCatalogJobLauncher = asyncCatalogJobLauncher;
        this.catalogJob = catalogJob;
        this.progressPort = progressPort;
        this.catalogAssetsCounter = Counter.builder("photomanager_catalog_assets_total")
                .description("Total assets cataloged")
                .register(meterRegistry);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"home-stats", "sub-folders", "asset-exif"}, allEntries = true)
    public CompletableFuture<Void> execute(long runId) {
        try {
            CompletableFuture<Void> completion = new CompletableFuture<>();
            progressPort.registerCompletion(runId, completion);

            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", runId)
                    .toJobParameters();

            JobExecution execution = asyncCatalogJobLauncher.run(catalogJob, params);
            log.debug("Started catalog job execution id={} runId={}", execution.getId(), runId);

            return completion;
        } catch (JobExecutionAlreadyRunningException e) {
            log.debug("Catalog already running, skipping");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to start catalog job", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void incrementCatalogCounter() {
        catalogAssetsCounter.increment();
    }
}
