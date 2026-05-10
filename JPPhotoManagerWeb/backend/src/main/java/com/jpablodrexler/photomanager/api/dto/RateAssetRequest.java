package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RateAssetRequest(
        @Min(0) @Max(5) int rating
) {}
