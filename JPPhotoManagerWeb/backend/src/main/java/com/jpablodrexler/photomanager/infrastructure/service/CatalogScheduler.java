package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
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

    private final CatalogAssetsUseCase catalogAssetsUseCase;
    private final CatalogStateRepository catalogStateRepository;
    private final TaskScheduler catalogTaskScheduler;
    private final String instanceId;

    @Value("${photomanager.catalog-cooldown-minutes:2}")
    private int catalogCooldownMinutes;

    @Value("${photomanager.catalog-timeout:60}")
    private int catalogTimeoutMinutes;

    public CatalogScheduler(CatalogAssetsUseCase catalogAssetsUseCase,
                            CatalogStateRepository catalogStateRepository,
                            @Qualifier("catalogTaskScheduler") TaskScheduler catalogTaskScheduler,
                            @Qualifier("catalogInstanceId") String instanceId) {
        this.catalogAssetsUseCase = catalogAssetsUseCase;
        this.catalogStateRepository = catalogStateRepository;
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
        try {
            catalogAssetsUseCase.execute(notification -> {}).get();
        } catch (Exception e) {
            log.error("Scheduled catalog run failed", e);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupStaleCatalogs() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(catalogTimeoutMinutes));

        if (catalogStateRepository.isStaleForInstance(instanceId, threshold)) {
            log.warn("Stale catalog run detected on this instance, releasing lock");
            catalogStateRepository.release(instanceId);
        }

        int released = catalogStateRepository.releaseStaleForOtherInstances(instanceId, threshold);
        if (released > 0) {
            log.warn("Released {} stale catalog lock(s) from other instances", released);
        }
    }
}
