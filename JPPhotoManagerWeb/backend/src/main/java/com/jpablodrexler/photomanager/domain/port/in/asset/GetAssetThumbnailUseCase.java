package com.jpablodrexler.photomanager.domain.port.in.asset;

public interface GetAssetThumbnailUseCase {
    byte[] execute(Long assetId);
}
