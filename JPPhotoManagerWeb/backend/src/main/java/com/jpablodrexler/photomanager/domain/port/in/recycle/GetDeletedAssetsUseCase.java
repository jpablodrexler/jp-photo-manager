package com.jpablodrexler.photomanager.domain.port.in.recycle;

import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;

public interface GetDeletedAssetsUseCase {
    PaginatedResult<Asset> execute(int page);
}
