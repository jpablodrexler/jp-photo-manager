package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;

import java.util.UUID;

public interface GetAlbumAssetsUseCase {
    PaginatedResult<Asset> execute(Long albumId, UUID userId, int page);
}
