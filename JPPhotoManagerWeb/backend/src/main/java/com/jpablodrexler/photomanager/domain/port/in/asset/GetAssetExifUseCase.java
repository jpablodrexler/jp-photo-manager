package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.AssetExif;

public interface GetAssetExifUseCase {
    AssetExif execute(Long assetId);
}
