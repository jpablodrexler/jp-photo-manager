package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetAssetImageUseCaseImpl implements GetAssetImageUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;

    @Override
    @Transactional(readOnly = true)
    public AssetImage execute(Long assetId) throws IOException {
        var asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        byte[] bytes = storagePort.readFileBytes(asset.getFullPath());
        return new AssetImage(bytes, asset.getFileName());
    }
}
