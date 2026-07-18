package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAssetsUseCaseImpl implements GetAssetsUseCase {

    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "assets", keyGenerator = "assetSearchCacheKeyGenerator")
    public PaginatedResult<Asset> execute(AssetFilter filter) {
        return assetRepository.findFiltered(filter);
    }
}
