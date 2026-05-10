package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePresetRequest(
        @NotBlank String name,
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
