package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkTagRequestDto(@NotEmpty List<Long> assetIds, @NotBlank String name) {}
