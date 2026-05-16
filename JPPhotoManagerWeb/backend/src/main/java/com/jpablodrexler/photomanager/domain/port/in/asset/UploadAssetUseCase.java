package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import java.io.IOException;

public interface UploadAssetUseCase {
    Asset execute(String folderPath, String fileName, byte[] content) throws IOException;
}
