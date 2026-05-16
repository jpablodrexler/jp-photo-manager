package com.jpablodrexler.photomanager.domain.port.in.asset;

public interface DeleteAssetsUseCase {
    void execute(Long[] assetIds, boolean permanently);
}
