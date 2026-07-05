package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.domain.port.in.catalog.PruneDeletedFoldersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

    @Override
    public void afterJob(JobExecution jobExecution) {
        long runId = jobExecution.getJobParameters().getLong("runId");
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

        kafkaTemplate.send("job.catalog.progress", String.valueOf(runId),
                CatalogProgressMessage.done(runId, foldersScanned, assetsAdded, durationMs));
    }
}
