package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePresetRequestDto(
        @NotBlank String name,
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
