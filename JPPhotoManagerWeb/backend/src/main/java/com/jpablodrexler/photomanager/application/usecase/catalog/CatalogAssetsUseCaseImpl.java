package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@Slf4j
public class CatalogAssetsUseCaseImpl implements CatalogAssetsUseCase {

    private final CatalogFolderPort catalogFolderService;
    private final StoragePort storagePort;
    private final CatalogStateRepository catalogStateRepository;
    private final String instanceId;
    private final Counter catalogAssetsCounter;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    public CatalogAssetsUseCaseImpl(CatalogFolderPort catalogFolderService,
            StoragePort storagePort,
            CatalogStateRepository catalogStateRepository,
            @Qualifier("catalogInstanceId") String instanceId,
            MeterRegistry meterRegistry) {
        this.catalogFolderService = catalogFolderService;
        this.storagePort = storagePort;
        this.catalogStateRepository = catalogStateRepository;
        this.instanceId = instanceId;
        this.catalogAssetsCounter = Counter.builder("photomanager_catalog_assets_total")
                .description("Total assets cataloged")
                .register(meterRegistry);
    }

    @Async
    @Override
    public CompletableFuture<Void> execute(Consumer<CatalogChangeNotification> listener) {
        if (!catalogStateRepository.tryAcquire(instanceId, Instant.now())) {
            log.debug("Catalog already running, skipping API-triggered run");
            return CompletableFuture.completedFuture(null);
        }
        Consumer<CatalogChangeNotification> countingListener = notification -> {
            if (notification.getReason() == ReasonEnum.ASSET_CREATED) {
                catalogAssetsCounter.increment();
            }
            if (listener != null) {
                listener.accept(notification);
            }
        };
        try {
            doRunCatalog(countingListener);
            catalogStateRepository.markCompleted(instanceId, Instant.now());
        } finally {
            catalogStateRepository.release(instanceId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void doRunCatalog(Consumer<CatalogChangeNotification> callback) {
        List<String> rootFolders = Arrays.asList(rootCatalogFolders.split(";"));
        List<String> allFolders = new ArrayList<>();
        for (String root : rootFolders) {
            if (storagePort.directoryExists(root)) {
                allFolders.add(root);
                collectSubFolders(root, allFolders);
            }
        }

        AtomicInteger processed = new AtomicInteger(0);
        int total = allFolders.size();

        for (String folderPath : allFolders) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Catalog interrupted at folder: {}", folderPath);
                return;
            }
            try {
                Runnable heartbeat = () -> catalogStateRepository.refreshHeartbeat(instanceId, Instant.now());
                catalogFolderService.catalogFolder(folderPath, callback, heartbeat, processed, total);
            } catch (Exception e) {
                log.error("Error cataloging folder: {}", folderPath, e);
            }
        }
    }

    private void collectSubFolders(String parentPath, List<String> result) {
        List<String> subs = storagePort.listSubDirectories(parentPath);
        for (String sub : subs) {
            result.add(sub);
            collectSubFolders(sub, result);
        }
    }
}
