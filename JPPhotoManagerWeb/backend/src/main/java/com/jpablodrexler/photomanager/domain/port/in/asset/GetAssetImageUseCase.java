package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.application.dto.AssetImage;
import java.io.IOException;

public interface GetAssetImageUseCase {
    AssetImage execute(Long assetId) throws IOException;
}
