package com.jpablodrexler.photomanager.application.usecase.recycle;

import com.jpablodrexler.photomanager.domain.port.in.recycle.RestoreAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestoreAssetsUseCaseImpl implements RestoreAssetsUseCase {

    private final AssetRepository assetRepository;
    private final AssetSearchCachePort assetSearchCachePort;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict("home-stats")
    public void execute(List<Long> assetIds) {
        var assets = assetRepository.findAllById(assetIds);
        for (var asset : assets) {
            asset.setDeletedAt(null);
            assetRepository.save(asset);

            // Same gap as soft/hard delete (see DeleteAssetsUseCaseImpl): no Kafka event covers
            // this path, so the folder's cached "assets" search results must be evicted
            // synchronously here, or the just-restored asset stays invisible in the gallery.
            Long folderId = asset.getFolder() != null ? asset.getFolder().getFolderId() : null;
            assetSearchCachePort.evictFolder(folderId);
        }
    }
}
