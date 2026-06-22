package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class CatalogScheduler {

    private final CatalogAssetsUseCase catalogAssetsUseCase;
    private final TaskScheduler catalogTaskScheduler;

    @Value("${photomanager.catalog-cooldown-minutes:2}")
    private int catalogCooldownMinutes;

    public CatalogScheduler(CatalogAssetsUseCase catalogAssetsUseCase,
                            @Qualifier("catalogTaskScheduler") TaskScheduler catalogTaskScheduler) {
        this.catalogAssetsUseCase = catalogAssetsUseCase;
        this.catalogTaskScheduler = catalogTaskScheduler;
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
            catalogAssetsUseCase.execute(System.currentTimeMillis()).get();
        } catch (Exception e) {
            log.error("Scheduled catalog run failed", e);
        }
    }
}
