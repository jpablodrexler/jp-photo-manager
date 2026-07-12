package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MoveAssetsRequestDto {

    @NotEmpty
    private Long[] assetIds;

    @NotBlank
    private String destinationFolderPath;

    private boolean preserveOriginal;
}
