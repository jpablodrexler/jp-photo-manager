package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;

import java.util.UUID;

public interface GetAlbumSummaryUseCase {
    AlbumData execute(Long albumId, UUID userId);
}
