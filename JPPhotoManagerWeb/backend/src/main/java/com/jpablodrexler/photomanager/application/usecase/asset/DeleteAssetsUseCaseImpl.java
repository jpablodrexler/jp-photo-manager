package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteAssetsUseCaseImpl implements DeleteAssetsUseCase {

    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"home-stats", "asset-exif"}, allEntries = true)
    public void execute(Long[] assetIds, boolean permanently) {
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        for (Asset asset : assets) {
            if (permanently) {
                String filePath = asset.getFolder().getPath() + "/" + asset.getFileName();
                try {
                    storagePort.deleteFile(filePath);
                } catch (IOException e) {
                    log.error("Failed to delete file {}", filePath, e);
                    continue;
                }
                thumbnailPort.deleteThumbnail(asset.getThumbnailBlobName());
                assetExifRepository.deleteByAssetId(asset.getAssetId());
                assetRepository.deleteById(asset.getAssetId());
            } else {
                asset.setDeletedAt(LocalDateTime.now());
                assetRepository.save(asset);
            }
        }
    }
}
