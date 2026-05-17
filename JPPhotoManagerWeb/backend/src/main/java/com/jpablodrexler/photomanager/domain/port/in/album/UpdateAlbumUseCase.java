package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import java.util.UUID;

public interface UpdateAlbumUseCase {
    AlbumData execute(Long albumId, UUID userId, String name, String description);
}
