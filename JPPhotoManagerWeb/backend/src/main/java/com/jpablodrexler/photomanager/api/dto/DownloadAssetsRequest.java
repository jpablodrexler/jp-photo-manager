package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DownloadAssetsRequest {

    @NotEmpty
    @Size(max = 500)
    private List<Long> assetIds;
}
