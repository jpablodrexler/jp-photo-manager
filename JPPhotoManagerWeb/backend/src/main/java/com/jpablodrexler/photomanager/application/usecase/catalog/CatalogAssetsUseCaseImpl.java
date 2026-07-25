package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ProgressPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final JobLauncher asyncCatalogJobLauncher;
    private final Job catalogJob;
    private final ProgressPort progressPort;
    private final JobExplorer jobExplorer;

    public CatalogAssetsUseCaseImpl(
            @Qualifier("asyncCatalogJobLauncher") JobLauncher asyncCatalogJobLauncher,
            Job catalogJob,
            ProgressPort progressPort,
            JobExplorer jobExplorer) {
        this.asyncCatalogJobLauncher = asyncCatalogJobLauncher;
        this.catalogJob = catalogJob;
        this.progressPort = progressPort;
        this.jobExplorer = jobExplorer;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"home-stats", "sub-folders", "asset-exif"}, allEntries = true)
    public CompletableFuture<Void> execute(long runId, UUID userId) {
        // JobParameters (including runId) are unique per invocation so each catalog run gets its
        // own JobInstance and can be relaunched after completing — that means Spring Batch's own
        // JobExecutionAlreadyRunningException guard, which only fires on *identical*
        // JobParameters, can never trigger here. findRunningJobExecutions instead queries actual
        // execution status in the shared JobRepository (Postgres, spanning every replica), so it
        // reflects whether the job is really running regardless of JobParameters.
        if (!jobExplorer.findRunningJobExecutions(catalogJob.getName()).isEmpty()) {
            log.debug("Catalog already running, skipping runId={}", runId);
            return CompletableFuture.completedFuture(null);
        }
        try {
            CompletableFuture<Void> completion = new CompletableFuture<>();
            progressPort.registerCompletion(runId, completion);

            JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                    .addLong("runId", runId);
            if (userId != null) {
                paramsBuilder.addString("userId", userId.toString());
            }
            JobParameters params = paramsBuilder.toJobParameters();

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
}
