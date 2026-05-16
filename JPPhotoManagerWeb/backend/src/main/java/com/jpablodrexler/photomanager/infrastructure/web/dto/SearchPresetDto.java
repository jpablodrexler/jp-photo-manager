package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.time.Instant;

public record SearchPresetDto(
        Long presetId,
        String name,
        Instant createdAt,
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
