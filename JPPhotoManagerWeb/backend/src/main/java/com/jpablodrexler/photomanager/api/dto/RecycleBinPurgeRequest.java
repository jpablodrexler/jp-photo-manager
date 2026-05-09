package com.jpablodrexler.photomanager.api.dto;

import java.util.List;

public record RecycleBinPurgeRequest(List<Long> assetIds) {}
