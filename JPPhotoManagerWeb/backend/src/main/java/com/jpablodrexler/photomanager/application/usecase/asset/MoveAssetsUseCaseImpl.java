package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.domain.port.in.asset.MoveAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.RecentTargetPathRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoveAssetsUseCaseImpl implements MoveAssetsUseCase {

    private static final int MAX_RECENT_PATHS = 20;

    private final AssetRepository assetRepository;
    private final FolderRepository folderRepository;
    private final StoragePort storagePort;
    private final RecentTargetPathRepository recentTargetPathRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"home-stats", "sub-folders"}, allEntries = true)
    public boolean execute(Long[] assetIds, String destinationPath, boolean preserveOriginal) {
        validateDestinationPath(destinationPath);
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        Folder destination = folderRepository.findByPath(destinationPath)
                .orElseGet(() -> folderRepository.save(Folder.builder().path(destinationPath).build()));

        TransactionTemplate perAssetTransaction = new TransactionTemplate(transactionManager);
        perAssetTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Asset asset : assets) {
            String sourcePath = asset.getFolder().getPath() + "/" + asset.getFileName();
            String destFilePath = destination.getPath() + "/" + asset.getFileName();
            try {
                if (!storagePort.directoryExists(destination.getPath())) {
                    storagePort.createDirectory(destination.getPath());
                }
                if (preserveOriginal) {
                    storagePort.copyFile(sourcePath, destFilePath);
                } else {
                    storagePort.moveFile(sourcePath, destFilePath);
                }
            } catch (IOException e) {
                log.error("Failed to move asset {} to {}", sourcePath, destFilePath, e);
                return false;
            }

            try {
                perAssetTransaction.executeWithoutResult(status -> {
                    asset.setFolder(destination);
                    assetRepository.save(asset);
                });
            } catch (RuntimeException e) {
                log.error("Failed to persist move for asset {}, reverting file on disk", asset.getAssetId(), e);
                revertFileOperation(preserveOriginal, sourcePath, destFilePath, asset.getAssetId());
                return false;
            }
        }

        saveRecentTargetPath(destinationPath);
        return true;
    }

    private void revertFileOperation(boolean preserveOriginal, String sourcePath, String destFilePath, Long assetId) {
        try {
            if (preserveOriginal) {
                storagePort.deleteFile(destFilePath);
            } else {
                storagePort.moveFile(destFilePath, sourcePath);
            }
        } catch (IOException undoEx) {
            log.error("Failed to revert file operation for asset {} after DB save failure", assetId, undoEx);
        }
    }

    private void validateDestinationPath(String destinationFolderPath) {
        var destination = Paths.get(destinationFolderPath).normalize().toAbsolutePath();
        boolean withinRoot = Arrays.stream(rootCatalogFolders.split(";"))
                .map(root -> Paths.get(root.trim()).normalize().toAbsolutePath())
                .anyMatch(destination::startsWith);
        if (!withinRoot) {
            throw new IllegalArgumentException("Destination path is outside the allowed catalog roots.");
        }
    }

    private void saveRecentTargetPath(String path) {
        if (!recentTargetPathRepository.existsByPath(path)) {
            recentTargetPathRepository.save(RecentTargetPath.builder().path(path).build());
            List<RecentTargetPath> all = recentTargetPathRepository.findAllOrderByIdDesc();
            if (all.size() > MAX_RECENT_PATHS) {
                recentTargetPathRepository.deleteAll(all.subList(MAX_RECENT_PATHS, all.size()));
            }
        }
    }
}
