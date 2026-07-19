package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.CropRegion;

import java.io.IOException;

public interface CropAssetUseCase {
    Asset execute(long assetId, CropRegion request) throws IOException;
}
