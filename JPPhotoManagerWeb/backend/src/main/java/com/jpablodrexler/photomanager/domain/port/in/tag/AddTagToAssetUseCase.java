package com.jpablodrexler.photomanager.domain.port.in.tag;

import java.util.UUID;

public interface AddTagToAssetUseCase {
    void execute(Long assetId, String name, UUID userId);
}
