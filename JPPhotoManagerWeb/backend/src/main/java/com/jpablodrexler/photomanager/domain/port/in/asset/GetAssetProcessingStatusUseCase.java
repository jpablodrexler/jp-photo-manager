package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;

public interface GetAssetProcessingStatusUseCase {
    ProcessingStatus execute(Long assetId);
}
