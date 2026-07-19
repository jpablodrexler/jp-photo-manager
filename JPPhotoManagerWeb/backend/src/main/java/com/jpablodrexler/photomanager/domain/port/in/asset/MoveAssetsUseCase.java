package com.jpablodrexler.photomanager.domain.port.in.asset;

public interface MoveAssetsUseCase {
    boolean execute(Long[] assetIds, String destinationPath, boolean preserveOriginal);
}
