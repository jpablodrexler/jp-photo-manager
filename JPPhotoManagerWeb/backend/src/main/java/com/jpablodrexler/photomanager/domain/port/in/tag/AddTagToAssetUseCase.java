package com.jpablodrexler.photomanager.domain.port.in.tag;

public interface AddTagToAssetUseCase {
    void execute(Long assetId, String name);
}
