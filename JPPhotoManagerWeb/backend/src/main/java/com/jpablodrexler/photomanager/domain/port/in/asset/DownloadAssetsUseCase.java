package com.jpablodrexler.photomanager.domain.port.in.asset;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface DownloadAssetsUseCase {
    void execute(List<Long> assetIds, OutputStream out) throws IOException;
}
