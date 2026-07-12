package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.out.ProgressPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock JobLauncher asyncCatalogJobLauncher;
    @Mock Job catalogJob;
    @Mock ProgressPort progressPort;
    @Mock JobExecution jobExecution;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CatalogAssetsUseCaseImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogAssetsUseCaseImpl(asyncCatalogJobLauncher, catalogJob, progressPort, meterRegistry);
    }

    @Test
    void execute_startsJobAndRegistersCompletion() throws Exception {
        when(asyncCatalogJobLauncher.run(eq(catalogJob), any(JobParameters.class))).thenReturn(jobExecution);

        sut.execute(42L);

        verify(progressPort).registerCompletion(eq(42L), any(CompletableFuture.class));
        verify(asyncCatalogJobLauncher).run(eq(catalogJob), any(JobParameters.class));
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

        sut.execute(42L);

        assertThat(completionRegistered.get()).isTrue();
    }

    @Test
    void execute_jobAlreadyRunning_returnsCompletedFutureImmediately() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any()))
                .thenThrow(new JobExecutionAlreadyRunningException("already running"));

        CompletableFuture<Void> result = sut.execute(42L);

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void execute_returnsCompletableFutureRegisteredInRegistry() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any())).thenReturn(jobExecution);
        ArgumentCaptor<CompletableFuture<Void>> futureCaptor = ArgumentCaptor.forClass(CompletableFuture.class);
        doNothing().when(progressPort).registerCompletion(anyLong(), futureCaptor.capture());

        CompletableFuture<Void> result = sut.execute(42L);

        assertThat(result).isSameAs(futureCaptor.getValue());
    }

    @Test
    void incrementCatalogCounter_incrementsMetricsCounter() {
        sut.incrementCatalogCounter();

        Counter counter = meterRegistry.find("photomanager_catalog_assets_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
