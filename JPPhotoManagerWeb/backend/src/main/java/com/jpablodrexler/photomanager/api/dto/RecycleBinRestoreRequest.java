package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecycleBinRestoreRequest(@NotEmpty List<Long> assetIds) {}
