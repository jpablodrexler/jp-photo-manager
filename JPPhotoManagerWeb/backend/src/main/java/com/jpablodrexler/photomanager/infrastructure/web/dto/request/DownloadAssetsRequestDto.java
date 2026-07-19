package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DownloadAssetsRequestDto(
        @NotEmpty @Size(max = 500) List<Long> assetIds) {
}
