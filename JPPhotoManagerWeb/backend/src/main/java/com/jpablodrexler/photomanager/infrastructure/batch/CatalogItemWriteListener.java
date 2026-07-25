package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.domain.port.in.folder.PruneDeletedFoldersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogItemWriteListener implements ItemWriteListener<CatalogBatchItem>, JobExecutionListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PruneDeletedFoldersUseCase pruneDeletedFoldersUseCase;

    @Override
    public void afterWrite(Chunk<? extends CatalogBatchItem> items) {
        // Per-asset notifications are sent by CatalogAssetItemWriter
    }

    // CatalogAssetsUseCaseImpl.execute() evicts these same caches too, but only at job *kickoff*
    // (it's @Async and returns its CompletableFuture immediately, so Spring's @CacheEvict fires
    // right away, not when the batch job actually finishes). Anything that reads home-stats
    // while the job is still running re-populates the cache with pre-run data, which then serves
    // stale results (e.g. "last catalog completed: Never") for up to the cache's full TTL even
    // after the job genuinely completes — confirmed via 02-home-dashboard.cy.ts's
    // homePage_afterCatalogRun_lastCatalogCompletedIsNotNever failing this way. Evict again here,
    // at real completion, so a stat card read shortly after a run finishes gets fresh data.
    @Override
    @CacheEvict(value = {"home-stats", "sub-folders", "asset-exif"}, allEntries = true)
    public void afterJob(JobExecution jobExecution) {
        long runId = jobExecution.getJobParameters().getLong("runId");
        String userIdParam = jobExecution.getJobParameters().getString("userId");
        UUID userId = userIdParam != null ? UUID.fromString(userIdParam) : null;
        log.debug("Catalog job (runId={}) completed with status {}", runId, jobExecution.getStatus());
        try {
            pruneDeletedFoldersUseCase.execute(null);
        } catch (Exception e) {
            log.error("Error pruning deleted folders after catalog job (runId={})", runId, e);
        }

        int foldersScanned = 0;
        long assetsAdded = 0;
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStepName().startsWith("catalogWorkerStep")) {
                foldersScanned++;
                assetsAdded += stepExecution.getWriteCount();
            }
        }
        long durationMs = (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null)
                ? Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                : 0L;

        try {
            kafkaTemplate.send("job.catalog.progress", String.valueOf(runId),
                    CatalogProgressMessage.done(runId, foldersScanned, assetsAdded, durationMs, userId));
        } catch (Exception e) {
            log.warn("Failed to publish job.catalog.progress done message (runId={}): {}", runId, e.getMessage());
        }
    }
}
