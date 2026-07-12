package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.RenameAssetsResult;

public interface RenameAssetsUseCase {
    RenameAssetsResult execute(Long[] assetIds, String pattern, boolean applied);
}
