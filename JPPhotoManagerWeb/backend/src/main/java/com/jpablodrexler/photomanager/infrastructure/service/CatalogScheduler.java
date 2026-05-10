package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.repository.CatalogRunStateRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogAssetsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class CatalogScheduler {

    private final CatalogAssetsService catalogAssetsService;
    private final CatalogRunStateRepository catalogRunStateRepository;
    private final TaskScheduler catalogTaskScheduler;
    private final String instanceId;

    @Value("${photomanager.catalog-cooldown-minutes:2}")
    private int catalogCooldownMinutes;

    @Value("${photomanager.catalog-timeout:60}")
    private int catalogTimeoutMinutes;

    private volatile Thread catalogRunThread;

    public CatalogScheduler(CatalogAssetsService catalogAssetsService,
                            CatalogRunStateRepository catalogRunStateRepository,
                            @Qualifier("catalogTaskScheduler") TaskScheduler catalogTaskScheduler,
                            @Qualifier("catalogInstanceId") String instanceId) {
        this.catalogAssetsService = catalogAssetsService;
        this.catalogRunStateRepository = catalogRunStateRepository;
        this.catalogTaskScheduler = catalogTaskScheduler;
        this.instanceId = instanceId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        catalogTaskScheduler.scheduleWithFixedDelay(
                this::executeCatalogRun,
                Instant.now(),
                Duration.ofMinutes(catalogCooldownMinutes)
        );
    }

    private void executeCatalogRun() {
        catalogRunThread = Thread.currentThread();
        try {
            catalogAssetsService.runCatalog();
        } finally {
            catalogRunThread = null;
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupStaleCatalogs() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(catalogTimeoutMinutes));

        if (catalogRunStateRepository.isStaleForInstance(instanceId, threshold)) {
            log.warn("Stale catalog run detected on this instance, interrupting and releasing lock");
            Thread runThread = catalogRunThread;
            if (runThread != null) {
                runThread.interrupt();
            }
            catalogRunStateRepository.release(instanceId);
        }

        int released = catalogRunStateRepository.releaseStaleForOtherInstances(instanceId, threshold);
        if (released > 0) {
            log.warn("Released {} stale catalog lock(s) from other instances", released);
        }
    }
}
