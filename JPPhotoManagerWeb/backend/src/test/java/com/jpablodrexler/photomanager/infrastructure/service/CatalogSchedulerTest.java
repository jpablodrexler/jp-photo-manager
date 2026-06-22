package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSchedulerTest {

    @Mock
    CatalogAssetsUseCase catalogAssetsUseCase;

    @Mock
    TaskScheduler catalogTaskScheduler;

    CatalogScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogScheduler(catalogAssetsUseCase, catalogTaskScheduler);
        ReflectionTestUtils.setField(sut, "catalogCooldownMinutes", 2);
    }

    @Test
    void onApplicationReady_schedulesImmediateRunWithConfiguredDelay() {
        sut.onApplicationReady();

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Duration> delayCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(catalogTaskScheduler).scheduleWithFixedDelay(any(Runnable.class), startCaptor.capture(), delayCaptor.capture());

        assertThat(startCaptor.getValue()).isBeforeOrEqualTo(Instant.now());
        assertThat(delayCaptor.getValue()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void onApplicationReady_scheduledRunCallsUseCase() throws Exception {
        when(catalogAssetsUseCase.execute(anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        sut.onApplicationReady();

        verify(catalogTaskScheduler).scheduleWithFixedDelay(runnableCaptor.capture(), any(Instant.class), any(Duration.class));
        runnableCaptor.getValue().run();

        verify(catalogAssetsUseCase).execute(anyLong());
    }
}
