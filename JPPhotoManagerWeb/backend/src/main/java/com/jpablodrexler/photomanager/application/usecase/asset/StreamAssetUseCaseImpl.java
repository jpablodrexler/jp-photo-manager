package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.StreamAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StreamAssetUseCaseImpl implements StreamAssetUseCase {

    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public Asset execute(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
    }
}
