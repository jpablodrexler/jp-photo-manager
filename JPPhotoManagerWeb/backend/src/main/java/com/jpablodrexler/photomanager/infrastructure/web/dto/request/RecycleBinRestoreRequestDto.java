package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecycleBinRestoreRequestDto(@NotEmpty List<Long> assetIds) {}
