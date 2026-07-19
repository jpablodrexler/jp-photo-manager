package com.jpablodrexler.photomanager.domain.port.in.asset;

import java.util.UUID;

public interface RateAssetUseCase {
    void execute(Long assetId, int rating, UUID userId);
}
