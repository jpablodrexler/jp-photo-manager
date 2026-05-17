package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;

public interface GetAssetsUseCase {
    PaginatedResult<Asset> execute(AssetFilter filter);
}
