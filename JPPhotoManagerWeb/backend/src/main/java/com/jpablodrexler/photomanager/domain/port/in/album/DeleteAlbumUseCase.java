package com.jpablodrexler.photomanager.domain.port.in.album;

import java.util.UUID;

public interface DeleteAlbumUseCase {
    void execute(Long albumId, UUID userId);
}
