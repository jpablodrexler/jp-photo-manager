package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.application.dto.RenameAssetsResult;

public interface RenameAssetsUseCase {
    RenameAssetsResult execute(Long[] assetIds, String pattern, boolean applied);
}
