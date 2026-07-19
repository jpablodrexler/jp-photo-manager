package com.jpablodrexler.photomanager.application.usecase.recycle;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.recycle.PurgeAssetsUseCase;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurgeAssetsUseCaseImpl implements PurgeAssetsUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final ThumbnailPort thumbnailPort;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict("home-stats")
    public void execute(List<Long> assetIds) {
        List<Asset> targets = assetIds.isEmpty()
                ? assetRepository.findAllDeleted()
                : assetRepository.findAllById(assetIds);

        for (Asset asset : targets) {
            try {
                storagePort.deleteFile(asset.getFullPath());
            } catch (IOException e) {
                log.warn("Could not delete file for asset {}: {}", asset.getAssetId(), e.getMessage());
            }
            thumbnailPort.deleteThumbnail(asset.getThumbnailBlobName());
            assetRepository.deleteById(asset.getAssetId());
        }
    }
}
