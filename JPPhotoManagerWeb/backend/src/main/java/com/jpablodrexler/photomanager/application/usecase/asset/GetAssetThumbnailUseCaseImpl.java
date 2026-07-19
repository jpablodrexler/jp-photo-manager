package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetThumbnailUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAssetThumbnailUseCaseImpl implements GetAssetThumbnailUseCase {

    private final ThumbnailPort thumbnailPort;

    @Override
    public byte[] execute(Long assetId) {
        return thumbnailPort.loadThumbnail(assetId + ".bin");
    }
}
