package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogRunHistoryServiceAdapterTest {

    @Mock JobExplorer jobExplorer;
    @InjectMocks CatalogRunHistoryServiceAdapter sut;

    @Test
    void findLastCompletedCatalogRunTime_noInstances_returnsEmpty() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());

        assertThat(sut.findLastCompletedCatalogRunTime()).isEmpty();
    }

    @Test
    void findLastCompletedCatalogRunTime_noCompletedExecutions_returnsEmpty() {
        JobInstance instance = new JobInstance(1L, "catalogJob");
        JobExecution running = new JobExecution(1L, new JobParameters());
        running.setStatus(BatchStatus.STARTED);
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(running));

        assertThat(sut.findLastCompletedCatalogRunTime()).isEmpty();
    }

    @Test
    void findLastCompletedCatalogRunTime_completedWithoutEndTime_returnsEmpty() {
        JobInstance instance = new JobInstance(1L, "catalogJob");
        JobExecution completedNoEnd = new JobExecution(1L, new JobParameters());
        completedNoEnd.setStatus(BatchStatus.COMPLETED);
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(completedNoEnd));

        assertThat(sut.findLastCompletedCatalogRunTime()).isEmpty();
    }

    @Test
    void findLastCompletedCatalogRunTime_multipleCompleted_returnsMostRecentEndTime() {
        JobInstance instance = new JobInstance(1L, "catalogJob");
        LocalDateTime olderEnd = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime newerEnd = LocalDateTime.of(2026, 1, 2, 10, 0);

        JobExecution older = new JobExecution(1L, new JobParameters());
        older.setStatus(BatchStatus.COMPLETED);
        older.setEndTime(olderEnd);

        JobExecution newer = new JobExecution(2L, new JobParameters());
        newer.setStatus(BatchStatus.COMPLETED);
        newer.setEndTime(newerEnd);

        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(older, newer));

        Optional<Instant> result = sut.findLastCompletedCatalogRunTime();

        assertThat(result).contains(newerEnd.toInstant(ZoneOffset.UTC));
    }
}
