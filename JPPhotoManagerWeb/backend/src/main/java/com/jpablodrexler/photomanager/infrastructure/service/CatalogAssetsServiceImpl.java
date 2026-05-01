package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.repository.CatalogRunStateRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogAssetsService;
import com.jpablodrexler.photomanager.domain.service.CatalogFolderService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
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
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    private final CatalogFolderService catalogFolderService;
    private final StorageService storageService;
    private final CatalogRunStateRepository catalogRunStateRepository;
    private final String instanceId;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    public CatalogAssetsServiceImpl(CatalogFolderService catalogFolderService,
            StorageService storageService,
            CatalogRunStateRepository catalogRunStateRepository,
            @Qualifier("catalogInstanceId") String instanceId) {
        this.catalogFolderService = catalogFolderService;
        this.storageService = storageService;
        this.catalogRunStateRepository = catalogRunStateRepository;
        this.instanceId = instanceId;
    }

    @Override
    public void runCatalog() {
        if (catalogRunStateRepository.tryAcquire(instanceId, Instant.now()) == 0) {
            log.debug("Catalog already running, skipping scheduled run");
            return;
        }
        try {
            doRunCatalog(n -> {
            });
        } catch (Exception e) {
            log.error("Error during catalog run", e);
        } finally {
            catalogRunStateRepository.release(instanceId);
        }
    }

    @Async
    @Override
    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        if (catalogRunStateRepository.tryAcquire(instanceId, Instant.now()) == 0) {
            log.debug("Catalog already running, skipping API-triggered run");
            return CompletableFuture.completedFuture(null);
        }
        try {
            doRunCatalog(callback);
        } finally {
            catalogRunStateRepository.release(instanceId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Asset createAsset(String directoryPath, String fileName) {
        return catalogFolderService.createAsset(directoryPath, fileName);
    }

    private void doRunCatalog(Consumer<CatalogChangeNotification> callback) {
        List<String> rootFolders = Arrays.asList(rootCatalogFolders.split(";"));
        List<String> allFolders = new ArrayList<>();

        for (String root : rootFolders) {
            if (storageService.directoryExists(root)) {
                allFolders.add(root);
                collectSubFolders(root, allFolders);
            }
        }

        AtomicInteger processed = new AtomicInteger(0);
        int total = allFolders.size();

        for (String folderPath : allFolders) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Catalog interrupted, stopping at folder: {}", folderPath);
                return;
            }
            try {
                Runnable heartbeat = () -> catalogRunStateRepository.refreshHeartbeat(instanceId, Instant.now());
                catalogFolderService.catalogFolder(folderPath, callback, heartbeat, processed, total);
            } catch (Exception e) {
                log.error("Error cataloging folder: {}", folderPath, e);
            }
        }
    }

    private void collectSubFolders(String parentPath, List<String> result) {
        List<String> subs = storageService.listSubDirectories(parentPath);
        for (String sub : subs) {
            result.add(sub);
            collectSubFolders(sub, result);
        }
    }
}
