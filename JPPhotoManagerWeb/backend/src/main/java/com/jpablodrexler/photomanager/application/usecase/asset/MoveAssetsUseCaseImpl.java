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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Override
    @Transactional
    public boolean execute(Long[] assetIds, String destinationPath, boolean preserveOriginal) {
        validateDestinationPath(destinationPath);
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        Folder destination = folderRepository.findByPath(destinationPath)
                .orElseGet(() -> folderRepository.save(Folder.builder().path(destinationPath).build()));

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
                asset.setFolder(destination);
                assetRepository.save(asset);
            } catch (IOException e) {
                log.error("Failed to move asset {} to {}", sourcePath, destFilePath, e);
                return false;
            }
        }

        saveRecentTargetPath(destinationPath);
        return true;
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
