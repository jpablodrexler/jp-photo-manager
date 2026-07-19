package com.jpablodrexler.photomanager.domain.port.in.asset;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public interface DownloadAssetsUseCase {
    void execute(List<Long> assetIds, OutputStream out, UUID userId) throws IOException;
}
