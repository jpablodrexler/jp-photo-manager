package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.AssetStreamInfo;

public interface StreamAssetUseCase {

    AssetStreamInfo execute(Long assetId);
}
