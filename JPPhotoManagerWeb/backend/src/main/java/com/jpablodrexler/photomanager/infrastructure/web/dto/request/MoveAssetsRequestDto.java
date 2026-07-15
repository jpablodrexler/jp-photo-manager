package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record MoveAssetsRequestDto(
        @NotEmpty Long[] assetIds,
        @NotBlank String destinationFolderPath,
        boolean preserveOriginal) {
}
