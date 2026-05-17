package com.jpablodrexler.photomanager.domain.port.in.tag;

public interface RemoveTagFromAssetUseCase {
    void execute(Long assetId, String name);
}
