package com.jpablodrexler.photomanager.domain.port.in.album;

import java.util.List;
import java.util.UUID;

public interface AddAssetsToAlbumUseCase {
    void execute(Long albumId, UUID userId, List<Long> assetIds);
}
