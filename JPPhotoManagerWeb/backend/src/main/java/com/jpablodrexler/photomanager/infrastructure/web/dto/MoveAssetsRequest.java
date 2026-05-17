package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MoveAssetsRequest {

    @NotEmpty
    private Long[] assetIds;

    @NotBlank
    private String destinationFolderPath;

    private boolean preserveOriginal;
}
