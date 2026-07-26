package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.out.ProgressPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String JOB_NAME = "catalogJob";

    @Mock JobLauncher asyncCatalogJobLauncher;
    @Mock Job catalogJob;
    @Mock ProgressPort progressPort;
    @Mock JobExecution jobExecution;
    @Mock JobExplorer jobExplorer;
    @Mock JobRepository jobRepository;

    CatalogAssetsUseCaseImpl sut;

    @BeforeEach
    void setUp() {
        when(catalogJob.getName()).thenReturn(JOB_NAME);
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Collections.emptySet());
        sut = new CatalogAssetsUseCaseImpl(asyncCatalogJobLauncher, catalogJob, progressPort, jobExplorer, jobRepository);
        ReflectionTestUtils.setField(sut, "staleExecutionThresholdMinutes", 15L);
    }

    @Test
    void execute_startsJobAndRegistersCompletion() throws Exception {
        when(asyncCatalogJobLauncher.run(eq(catalogJob), any(JobParameters.class))).thenReturn(jobExecution);

        sut.execute(42L, USER_ID);

        verify(progressPort).registerCompletion(eq(42L), any(CompletableFuture.class));
        verify(asyncCatalogJobLauncher).run(eq(catalogJob), any(JobParameters.class));
    }

    @Test
    void execute_includesUserIdInJobParameters() throws Exception {
        ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        when(asyncCatalogJobLauncher.run(eq(catalogJob), paramsCaptor.capture())).thenReturn(jobExecution);

        sut.execute(42L, USER_ID);

        assertThat(paramsCaptor.getValue().getString("userId")).isEqualTo(USER_ID.toString());
    }

    @Test
    void execute_nullUserId_omitsUserIdJobParameter() throws Exception {
        ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        when(asyncCatalogJobLauncher.run(eq(catalogJob), paramsCaptor.capture())).thenReturn(jobExecution);

        sut.execute(42L, null);

        assertThat(paramsCaptor.getValue().getString("userId")).isNull();
    }

    @Test
    void execute_registersCompletionBeforeStartingJob() throws Exception {
        AtomicBoolean completionRegistered = new AtomicBoolean(false);
        doAnswer(inv -> { completionRegistered.set(true); return null; })
                .when(progressPort).registerCompletion(anyLong(), any());
        when(asyncCatalogJobLauncher.run(any(), any())).thenAnswer(inv -> {
            assertThat(completionRegistered.get()).isTrue();
            return jobExecution;
        });

        sut.execute(42L, USER_ID);

        assertThat(completionRegistered.get()).isTrue();
    }

    @Test
    void execute_jobAlreadyRunning_returnsCompletedFutureImmediately() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any()))
                .thenThrow(new JobExecutionAlreadyRunningException("already running"));

        CompletableFuture<Void> result = sut.execute(42L, USER_ID);

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void execute_jobAlreadyRunningAccordingToJobExplorer_skipsWithoutStartingJob() {
        JobExecution runningExecution = mock(JobExecution.class);
        when(runningExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Set.of(runningExecution));

        CompletableFuture<Void> result = sut.execute(42L, USER_ID);

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        verifyNoInteractions(asyncCatalogJobLauncher);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void execute_runningExecutionHasNoStartTimeYet_treatedAsGenuinelyRunning() {
        // JobExecution.getStartTime() is null between JobRepository.createJobExecution() and the
        // launcher actually invoking the job - unstubbed (null) must not be misread as "old enough
        // to be stale", or a job that's merely mid-launch would be wrongly abandoned.
        JobExecution runningExecution = mock(JobExecution.class);
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Set.of(runningExecution));

        CompletableFuture<Void> result = sut.execute(42L, USER_ID);

        assertThat(result.isDone()).isTrue();
        verifyNoInteractions(asyncCatalogJobLauncher);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void execute_runningExecutionIsStale_abandonsItAndStartsNewJobAnyway() throws Exception {
        // Simulates an execution orphaned by a crashed/killed process (pod restart, OOM): left in
        // STARTED forever, which would otherwise permanently block every future catalog run
        // (scheduled and manual) with no error surfaced anywhere - see execute()'s own comment.
        JobExecution staleExecution = mock(JobExecution.class);
        when(staleExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(30));
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Set.of(staleExecution));
        when(asyncCatalogJobLauncher.run(eq(catalogJob), any(JobParameters.class))).thenReturn(jobExecution);

        CompletableFuture<Void> result = sut.execute(42L, USER_ID);

        verify(staleExecution).setStatus(BatchStatus.ABANDONED);
        verify(staleExecution).setEndTime(any(LocalDateTime.class));
        verify(jobRepository).update(staleExecution);
        verify(asyncCatalogJobLauncher).run(eq(catalogJob), any(JobParameters.class));
        assertThat(result.isDone()).isFalse();
    }

    @Test
    void execute_returnsCompletableFutureRegisteredInRegistry() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any())).thenReturn(jobExecution);
        ArgumentCaptor<CompletableFuture<Void>> futureCaptor = ArgumentCaptor.forClass(CompletableFuture.class);
        doNothing().when(progressPort).registerCompletion(anyLong(), futureCaptor.capture());

        CompletableFuture<Void> result = sut.execute(42L, USER_ID);

        assertThat(result).isSameAs(futureCaptor.getValue());
    }
}
