package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ProgressPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final JobLauncher asyncCatalogJobLauncher;
    private final Job catalogJob;
    private final ProgressPort progressPort;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;

    // Documented in application.yml / docs/backend.md as "Minutes without a heartbeat before a
    // catalog run is considered stale" — previously unused by any code (confirmed: no other
    // reference to this property existed) despite already being configured; this is that
    // property's actual implementation.
    @Value("${photomanager.catalog-timeout:60}")
    private long staleExecutionThresholdMinutes;

    public CatalogAssetsUseCaseImpl(
            @Qualifier("asyncCatalogJobLauncher") JobLauncher asyncCatalogJobLauncher,
            Job catalogJob,
            ProgressPort progressPort,
            JobExplorer jobExplorer,
            JobRepository jobRepository) {
        this.asyncCatalogJobLauncher = asyncCatalogJobLauncher;
        this.catalogJob = catalogJob;
        this.progressPort = progressPort;
        this.jobExplorer = jobExplorer;
        this.jobRepository = jobRepository;
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
        if (hasGenuinelyRunningExecution(runId)) {
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

    // A JobExecution is left in STARTING/STARTED forever if the process running it dies mid-run
    // (pod restart, OOM-kill, crash) - Spring Batch has no built-in way to notice this on its own,
    // so findRunningJobExecutions() would otherwise treat that orphaned row as "still running"
    // and silently block every future catalog run (scheduled and manual alike) from ever launching
    // again, with no error surfaced anywhere. This already happened for real once (an execution
    // orphaned since a prior crash blocked all catalog runs until someone noticed and manually
    // marked it FAILED in Postgres) - age-based staleness detection recovers from this
    // automatically instead of relying on that happening again.
    private boolean hasGenuinelyRunningExecution(long runId) {
        boolean stillRunning = false;
        for (JobExecution execution : jobExplorer.findRunningJobExecutions(catalogJob.getName())) {
            if (isStale(execution)) {
                log.warn("Abandoning stale catalog job execution id={} (started={}), orphaned by "
                                + "a crashed/killed process - recovering so runId={} can launch",
                        execution.getId(), execution.getStartTime(), runId);
                execution.setStatus(BatchStatus.ABANDONED);
                execution.setEndTime(LocalDateTime.now());
                jobRepository.update(execution);
            } else {
                stillRunning = true;
            }
        }
        return stillRunning;
    }

    private boolean isStale(JobExecution execution) {
        LocalDateTime startTime = execution.getStartTime();
        if (startTime == null) {
            return false;
        }
        return Duration.between(startTime, LocalDateTime.now()).toMinutes() >= staleExecutionThresholdMinutes;
    }
}
