package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.time.Instant;

public record SearchPresetResponseDto(
        Long presetId,
        String name,
        Instant createdAt,
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
