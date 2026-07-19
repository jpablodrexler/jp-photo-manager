package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.AssetImage;
import java.io.IOException;
import java.util.UUID;

public interface GetAssetImageUseCase {
    AssetImage execute(Long assetId, UUID userId) throws IOException;
}
