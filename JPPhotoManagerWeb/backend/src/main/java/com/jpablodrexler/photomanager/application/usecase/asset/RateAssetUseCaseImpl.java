package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RateAssetUseCaseImpl implements RateAssetUseCase {

    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public void execute(Long assetId, int rating) {
        var asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        asset.setRating(rating);
        assetRepository.save(asset);
    }
}
