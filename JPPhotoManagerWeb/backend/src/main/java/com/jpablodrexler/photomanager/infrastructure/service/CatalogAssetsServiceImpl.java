package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.service.CatalogAssetsService;
import com.jpablodrexler.photomanager.domain.service.CatalogFolderService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    private final CatalogFolderService catalogFolderService;
    private final StorageService storageService;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Async
    @Override
    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
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
            try {
                catalogFolderService.catalogFolder(folderPath, callback, processed, total);
            } catch (Exception e) {
                log.error("Error cataloging folder: {}", folderPath, e);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Asset createAsset(String directoryPath, String fileName) {
        return catalogFolderService.createAsset(directoryPath, fileName);
    }

    private void collectSubFolders(String parentPath, List<String> result) {
        List<String> subs = storageService.listSubDirectories(parentPath);
        for (String sub : subs) {
            result.add(sub);
            collectSubFolders(sub, result);
        }
    }
}
