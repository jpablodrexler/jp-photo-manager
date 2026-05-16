package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import java.util.UUID;

public interface GetAlbumUseCase {
    AlbumData executeSummary(Long albumId, UUID userId);
    PaginatedResult<Asset> executeAssets(Long albumId, UUID userId, int page);
}
