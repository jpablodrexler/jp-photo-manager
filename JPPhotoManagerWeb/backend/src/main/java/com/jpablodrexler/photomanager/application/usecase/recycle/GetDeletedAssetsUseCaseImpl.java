package com.jpablodrexler.photomanager.application.usecase.recycle;

import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.recycle.GetDeletedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDeletedAssetsUseCaseImpl implements GetDeletedAssetsUseCase {

    private static final int PAGE_SIZE = 100;

    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> execute(int page) {
        return assetRepository.findDeleted(page, PAGE_SIZE);
    }
}
