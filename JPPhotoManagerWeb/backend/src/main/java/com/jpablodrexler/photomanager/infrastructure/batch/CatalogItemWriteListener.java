package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.infrastructure.service.SseNotificationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogItemWriteListener implements ItemWriteListener<CatalogBatchItem>, JobExecutionListener {

    private final SseNotificationRegistry sseNotificationRegistry;

    @Override
    public void afterWrite(Chunk<? extends CatalogBatchItem> items) {
        // Per-asset notifications are sent by CatalogAssetItemWriter
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long runId = jobExecution.getJobParameters().getLong("runId");
        log.debug("Catalog job (runId={}) completed with status {}", runId, jobExecution.getStatus());
        sseNotificationRegistry.complete(runId);
        sseNotificationRegistry.remove(runId);
    }
}
