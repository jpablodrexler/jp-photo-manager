package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DownloadAssetsRequestDto {

    @NotEmpty
    @Size(max = 500)
    private List<Long> assetIds;
}
