package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.repository.FolderRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogAssetsService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogAssetsServiceImpl implements CatalogAssetsService {

    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 150;

    private final AssetRepository assetRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final ThumbnailStorageService thumbnailStorageService;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Value("${photomanager.catalog-batch-size:1000}")
    private int catalogBatchSize;

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
                catalogFolder(folderPath, callback, processed, total);
            } catch (Exception e) {
                log.error("Error cataloging folder: {}", folderPath, e);
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional
    public Asset createAsset(String directoryPath, String fileName) {
        Folder folder = folderRepository.findByPath(directoryPath).orElseGet(() -> {
            Folder f = new Folder();
            f.setPath(directoryPath);
            return folderRepository.save(f);
        });

        String filePath = directoryPath + "/" + fileName;
        try {
            Asset asset = new Asset();
            asset.setFolder(folder);
            asset.setFileName(fileName);
            asset.setFileSize(storageService.getFileSize(filePath));
            asset.setHash(storageService.computeHash(filePath));
            asset.setFileCreationDateTime(storageService.getFileCreationDateTime(filePath));
            asset.setFileModificationDateTime(storageService.getFileModificationDateTime(filePath));
            asset.setThumbnailCreationDateTime(LocalDateTime.now());

            var rotation = storageService.getImageRotation(filePath);
            asset.setImageRotation(rotation);

            byte[] thumbnail = storageService.generateThumbnail(filePath, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
            asset = assetRepository.save(asset);
            thumbnailStorageService.saveThumbnail(asset.getThumbnailBlobName(), thumbnail);

            return asset;
        } catch (IOException e) {
            log.error("Failed to create asset for {}", filePath, e);
            throw new RuntimeException("Failed to create asset", e);
        }
    }

    @Transactional
    protected void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                                  AtomicInteger processed, int total) {
        Folder folder = folderRepository.findByPath(folderPath).orElseGet(() -> {
            Folder f = new Folder();
            f.setPath(folderPath);
            Folder saved = folderRepository.save(f);
            if (callback != null) {
                callback.accept(new CatalogChangeNotification(ReasonEnum.FOLDER_CREATED, folderPath,
                        computePercent(processed.get(), total)));
            }
            return saved;
        });

        List<String> filesOnDisk = storageService.listFiles(folderPath);
        Set<String> fileNamesOnDisk = new HashSet<>();
        for (String filePath : filesOnDisk) {
            fileNamesOnDisk.add(Paths.get(filePath).getFileName().toString());
        }

        List<Asset> cataloguedAssets = assetRepository.findByFolder(folder);
        Set<String> cataloguedFileNames = new HashSet<>();
        for (Asset asset : cataloguedAssets) {
            cataloguedFileNames.add(asset.getFileName());
        }

        // Add new assets
        for (String filePath : filesOnDisk) {
            String fileName = Paths.get(filePath).getFileName().toString();
            if (!cataloguedFileNames.contains(fileName)) {
                try {
                    Asset asset = createAsset(folderPath, fileName);
                    if (callback != null) {
                        callback.accept(new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, asset,
                                computePercent(processed.get(), total)));
                    }
                } catch (Exception e) {
                    log.error("Failed to catalog asset: {}", filePath, e);
                }
            }
        }

        // Remove deleted assets
        for (Asset asset : cataloguedAssets) {
            if (!fileNamesOnDisk.contains(asset.getFileName())) {
                assetRepository.delete(asset);
                thumbnailStorageService.deleteThumbnail(asset.getThumbnailBlobName());
                if (callback != null) {
                    callback.accept(new CatalogChangeNotification(ReasonEnum.ASSET_DELETED, asset,
                            computePercent(processed.get(), total)));
                }
            }
        }

        processed.incrementAndGet();
    }

    private void collectSubFolders(String parentPath, List<String> result) {
        List<String> subs = storageService.listSubDirectories(parentPath);
        for (String sub : subs) {
            result.add(sub);
            collectSubFolders(sub, result);
        }
    }

    private int computePercent(int processed, int total) {
        if (total == 0) return 100;
        return (int) ((double) processed / total * 100);
    }
}
