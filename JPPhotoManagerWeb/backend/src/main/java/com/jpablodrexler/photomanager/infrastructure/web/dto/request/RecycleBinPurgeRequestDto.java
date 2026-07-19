package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecycleBinPurgeRequestDto(@NotNull List<Long> assetIds) {}
