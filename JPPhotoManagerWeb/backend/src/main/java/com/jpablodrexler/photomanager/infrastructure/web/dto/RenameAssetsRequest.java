package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RenameAssetsRequest(
        @NotEmpty Long[] assetIds,
        @NotBlank String pattern,
        boolean applied
) {}
