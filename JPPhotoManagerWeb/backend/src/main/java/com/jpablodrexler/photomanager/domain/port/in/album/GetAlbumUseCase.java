package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import java.util.UUID;

public interface GetAlbumUseCase {
    AlbumData executeSummary(Long albumId, UUID userId);
    PaginatedResult<Asset> executeAssets(Long albumId, UUID userId, int page);
}
