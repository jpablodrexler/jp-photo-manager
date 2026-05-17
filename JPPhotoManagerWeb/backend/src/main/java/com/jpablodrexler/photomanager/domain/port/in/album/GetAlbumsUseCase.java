package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import java.util.List;
import java.util.UUID;

public interface GetAlbumsUseCase {
    List<AlbumData> execute(UUID userId);
}
