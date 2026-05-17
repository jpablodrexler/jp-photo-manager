package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.util.List;

public record RecycleBinPurgeRequest(List<Long> assetIds) {}
