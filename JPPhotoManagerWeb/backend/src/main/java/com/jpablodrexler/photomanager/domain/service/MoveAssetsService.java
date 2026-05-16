package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;

public interface MoveAssetsService {

    boolean moveAssets(Asset[] assets, Folder destinationFolder, boolean preserveOriginalFile);

    void deleteAssets(Asset[] assets, boolean deleteFile);
}
