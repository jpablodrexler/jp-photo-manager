package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;

public interface StreamAssetUseCase {

    Asset execute(Long assetId);
}
