package com.jpablodrexler.photomanager.domain.port.in.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import java.util.List;
import java.util.UUID;

public interface GetAlbumsUseCase {
    List<AlbumData> execute(UUID userId);
}
