package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AlbumAssetIdsRequest(
        @NotEmpty List<Long> assetIds
) {}
