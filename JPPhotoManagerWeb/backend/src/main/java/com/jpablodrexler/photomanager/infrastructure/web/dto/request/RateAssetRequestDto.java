package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateAssetRequestDto(
        @Min(0) @Max(5) int rating
) {}
