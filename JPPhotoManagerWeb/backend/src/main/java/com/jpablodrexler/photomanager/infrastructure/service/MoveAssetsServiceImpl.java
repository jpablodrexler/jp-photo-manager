package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.MoveAssetsService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoveAssetsServiceImpl implements MoveAssetsService {

    private final AssetRepository assetRepository;
    private final StorageService storageService;
    private final ThumbnailStorageService thumbnailStorageService;

    @Override
    @Transactional
    public boolean moveAssets(Asset[] assets, Folder destinationFolder, boolean preserveOriginalFile) {
        for (Asset asset : assets) {
            String sourcePath = asset.getFolder().getPath() + "/" + asset.getFileName();
            String destPath = destinationFolder.getPath() + "/" + asset.getFileName();

            try {
                if (!storageService.directoryExists(destinationFolder.getPath())) {
                    storageService.createDirectory(destinationFolder.getPath());
                }

                if (preserveOriginalFile) {
                    storageService.copyFile(sourcePath, destPath);
                } else {
                    storageService.moveFile(sourcePath, destPath);
                }

                asset.setFolder(destinationFolder);
                assetRepository.save(asset);
            } catch (IOException e) {
                log.error("Failed to move asset {} to {}", sourcePath, destPath, e);
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void deleteAssets(Asset[] assets, boolean deleteFile) {
        for (Asset asset : assets) {
            if (deleteFile) {
                String filePath = asset.getFolder().getPath() + "/" + asset.getFileName();
                try {
                    storageService.deleteFile(filePath);
                } catch (IOException e) {
                    log.error("Failed to delete file {}", filePath, e);
                }
            }
            thumbnailStorageService.deleteThumbnail(asset.getThumbnailBlobName());
            assetRepository.delete(asset);
        }
    }
}
