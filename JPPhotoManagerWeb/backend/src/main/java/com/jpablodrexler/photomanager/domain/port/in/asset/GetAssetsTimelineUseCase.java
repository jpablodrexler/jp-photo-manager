package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;

public interface GetAssetsTimelineUseCase {
    PaginatedResult<TimelineGroup> execute(AssetFilter filter);
}
