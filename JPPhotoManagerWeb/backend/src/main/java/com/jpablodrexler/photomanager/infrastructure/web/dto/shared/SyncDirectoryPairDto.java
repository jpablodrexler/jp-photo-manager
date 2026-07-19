package com.jpablodrexler.photomanager.infrastructure.web.dto.shared;

import jakarta.validation.constraints.NotBlank;

public record SyncDirectoryPairDto(
        Long id,
        @NotBlank String sourceDirectory,
        @NotBlank String destinationDirectory,
        boolean includeSubFolders,
        boolean deleteAssetsNotInSource,
        int order
) {}
