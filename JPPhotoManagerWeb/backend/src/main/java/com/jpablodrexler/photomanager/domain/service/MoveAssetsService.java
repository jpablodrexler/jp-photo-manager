package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;

public interface MoveAssetsService {

    boolean moveAssets(Asset[] assets, Folder destinationFolder, boolean preserveOriginalFile);

    void deleteAssets(Asset[] assets, boolean deleteFile);
}
