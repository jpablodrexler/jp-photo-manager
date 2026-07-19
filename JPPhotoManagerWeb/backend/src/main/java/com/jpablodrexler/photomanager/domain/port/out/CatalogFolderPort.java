package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.Asset;

public interface CatalogFolderPort {

    Asset createAsset(String directoryPath, String fileName);
}
