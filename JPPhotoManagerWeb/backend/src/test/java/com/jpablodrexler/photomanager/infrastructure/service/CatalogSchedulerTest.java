package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.repository.CatalogRunStateRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogAssetsService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSchedulerTest {

    @Mock
    CatalogAssetsService catalogAssetsService;

    @Mock
    CatalogRunStateRepository catalogRunStateRepository;

    @Mock
    TaskScheduler catalogTaskScheduler;

    CatalogScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogScheduler(catalogAssetsService, catalogRunStateRepository, catalogTaskScheduler, "test-instance");
        ReflectionTestUtils.setField(sut, "catalogCooldownMinutes", 2);
        ReflectionTestUtils.setField(sut, "catalogTimeoutMinutes", 60);
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
    void cleanupStaleCatalogs_ownStaleLock_interruptsThreadAndReleasesLock() throws InterruptedException {
        when(catalogRunStateRepository.isStaleForInstance(eq("test-instance"), any())).thenReturn(true);
        when(catalogRunStateRepository.releaseStaleForOtherInstances(eq("test-instance"), any())).thenReturn(0);

        CountDownLatch interruptReceived = new CountDownLatch(1);
        Thread runThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                interruptReceived.countDown();
            }
        });
        runThread.start();
        Thread.sleep(50);
        ReflectionTestUtils.setField(sut, "catalogRunThread", runThread);

        sut.cleanupStaleCatalogs();

        assertThat(interruptReceived.await(500, TimeUnit.MILLISECONDS)).isTrue();
        verify(catalogRunStateRepository).release("test-instance");
        runThread.join(500);
    }

    @Test
    void cleanupStaleCatalogs_remoteStaleLock_releasesWithoutInterrupt() {
        when(catalogRunStateRepository.isStaleForInstance(eq("test-instance"), any())).thenReturn(false);
        when(catalogRunStateRepository.releaseStaleForOtherInstances(eq("test-instance"), any())).thenReturn(1);

        sut.cleanupStaleCatalogs();

        verify(catalogRunStateRepository, never()).release(any());
        verify(catalogRunStateRepository).releaseStaleForOtherInstances(eq("test-instance"), any());
    }

    @Test
    void cleanupStaleCatalogs_noStaleLock_doesNothing() {
        when(catalogRunStateRepository.isStaleForInstance(eq("test-instance"), any())).thenReturn(false);
        when(catalogRunStateRepository.releaseStaleForOtherInstances(eq("test-instance"), any())).thenReturn(0);

        sut.cleanupStaleCatalogs();

        verify(catalogRunStateRepository, never()).release(any());
    }
}
