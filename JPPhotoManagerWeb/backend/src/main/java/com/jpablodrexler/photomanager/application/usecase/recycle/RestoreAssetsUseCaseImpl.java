package com.jpablodrexler.photomanager.application.usecase.recycle;

import com.jpablodrexler.photomanager.domain.port.in.recycle.RestoreAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
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

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict("home-stats")
    public void execute(List<Long> assetIds) {
        var assets = assetRepository.findAllById(assetIds);
        for (var asset : assets) {
            asset.setDeletedAt(null);
            assetRepository.save(asset);
        }
    }
}
