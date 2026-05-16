package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateAssetRequest(
        @Min(0) @Max(5) int rating
) {}
