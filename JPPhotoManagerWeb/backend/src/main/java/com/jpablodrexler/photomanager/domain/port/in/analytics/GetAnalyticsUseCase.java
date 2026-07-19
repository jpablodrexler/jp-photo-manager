package com.jpablodrexler.photomanager.domain.port.in.analytics;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;

public interface GetAnalyticsUseCase {
    AnalyticsData execute();
}
