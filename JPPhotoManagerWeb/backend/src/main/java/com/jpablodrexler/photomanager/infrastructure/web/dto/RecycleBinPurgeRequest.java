package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecycleBinPurgeRequest(@NotNull List<Long> assetIds) {}
