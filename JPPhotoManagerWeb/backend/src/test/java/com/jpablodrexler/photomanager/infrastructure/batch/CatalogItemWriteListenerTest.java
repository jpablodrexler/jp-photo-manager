package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.port.in.folder.PruneDeletedFoldersUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogItemWriteListenerTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock PruneDeletedFoldersUseCase pruneDeletedFoldersUseCase;
    @InjectMocks CatalogItemWriteListener sut;

    @Test
    void afterWrite_doesNothing() {
        sut.afterWrite(null);
        // Per-asset notifications are sent by CatalogAssetItemWriter; this listener is a no-op here.
    }

    @Test
    void afterJob_completed_prunesDeletedFoldersAndPublishesDoneMessage() {
        JobParameters params = new JobParametersBuilder()
                .addLong("runId", 1L)
                .addString("userId", UUID.randomUUID().toString())
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(1L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        jobExecution.setEndTime(LocalDateTime.of(2026, 1, 1, 10, 5));

        StepExecution worker = jobExecution.createStepExecution("catalogWorkerStep0");
        worker.setWriteCount(5);

        sut.afterJob(jobExecution);

        verify(pruneDeletedFoldersUseCase).execute(null);
        verify(kafkaTemplate).send(eq("job.catalog.progress"), eq("1"), any());
    }

    @Test
    void afterJob_pruneThrows_stillPublishesDoneMessage() {
        JobParameters params = new JobParametersBuilder().addLong("runId", 2L).toJobParameters();
        JobExecution jobExecution = new JobExecution(2L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        doThrow(new RuntimeException("prune failed")).when(pruneDeletedFoldersUseCase).execute(null);

        sut.afterJob(jobExecution);

        verify(kafkaTemplate).send(eq("job.catalog.progress"), eq("2"), any());
    }

    @Test
    void afterJob_kafkaSendThrows_doesNotPropagate() {
        JobParameters params = new JobParametersBuilder().addLong("runId", 3L).toJobParameters();
        JobExecution jobExecution = new JobExecution(3L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), any());

        sut.afterJob(jobExecution);

        verify(pruneDeletedFoldersUseCase).execute(null);
    }

    @Test
    void afterJob_noStartOrEndTime_publishesZeroDuration() {
        JobParameters params = new JobParametersBuilder().addLong("runId", 4L).toJobParameters();
        JobExecution jobExecution = new JobExecution(4L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        sut.afterJob(jobExecution);

        verify(kafkaTemplate, times(1)).send(eq("job.catalog.progress"), eq("4"), any());
    }

    @Test
    void afterJob_noUserId_usesNullUser() {
        JobParameters params = new JobParametersBuilder().addLong("runId", 5L).toJobParameters();
        JobExecution jobExecution = new JobExecution(5L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);

        sut.afterJob(jobExecution);

        verify(pruneDeletedFoldersUseCase).execute(null);
        verify(kafkaTemplate).send(eq("job.catalog.progress"), eq("5"), any());
        verify(kafkaTemplate, never()).send(eq("asset.cataloged"), anyString(), any());
    }
}
