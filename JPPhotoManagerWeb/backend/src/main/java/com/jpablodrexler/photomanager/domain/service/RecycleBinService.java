package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;

public interface RecycleBinService {
    PaginatedData<Asset> getDeletedAssets(int pageIndex);
    void restoreAssets(List<Long> assetIds);
    void purgeAssets(List<Long> assetIds);
    void purgeAll();
    void purgeExpired(int retentionDays);
}
