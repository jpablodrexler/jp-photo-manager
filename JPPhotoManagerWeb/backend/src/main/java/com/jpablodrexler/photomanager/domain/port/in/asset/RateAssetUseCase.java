package com.jpablodrexler.photomanager.domain.port.in.asset;

public interface RateAssetUseCase {
    void execute(Long assetId, int rating);
}
