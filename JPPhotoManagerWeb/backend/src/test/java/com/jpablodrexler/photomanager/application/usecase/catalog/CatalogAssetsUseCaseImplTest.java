package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.service.SseNotificationRegistry;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock JobLauncher asyncCatalogJobLauncher;
    @Mock Job catalogJob;
    @Mock SseNotificationRegistry sseNotificationRegistry;
    @Mock JobExecution jobExecution;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CatalogAssetsUseCaseImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogAssetsUseCaseImpl(asyncCatalogJobLauncher, catalogJob, sseNotificationRegistry, meterRegistry);
    }

    @Test
    void execute_startsJobAndRegistersConsumer() throws Exception {
        when(asyncCatalogJobLauncher.run(eq(catalogJob), any(JobParameters.class))).thenReturn(jobExecution);

        sut.execute(notification -> {});

        verify(sseNotificationRegistry).register(any(Long.class), any(), any());
        verify(asyncCatalogJobLauncher).run(eq(catalogJob), any(JobParameters.class));
    }

    @Test
    void execute_registersConsumerBeforeStartingJob() throws Exception {
        AtomicReference<Long> registeredRunId = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(inv -> {
            registeredRunId.set(inv.getArgument(0));
            return null;
        }).when(sseNotificationRegistry).register(any(Long.class), any(), any());
        when(asyncCatalogJobLauncher.run(any(), any())).thenReturn(jobExecution);

        sut.execute(null);

        assertThat(registeredRunId.get()).isNotNull();
    }

    @Test
    void execute_jobAlreadyRunning_returnsCompletedFutureImmediately() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any()))
                .thenThrow(new JobExecutionAlreadyRunningException("already running"));

        CompletableFuture<Void> result = sut.execute(notification -> {});

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void execute_assetCreatedNotification_incrementsCounter() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any())).thenReturn(jobExecution);

        ArgumentCaptor<Consumer<CatalogChangeNotification>> consumerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        org.mockito.Mockito.doAnswer(inv -> null)
                .when(sseNotificationRegistry).register(any(Long.class), consumerCaptor.capture(), any());

        sut.execute(notification -> {});

        Asset asset = new Asset();
        consumerCaptor.getValue().accept(new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, asset, 50));

        Counter counter = meterRegistry.find("photomanager_catalog_assets_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void execute_nullListener_doesNotRegisterConsumer() throws Exception {
        when(asyncCatalogJobLauncher.run(any(), any())).thenReturn(jobExecution);

        sut.execute(null);

        verify(sseNotificationRegistry).register(any(Long.class), eq(null), any());
    }
}
