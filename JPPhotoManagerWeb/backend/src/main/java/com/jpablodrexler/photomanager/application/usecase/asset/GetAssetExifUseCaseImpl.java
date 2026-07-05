package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetExifUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetAssetExifUseCaseImpl implements GetAssetExifUseCase {

    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "asset-exif", key = "#assetId")
    public AssetExif execute(Long assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        return assetExifRepository.findByAssetId(assetId).orElse(null);
    }
}
