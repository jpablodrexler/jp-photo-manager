package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetStreamInfo;
import com.jpablodrexler.photomanager.domain.port.in.asset.StreamAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StreamAssetUseCaseImpl implements StreamAssetUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;

    @Override
    @Transactional(readOnly = true)
    public AssetStreamInfo execute(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        if (!storagePort.fileExists(asset.getFullPath())) {
            throw new NoSuchElementException("Asset file not found on disk: " + asset.getFullPath());
        }
        return new AssetStreamInfo(asset, storagePort.getFileSize(asset.getFullPath()));
    }
}
